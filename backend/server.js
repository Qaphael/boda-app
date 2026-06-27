const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const db = require('./db');
const { verifyFirebaseToken, getFirebaseAdmin } = require('./middleware/auth');
require('dotenv').config();

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST']
  }
});

app.use(cors());
app.use(express.json());

// Serve Admin Dashboard static files
const path = require('path');
app.use('/admin', express.static(path.join(__dirname, 'admin-dashboard')));

const PORT = process.env.PORT || 3000;

// Health Check Endpoint
app.get('/health', (req, res) => {
  res.status(200).json({ status: 'healthy', timestamp: new Date(), message: 'Boda Gulu API' });
});

// Admin authentication middleware
const adminAuth = (req, res, next) => {
  const key = req.headers['x-admin-key'];
  if (!key || key !== process.env.ADMIN_SECRET_KEY) {
    return res.status(403).json({ error: 'Forbidden: invalid or missing admin key' });
  }
  next();
};

// ==========================================
// RIDER / PASSENGER API ENDPOINTS (SECURE)
// ==========================================

// Register / Sync User details in database
app.post('/api/users/sync', verifyFirebaseToken, async (req, res) => {
  const { uid, phone_number, name, email } = req.user;
  try {
    const query = `
      INSERT INTO users (uid, phone, full_name, email, wallet_balance)
      VALUES ($1, $2, $3, $4, 5000.00)
      ON CONFLICT (uid) DO UPDATE
      SET full_name = EXCLUDED.full_name, email = EXCLUDED.email, updated_at = NOW()
      RETURNING *;
    `;
    const result = await db.query(query, [uid, phone_number || req.body.phone || '+256770000000', name || req.body.name || 'Gulu Passenger', email]);
    res.json({ success: true, user: result.rows[0] });
  } catch (error) {
    console.error('Error syncing user:', error);
    res.status(500).json({ error: 'Database sync failure: ' + error.message });
  }
});

// Store FCM push token for a user
app.post('/api/users/fcm-token', async (req, res) => {
  const { fcm_token } = req.body;
  const uid = req.user?.uid || req.body.uid;
  if (!fcm_token || !uid) {
    return res.status(400).json({ error: 'Missing fcm_token or uid' });
  }
  try {
    await db.query('UPDATE users SET fcm_token = $1, updated_at = NOW() WHERE uid = $2', [fcm_token, uid]);
    res.json({ success: true });
  } catch (error) {
    console.error('Error storing FCM token:', error);
    res.status(500).json({ error: error.message });
  }
});

// Helper: send push notification to a user by UID
async function sendPushToUser(uid, title, body, data = {}) {
  try {
    const result = await db.query('SELECT fcm_token FROM users WHERE uid = $1', [uid]);
    const token = result.rows[0]?.fcm_token;
    if (!token) return;

    const admin = getFirebaseAdmin();
    if (!admin) return;

    await admin.messaging().send({
      token,
      notification: { title, body },
      data,
      android: { priority: 'high', notification: { channelId: 'boda_gulu_alerts' } }
    });
  } catch (err) {
    console.error('Push send failed:', err.message);
  }
}

// Helper: send push notification to a driver by UID
async function sendPushToDriver(uid, title, body, data = {}) {
  try {
    const result = await db.query('SELECT fcm_token FROM drivers WHERE uid = $1', [uid]);
    const token = result.rows[0]?.fcm_token;
    if (!token) return;

    const admin = getFirebaseAdmin();
    if (!admin) return;

    await admin.messaging().send({
      token,
      notification: { title, body },
      data,
      android: { priority: 'high', notification: { channelId: 'boda_gulu_alerts' } }
    });
  } catch (err) {
    console.error('Push send to driver failed:', err.message);
  }
}

