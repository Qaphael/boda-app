const express = require('express');
const db = require('../db');
const { verifyFirebaseToken } = require('../middleware/auth');
const { bookingLimiter } = require('../config/rateLimits');
const { sendPushToUser, sendPushToDriver } = require('../services/push.service');
const { calculateFare } = require('../services/fare.service');
const { bookTripSchema, tripStatusSchema, calculateFareSchema } = require('../schemas/trip.schema');

module.exports = function createTripsRouter(io) {
  const router = express.Router();

  router.get('/', verifyFirebaseToken, async (req, res) => {
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

  router.post('/book', verifyFirebaseToken, bookingLimiter, async (req, res) => {
    const parsed = bookTripSchema.safeParse(req.body);
    if (!parsed.success) {
      return res.status(400).json({ errors: parsed.error.flatten().fieldErrors });
    }
    const {
      pickup_name, pickup_lat, pickup_lon,
      dropoff_name, dropoff_lat, dropoff_lon,
      distance_km, duration_mins, fare,
      promo_applied, payment_method
    } = parsed.data;

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
      io.emit('new_trip_request', bookedTrip);

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

  router.patch('/:id/claim', verifyFirebaseToken, async (req, res) => {
    const driverUid = req.user.uid;
    const tripId = parseInt(req.params.id);
    try {
      await db.query('BEGIN');
      const result = await db.query(
        `UPDATE trips SET driver_uid = $1, status = 'en_route'
         WHERE id = $2 AND driver_uid IS NULL AND status = 'matching'
         RETURNING *`,
        [driverUid, tripId]
      );
      if (result.rows.length === 0) {
        await db.query('ROLLBACK');
        return res.status(409).json({ error: 'Trip already claimed or not available' });
      }
      const trip = result.rows[0];
      await db.query('COMMIT');

      io.to(`trip_${tripId}`).emit('trip_claimed', { driverUid, tripId });

      sendPushToUser(trip.passenger_uid, 'Driver Found!', `Your ride is on the way. Fare: UGX ${trip.fare}`, {
        trip_id: String(tripId),
        driver_uid: driverUid,
        type: 'trip_claimed'
      });

      res.json({ success: true, trip });
    } catch (error) {
      await db.query('ROLLBACK').catch(() => {});
      res.status(500).json({ error: error.message });
    }
  });

  router.get('/:id', verifyFirebaseToken, async (req, res) => {
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

  router.get('/:id/messages', verifyFirebaseToken, async (req, res) => {
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

  router.patch('/:id/status', verifyFirebaseToken, async (req, res) => {
    const parsed = tripStatusSchema.safeParse(req.body);
    if (!parsed.success) {
      return res.status(400).json({ errors: parsed.error.flatten().fieldErrors });
    }
    const { status, driver_uid, rating, comment, dispute_reason, dispute_evidence } = parsed.data;
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

      if (status === 'completed') {
        const completedTrip = result.rows[0];
        const effectiveDriverUid = completedTrip.driver_uid || driver_uid || req.user.uid;

        if (effectiveDriverUid) {
          console.log(`[EARNINGS] trip=${req.params.id} driver=${effectiveDriverUid} fare=${completedTrip.fare}`);
          await db.query(
            `UPDATE drivers
             SET earnings = earnings + $1,
                 completed_trips = completed_trips + 1,
                 updated_at = NOW()
             WHERE uid = $2`,
            [completedTrip.fare, effectiveDriverUid]
          );
          io.to(`driver_${effectiveDriverUid}`).emit('earnings_updated', {
            earned: completedTrip.fare,
            tripId: parseInt(req.params.id)
          });
        } else {
          console.warn(`[EARNINGS] trip=${req.params.id} — no driver_uid found, skipping earnings update`);
        }
      }

      io.to(`trip_${req.params.id}`).emit('trip_status_updated', { tripId: parseInt(req.params.id), status });
      res.json({ success: true, trip: result.rows[0] });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  router.post('/calculate-fare', async (req, res) => {
    const parsed = calculateFareSchema.safeParse(req.body);
    if (!parsed.success) {
      return res.status(400).json({ errors: parsed.error.flatten().fieldErrors });
    }
    const { distance_km, duration_mins } = parsed.data;
    try {
      const fare = await calculateFare(distance_km, duration_mins);
      res.json(fare);
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  return router;
};
