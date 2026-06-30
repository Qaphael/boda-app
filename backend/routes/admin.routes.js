const express = require('express');
const db = require('../db');
const { adminAuth } = require('../middleware/admin');
const { pricingUpdateSchema, riderCreditSchema } = require('../schemas/admin.schema');
const { createPromoSchema } = require('../schemas/promo.schema');

module.exports = function createAdminRouter(io) {
  const router = express.Router();

  router.get('/stats', adminAuth, async (req, res) => {
    try {
      const ridersCount = await db.query('SELECT COUNT(*) FROM users');
      const driversCount = await db.query('SELECT COUNT(*) FROM drivers');
      const onlineDriversCount = await db.query('SELECT COUNT(*) FROM drivers WHERE is_online = true');
      const activeTrips = await db.query("SELECT COUNT(*) FROM trips WHERE status IN ('matching', 'en_route', 'active')");
      const completedTrips = await db.query("SELECT COUNT(*) FROM trips WHERE status = 'completed'");
      const totalEarnings = await db.query("SELECT COALESCE(SUM(fare), 0) FROM trips WHERE status = 'completed'");
      
      res.json({
        totalRiders: parseInt(ridersCount.rows[0].count),
        totalDrivers: parseInt(driversCount.rows[0].count),
        onlineDrivers: parseInt(onlineDriversCount.rows[0].count),
        activeTrips: parseInt(activeTrips.rows[0].count),
        completedTrips: parseInt(completedTrips.rows[0].count),
        grossRevenue: parseFloat(totalEarnings.rows[0].sum)
      });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  router.get('/active-trips', adminAuth, async (req, res) => {
    try {
      const query = `
        SELECT t.*, u.full_name as passenger_name, d.full_name as driver_name, d.plate_number
        FROM trips t
        LEFT JOIN users u ON t.passenger_uid = u.uid
        LEFT JOIN drivers d ON t.driver_uid = d.uid
        WHERE t.status IN ('matching', 'en_route', 'active')
        ORDER BY t.created_at DESC;
      `;
      const result = await db.query(query);
      res.json(result.rows);
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  router.get('/pricing', adminAuth, async (req, res) => {
    try {
      const result = await db.query('SELECT * FROM pricing_settings LIMIT 1');
      res.json(result.rows[0] || { base_fare: 1500, rate_per_km: 1000, rate_per_min: 150, surge_multiplier: 1.0, surge_reason: 'Normal' });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  router.post('/pricing', adminAuth, async (req, res) => {
    const parsed = pricingUpdateSchema.safeParse(req.body);
    if (!parsed.success) {
      return res.status(400).json({ errors: parsed.error.flatten().fieldErrors });
    }
    const { multiplier, reason, base_fare, rate_per_km, rate_per_min } = parsed.data;
    try {
      const current = await db.query('SELECT * FROM pricing_settings LIMIT 1');
      const row = current.rows[0] || {};
      const newMultiplier = multiplier ? parseFloat(multiplier) : (row.surge_multiplier || 1.0);
      const newReason = reason || row.surge_reason || 'Normal';
      const newBase = base_fare ? parseInt(base_fare) : (row.base_fare || 1500);
      const newRateKm = rate_per_km ? parseInt(rate_per_km) : (row.rate_per_km || 1000);
      const newRateMin = rate_per_min ? parseInt(rate_per_min) : (row.rate_per_min || 150);

      await db.query(
        `UPDATE pricing_settings SET surge_multiplier=$1, surge_reason=$2, base_fare=$3, rate_per_km=$4, rate_per_min=$5, updated_at=NOW() WHERE id=(SELECT id FROM pricing_settings LIMIT 1)`,
        [newMultiplier, newReason, newBase, newRateKm, newRateMin]
      );

      const updated = { base_fare: newBase, rate_per_km: newRateKm, rate_per_min: newRateMin, surge_multiplier: newMultiplier, surge_reason: newReason };
      io.emit('pricing_rules_updated', updated);
      res.json({ success: true, pricingSettings: updated });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  router.get('/promos', adminAuth, async (req, res) => {
    try {
      const result = await db.query('SELECT * FROM promo_codes ORDER BY created_at DESC');
      res.json(result.rows);
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  router.post('/promos', adminAuth, async (req, res) => {
    const parsed = createPromoSchema.safeParse(req.body);
    if (!parsed.success) {
      return res.status(400).json({ errors: parsed.error.flatten().fieldErrors });
    }
    const { code, discount_type, value } = parsed.data;
    try {
      const result = await db.query(
        'INSERT INTO promo_codes (code, discount_type, value) VALUES ($1, $2, $3) RETURNING *',
        [code.toUpperCase(), discount_type, parseFloat(value)]
      );
      res.json({ success: true, promo: result.rows[0] });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  router.get('/sos', adminAuth, async (req, res) => {
    try {
      const result = await db.query('SELECT * FROM sos_alerts ORDER BY created_at DESC');
      res.json(result.rows);
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  router.post('/sos/:id/resolve', adminAuth, async (req, res) => {
    const alertId = req.params.id;
    try {
      const result = await db.query(
        "UPDATE sos_alerts SET status = 'resolved' WHERE id = $1 RETURNING *",
        [alertId]
      );
      if (result.rows.length === 0) {
        return res.status(404).json({ error: 'Alert not found' });
      }
      io.emit('sos_alert_resolved', alertId);
      res.json({ success: true, message: 'SOS Alert marked as resolved.' });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  router.get('/drivers', adminAuth, async (req, res) => {
    try {
      const result = await db.query('SELECT * FROM drivers ORDER BY created_at DESC');
      res.json(result.rows);
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  router.post('/drivers/:uid/toggle-status', adminAuth, async (req, res) => {
    const { uid } = req.params;
    try {
      const result = await db.query(
        'UPDATE drivers SET is_online = NOT is_online, updated_at = NOW() WHERE uid = $1 RETURNING *',
        [uid]
      );
      if (result.rows.length === 0) {
        return res.status(404).json({ error: 'Driver not found' });
      }
      io.emit('driver_location_update', { 
        uid, 
        is_online: result.rows[0].is_online, 
        latitude: result.rows[0].latitude, 
        longitude: result.rows[0].longitude 
      });
      res.json({ success: true, driver: result.rows[0] });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  router.post('/drivers/:uid/settle', adminAuth, async (req, res) => {
    const { uid } = req.params;
    try {
      const result = await db.query(
        'UPDATE drivers SET earnings = 0.00, updated_at = NOW() WHERE uid = $1 RETURNING *',
        [uid]
      );
      if (result.rows.length === 0) {
        return res.status(404).json({ error: 'Driver not found' });
      }
      res.json({ success: true, driver: result.rows[0] });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  router.get('/riders', adminAuth, async (req, res) => {
    try {
      const result = await db.query('SELECT * FROM users ORDER BY created_at DESC');
      res.json(result.rows);
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  router.post('/riders/:uid/credit', adminAuth, async (req, res) => {
    const parsed = riderCreditSchema.safeParse(req.body);
    if (!parsed.success) {
      return res.status(400).json({ errors: parsed.error.flatten().fieldErrors });
    }
    const { uid } = req.params;
    const { amount } = parsed.data;
    const creditAmount = parseFloat(amount);

    try {
      await db.query('BEGIN');
      const userResult = await db.query(
        'UPDATE users SET wallet_balance = wallet_balance + $1, updated_at = NOW() WHERE uid = $2 RETURNING *',
        [creditAmount, uid]
      );
      
      if (userResult.rows.length === 0) {
        await db.query('ROLLBACK');
        return res.status(404).json({ error: 'Rider not found' });
      }

      const txRef = `TX-${Date.now()}-${Math.floor(1000 + Math.random() * 9000)}`;
      await db.query(
        `INSERT INTO transactions (user_uid, transaction_ref, type, amount, payment_provider, status)
         VALUES ($1, $2, 'deposit', $3, 'InternalWallet', 'completed')`,
        [uid, txRef, creditAmount]
      );

      await db.query('COMMIT');
      res.json({ success: true, rider: userResult.rows[0] });
    } catch (error) {
      try { await db.query('ROLLBACK'); } catch (_) {}
      res.status(500).json({ error: error.message });
    }
  });

  router.post('/riders/:uid/status', adminAuth, async (req, res) => {
    const { uid } = req.params;
    const { status } = req.body;
    
    if (status !== 'Active' && status !== 'Suspended') {
      return res.status(400).json({ error: 'Invalid rider status specified' });
    }

    try {
      const result = await db.query(
        'UPDATE users SET status = $1, updated_at = NOW() WHERE uid = $2 RETURNING *',
        [status, uid]
      );
      if (result.rows.length === 0) {
        return res.status(404).json({ error: 'Rider not found' });
      }
      res.json({ success: true, rider: result.rows[0] });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  router.get('/trips', adminAuth, async (req, res) => {
    try {
      const query = `
        SELECT t.*, u.full_name as passenger_name, u.phone as passenger_phone, d.full_name as driver_name, d.plate_number
        FROM trips t
        LEFT JOIN users u ON t.passenger_uid = u.uid
        LEFT JOIN drivers d ON t.driver_uid = d.uid
        ORDER BY t.created_at DESC;
      `;
      const result = await db.query(query);
      res.json(result.rows);
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  return router;
};