// Fetch saved places for logged-in user
app.get('/api/saved-places', verifyFirebaseToken, async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM saved_places WHERE user_uid = $1 ORDER BY id DESC', [req.user.uid]);
    res.json(result.rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Save a new place
app.post('/api/saved-places', verifyFirebaseToken, async (req, res) => {
  const { label, name, latitude, longitude } = req.body;
  try {
    const query = `
      INSERT INTO saved_places (user_uid, label, name, latitude, longitude)
      VALUES ($1, $2, $3, $4, $5) RETURNING *;
    `;
    const result = await db.query(query, [req.user.uid, label, name, latitude, longitude]);
    res.json({ success: true, savedPlace: result.rows[0] });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get user profile from PostgreSQL
app.get('/api/users/me', verifyFirebaseToken, async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM users WHERE uid = $1', [req.user.uid]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'User not found' });
    res.json(result.rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// List rider's trip history
app.get('/api/trips', verifyFirebaseToken, async (req, res) => {
  try {
    const result = await db.query(
      `SELECT t.*, d.full_name as driver_name, d.plate_number
       FROM trips t LEFT JOIN drivers d ON t.driver_uid = d.uid
       WHERE t.passenger_uid = $1 ORDER BY t.created_at DESC`,
      [req.user.uid]
    );
    res.json(result.rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Create a new ride booking
app.post('/api/trips/book', verifyFirebaseToken, async (req, res) => {
  const {
    pickup_name, pickup_lat, pickup_lon,
    dropoff_name, dropoff_lat, dropoff_lon,
    distance_km, duration_mins, fare,
    promo_applied, payment_method
  } = req.body;

  const tripCode = 'TRP-' + Math.floor(1000 + Math.random() * 9000) + '-GULU';

  try {
    const query = `
      INSERT INTO trips (
        trip_code, passenger_uid, pickup_name, pickup_lat, pickup_lon,
        dropoff_name, dropoff_lat, dropoff_lon, distance_km, duration_mins,
        fare, promo_applied, payment_method, status
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, 'matching')
      RETURNING *;
    `;
    const result = await db.query(query, [
      tripCode, req.user.uid, pickup_name, pickup_lat, pickup_lon,
      dropoff_name, dropoff_lat, dropoff_lon, distance_km, duration_mins,
      fare, promo_applied, payment_method
    ]);

    const bookedTrip = result.rows[0];

    // Broadcast new trip request to nearby online drivers via Websockets
    io.emit('new_trip_request', bookedTrip);

    // Push notification to nearby online drivers
    try {
      const nearbyDrivers = await db.query(
        "SELECT uid FROM drivers WHERE is_online = true AND latitude IS NOT NULL",
      );
      for (const driver of nearbyDrivers.rows) {
        sendPushToDriver(driver.uid, 'New Ride Request', `${pickup_name} → ${dropoff_name} | UGX ${fare}`, {
          trip_id: String(bookedTrip.id),
          type: 'new_trip'
        });
      }
    } catch (_) {}

    res.json({ success: true, trip: bookedTrip });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Retrieve single trip status
app.get('/api/trips/:id', verifyFirebaseToken, async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM trips WHERE id = $1', [req.params.id]);
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Trip not found' });
    }
    res.json(result.rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Fetch chat history for a trip
app.get('/api/trips/:id/messages', verifyFirebaseToken, async (req, res) => {
  try {
    const result = await db.query(
      'SELECT * FROM chat_messages WHERE trip_id = $1 ORDER BY created_at ASC',
      [req.params.id]
    );
    res.json(result.rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// ==========================================
// TRIP STATUS UPDATES
// ==========================================

app.patch('/api/trips/:id/status', verifyFirebaseToken, async (req, res) => {
  const { status, driver_uid, rating, comment, dispute_reason, dispute_evidence } = req.body;
  try {
    let query, params;
    if (status === 'completed' && rating) {
      query = `UPDATE trips SET status = $1, completed_at = NOW(), driver_uid = COALESCE($2, driver_uid), rating = $3, comment = $4 WHERE id = $5 RETURNING *`;
      params = [status, driver_uid, rating, comment, req.params.id];
    } else if (status === 'completed') {
      query = `UPDATE trips SET status = $1, completed_at = NOW(), driver_uid = COALESCE($2, driver_uid) WHERE id = $3 RETURNING *`;
      params = [status, driver_uid, req.params.id];
    } else if (status === 'disputed') {
      query = `UPDATE trips SET status = $1, dispute_reason = $2, dispute_evidence = $3 WHERE id = $4 RETURNING *`;
      params = [status, dispute_reason, dispute_evidence, req.params.id];
    } else {
      query = `UPDATE trips SET status = $1 WHERE id = $2 RETURNING *`;
      params = [status, req.params.id];
    }
    const result = await db.query(query, params);
    if (result.rows.length === 0) return res.status(404).json({ error: 'Trip not found' });
    io.to(`trip_${req.params.id}`).emit('trip_status_updated', { tripId: parseInt(req.params.id), status });
    res.json({ success: true, trip: result.rows[0] });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// ==========================================
// WALLET / TRANSACTIONS
// ==========================================

app.get('/api/wallet/transactions', verifyFirebaseToken, async (req, res) => {
  try {
    const result = await db.query(
      'SELECT * FROM transactions WHERE user_uid = $1 ORDER BY created_at DESC',
      [req.user.uid]
    );
    res.json(result.rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.post('/api/wallet/topup', verifyFirebaseToken, async (req, res) => {
  const { amount, payment_provider } = req.body;
  const uid = req.user.uid;
  const amt = parseFloat(amount);
  if (isNaN(amt) || amt <= 0) return res.status(400).json({ error: 'Invalid amount' });

  try {
    await db.query('BEGIN');
    await db.query(
      'UPDATE users SET wallet_balance = wallet_balance + $1, updated_at = NOW() WHERE uid = $2',
      [amt, uid]
    );
    const txRef = `MOM-${Date.now()}-${Math.floor(1000 + Math.random() * 9000)}`;
    await db.query(
      `INSERT INTO transactions (user_uid, transaction_ref, type, amount, payment_provider, status)
       VALUES ($1, $2, 'deposit', $3, $4, 'completed')`,
      [uid, txRef, amt, payment_provider || 'MTN']
    );
    await db.query('COMMIT');
    res.json({ success: true, reference: txRef });
  } catch (error) {
    await db.query('ROLLBACK').catch(() => {});
    res.status(500).json({ error: error.message });
  }
});

// Record a ride payment (deduct from wallet or record mobile money payment)
app.post('/api/wallet/pay', verifyFirebaseToken, async (req, res) => {
  const { amount, payment_provider, trip_id } = req.body;
  const uid = req.user.uid;
  const amt = parseFloat(amount);
  if (isNaN(amt) || amt <= 0) return res.status(400).json({ error: 'Invalid amount' });

  try {
    await db.query('BEGIN');
    if (payment_provider === 'Wallet') {
      const bal = await db.query('SELECT wallet_balance FROM users WHERE uid = $1', [uid]);
      if (bal.rows.length === 0 || parseFloat(bal.rows[0].wallet_balance) < amt) {
        await db.query('ROLLBACK');
        return res.status(400).json({ error: 'Insufficient wallet balance' });
      }
      await db.query(
        'UPDATE users SET wallet_balance = wallet_balance - $1, updated_at = NOW() WHERE uid = $2',
        [amt, uid]
      );
    }
    const txRef = `PAY-${Date.now()}-${Math.floor(1000 + Math.random() * 9000)}`;
    await db.query(
      `INSERT INTO transactions (user_uid, transaction_ref, type, amount, payment_provider, status)
       VALUES ($1, $2, 'payment', $3, $4, 'completed')`,
      [uid, txRef, amt, payment_provider]
    );
    await db.query('COMMIT');
    res.json({ success: true, reference: txRef });
  } catch (error) {
    await db.query('ROLLBACK').catch(() => {});
    res.status(500).json({ error: error.message });
  }
});

// ==========================================
// EMERGENCY CONTACTS
// ==========================================

app.get('/api/emergency-contacts', verifyFirebaseToken, async (req, res) => {
  try {
    const result = await db.query(
      'SELECT * FROM emergency_contacts WHERE user_uid = $1 ORDER BY id DESC',
      [req.user.uid]
    );
    res.json(result.rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.post('/api/emergency-contacts', verifyFirebaseToken, async (req, res) => {
  const { name, phone_number } = req.body;
  try {
    const result = await db.query(
      'INSERT INTO emergency_contacts (user_uid, name, phone_number) VALUES ($1, $2, $3) RETURNING *',
      [req.user.uid, name, phone_number]
    );
    res.json({ success: true, contact: result.rows[0] });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.delete('/api/emergency-contacts/:id', verifyFirebaseToken, async (req, res) => {
  try {
    await db.query('DELETE FROM emergency_contacts WHERE id = $1 AND user_uid = $2', [req.params.id, req.user.uid]);
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// ==========================================
// REFERRALS
// ==========================================

app.get('/api/referrals', verifyFirebaseToken, async (req, res) => {
  try {
    const result = await db.query(
      'SELECT * FROM referrals WHERE referrer_uid = $1 ORDER BY created_at DESC',
      [req.user.uid]
    );
    res.json(result.rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.post('/api/referrals', verifyFirebaseToken, async (req, res) => {
  const { referred_name, referred_phone, referral_code } = req.body;
  try {
    const result = await db.query(
      `INSERT INTO referrals (referrer_uid, referred_name, referred_phone, referral_code, status, reward_amount)
       VALUES ($1, $2, $3, $4, 'pending', 3000.00) RETURNING *`,
      [req.user.uid, referred_name, referred_phone, referral_code]
    );
    res.json({ success: true, referral: result.rows[0] });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.post('/api/referrals/:id/complete', verifyFirebaseToken, async (req, res) => {
  const referralId = parseInt(req.params.id);
  try {
    const refResult = await db.query('SELECT * FROM referrals WHERE id = $1', [referralId]);
    if (refResult.rows.length === 0) return res.status(404).json({ error: 'Referral not found' });
    const ref = refResult.rows[0];
    if (ref.status === 'completed') return res.json({ success: true, message: 'Already completed' });

    await db.query('BEGIN');
    await db.query(
      'UPDATE referrals SET status = \'completed\', updated_at = NOW() WHERE id = $1',
      [referralId]
    );
    const rewardAmt = parseFloat(ref.reward_amount) || 3000.00;
    const txRef = `REF-BONUS-${referralId}-${Date.now()}`;
    await db.query(
      'UPDATE users SET wallet_balance = wallet_balance + $1, updated_at = NOW() WHERE uid = $2',
      [rewardAmt, ref.referrer_uid]
    );
    await db.query(
      `INSERT INTO transactions (user_uid, transaction_ref, type, amount, payment_provider, status)
       VALUES ($1, $2, 'bonus', $3, 'Wallet', 'completed')`,
      [ref.referrer_uid, txRef, rewardAmt]
    );
    await db.query('COMMIT');
    res.json({ success: true, reward: rewardAmt, referrer_uid: ref.referrer_uid });
  } catch (error) {
    await db.query('ROLLBACK').catch(() => {});
    res.status(500).json({ error: error.message });
  }
});

// ==========================================
// SAVED PLACES DELETE
// ==========================================

app.delete('/api/saved-places/:id', verifyFirebaseToken, async (req, res) => {
  try {
    await db.query('DELETE FROM saved_places WHERE id = $1 AND user_uid = $2', [req.params.id, req.user.uid]);
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// ==========================================
// DRIVER API ENDPOINTS (ONLINE / GPS UPDATES)
// ==========================================

// Register driver
app.post('/api/drivers/register', verifyFirebaseToken, async (req, res) => {
  const uid = req.user.uid;
  const { full_name, phone, plate_number, helmet_number } = req.body;
  try {
    const query = `
      INSERT INTO drivers (uid, full_name, phone, plate_number, helmet_number)
      VALUES ($1, $2, $3, $4, $5)
      ON CONFLICT (uid) DO NOTHING RETURNING *;
    `;
    const result = await db.query(query, [uid, full_name, phone, plate_number, helmet_number]);
    res.json({ success: true, driver: result.rows[0] });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Set online status & coords
app.post('/api/drivers/status', verifyFirebaseToken, async (req, res) => {
  const uid = req.user.uid;
  const { is_online, latitude, longitude } = req.body;
  try {
    const query = `
      UPDATE drivers
      SET is_online = $2, latitude = $3, longitude = $4, updated_at = NOW()
      WHERE uid = $1 RETURNING *;
    `;
    const result = await db.query(query, [uid, is_online, latitude, longitude]);

    // Broadcast updated driver location to admins/riders
    io.emit('driver_location_update', { uid, is_online, latitude, longitude });

    res.json({ success: true, driver: result.rows[0] });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// ==========================================
// ADMIN DASHBOARD API ENDPOINTS
// ==========================================

// Fetch general system statistics for admin panel
app.get('/api/admin/stats', adminAuth, async (req, res) => {
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

// Fetch active live tracking logs
app.get('/api/admin/active-trips', adminAuth, async (req, res) => {
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

// Dynamic pricing configurations
let pricingSettings = {
  base_fare: 1500,
  rate_per_km: 1000,
  rate_per_min: 150,
  surge_multiplier: 1.0,
  surge_reason: 'Normal'
};

// In-memory active promo campaigns
let promoCodes = [];

// In-memory active emergency SOS alerts
let activeSOSAlerts = [];

// Fetch Gulu pricing & surge settings
app.get('/api/admin/pricing', adminAuth, (req, res) => {
  res.json(pricingSettings);
});

// Update surge pricing multiplier
app.post('/api/admin/pricing', adminAuth, (req, res) => {
  const { multiplier, reason } = req.body;
  if (multiplier) pricingSettings.surge_multiplier = parseFloat(multiplier);
  if (reason) pricingSettings.surge_reason = reason;
  
  // Broadcast update to all clients
  io.emit('pricing_rules_updated', pricingSettings);
  res.json({ success: true, pricingSettings });
});

// Calculate Dynamic Fare route
app.post('/api/trips/calculate-fare', (req, res) => {
  const { distance_km, duration_mins } = req.body;
  const rawFare = pricingSettings.base_fare + 
                  (parseFloat(distance_km || 0) * pricingSettings.rate_per_km) + 
                  (parseInt(duration_mins || 0) * pricingSettings.rate_per_min);
  const surgeFare = Math.round(rawFare * pricingSettings.surge_multiplier);
  res.json({
    base_fare: pricingSettings.base_fare,
    distance_km,
    duration_mins,
    surge_multiplier: pricingSettings.surge_multiplier,
    surge_reason: pricingSettings.surge_reason,
    original_fare: rawFare,
    final_fare: surgeFare
  });
});

// Fetch active promo codes
app.get('/api/admin/promos', adminAuth, (req, res) => {
  res.json(promoCodes);
});

// Add new promo code
app.post('/api/admin/promos', adminAuth, (req, res) => {
  const { code, discount_type, value } = req.body;
  if (!code || !discount_type || !value) {
    return res.status(400).json({ error: 'Missing code, discount_type, or value' });
  }
  const newPromo = { code: code.toUpperCase(), discount_type, value: parseFloat(value), active: true };
  promoCodes.push(newPromo);
  res.json({ success: true, promo: newPromo });
});

// Validate promo code
app.post('/api/promos/validate', (req, res) => {
  const { code, original_fare } = req.body;
  const promo = promoCodes.find(p => p.code.toUpperCase() === code.toUpperCase() && p.active);
  if (!promo) {
    return res.status(404).json({ valid: false, message: 'Invalid or expired promo code' });
  }
  let discount = 0;
  if (promo.discount_type === 'percent') {
    discount = Math.round((parseFloat(original_fare) * promo.value) / 100);
  } else {
    discount = promo.value;
  }
  discount = Math.min(discount, original_fare);
  res.json({
    valid: true,
    code: promo.code,
    discount_amount: discount,
    final_fare: original_fare - discount
  });
});

// Fetch all active SOS alerts
app.get('/api/admin/sos', adminAuth, (req, res) => {
  res.json(activeSOSAlerts);
});

// Resolve SOS Alert
app.post('/api/admin/sos/:id/resolve', adminAuth, (req, res) => {
  const alertId = req.params.id;
  const alertIndex = activeSOSAlerts.findIndex(a => a.id === alertId);
  if (alertIndex !== -1) {
    activeSOSAlerts[alertIndex].status = 'resolved';
    io.emit('sos_alert_resolved', alertId);
    return res.json({ success: true, message: 'SOS Alert marked as fully resolved in Gulu.' });
  }
  res.status(404).json({ error: 'Alert not found' });
});

// Fetch all Gulu drivers for visual map plot
app.get('/api/admin/drivers', adminAuth, async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM drivers ORDER BY created_at DESC');
    res.json(result.rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Toggle driver online status in database
app.post('/api/admin/drivers/:uid/toggle-status', adminAuth, async (req, res) => {
  const { uid } = req.params;
  try {
    const result = await db.query(
      'UPDATE drivers SET is_online = NOT is_online, updated_at = NOW() WHERE uid = $1 RETURNING *',
      [uid]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Driver not found' });
    }
    // Broadcast status change via websockets
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

// Settle driver wallet (zero-out earnings)
app.post('/api/admin/drivers/:uid/settle', adminAuth, async (req, res) => {
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

// Fetch all registered riders (users)
app.get('/api/admin/riders', adminAuth, async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM users ORDER BY created_at DESC');
    res.json(result.rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Add credit/balance to rider's wallet
app.post('/api/admin/riders/:uid/credit', adminAuth, async (req, res) => {
  const { uid } = req.params;
  const { amount } = req.body;
  const creditAmount = parseFloat(amount);
  
  if (isNaN(creditAmount) || creditAmount <= 0) {
    return res.status(400).json({ error: 'Invalid credit amount specified' });
  }

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

// Toggle suspension / activation status of a rider
app.post('/api/admin/riders/:uid/status', adminAuth, async (req, res) => {
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

// Fetch all trips (active & completed) for tabular view
app.get('/api/admin/trips', adminAuth, async (req, res) => {
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

// ==========================================
// REAL-TIME WEBSOCKET COMMUNICATOR
// ==========================================
io.on('connection', (socket) => {
  console.log(`🔌 New Web Socket Client Connected: ${socket.id}`);

  // Join spatial channels
  socket.on('join_trip_channel', (tripId) => {
    socket.join(`trip_${tripId}`);
    console.log(`Client joined real-time channel for trip_${tripId}`);
  });

  // Handle active live coordinates broadcast
  socket.on('update_live_gps', (data) => {
    io.to(`trip_${data.tripId}`).emit('live_gps_broadcast', data);
    db.query('UPDATE drivers SET latitude = $1, longitude = $2 WHERE uid = (SELECT driver_uid FROM trips WHERE id = $3)', [data.latitude, data.longitude, data.tripId])
      .catch(err => console.error('Silent postgis-log fail:', err.message));
  });

  // Rider ↔ Driver real-time chat
  socket.on('chat_message', async (data) => {
    // data = { tripId, senderUid, senderName, senderRole, message }
    const { tripId, senderUid, senderName, senderRole, message } = data;
    if (!tripId || !message || !senderUid) return;

    try {
      const result = await db.query(
        `INSERT INTO chat_messages (trip_id, sender_uid, sender_name, sender_role, message)
         VALUES ($1, $2, $3, $4, $5) RETURNING *`,
        [tripId, senderUid, senderName, senderRole, message]
      );
      const saved = result.rows[0];
      io.to(`trip_${tripId}`).emit('chat_message', {
        id: saved.id,
        tripId: saved.trip_id,
        senderUid: saved.sender_uid,
        senderName: saved.sender_name,
        senderRole: saved.sender_role,
        message: saved.message,
        createdAt: saved.created_at
      });
    } catch (err) {
      console.error('Chat message save failed:', err.message);
    }
  });

  // Typing indicator (no DB persistence, just broadcast)
  socket.on('chat_typing', (data) => {
    const { tripId, senderUid, senderName, isTyping } = data;
    if (!tripId) return;
    socket.to(`trip_${tripId}`).emit('chat_typing', { tripId, senderUid, senderName, isTyping });
  });

  socket.on('disconnect', () => {
    console.log(`🔌 Client disconnected: ${socket.id}`);
  });
});

// Start listening
server.listen(PORT, '0.0.0.0', () => {
  console.log(`===================================================`);
  console.log(`🚀 SafeBoda Gulu Backend Server Live at http://0.0.0.0:${PORT}`);
  console.log(`📡 Real-Time WebSockets configured on same port`);
  console.log(`🗄️ Connected to PostgreSQL pooling module`);
  console.log(`===================================================`);
});
