const express = require('express');
const db = require('../db');
const { verifyFirebaseToken } = require('../middleware/auth');
const { registerDriverSchema, driverStatusSchema } = require('../schemas/driver.schema');

module.exports = function createDriversRouter(io) {
  const router = express.Router();

  router.post('/register', verifyFirebaseToken, async (req, res) => {
    const parsed = registerDriverSchema.safeParse(req.body);
    if (!parsed.success) {
      return res.status(400).json({ errors: parsed.error.flatten().fieldErrors });
    }
    const uid = req.user.uid;
    const { full_name, phone, plate_number, helmet_number } = parsed.data;
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

  router.post('/status', verifyFirebaseToken, async (req, res) => {
    const parsed = driverStatusSchema.safeParse(req.body);
    if (!parsed.success) {
      return res.status(400).json({ errors: parsed.error.flatten().fieldErrors });
    }
    const uid = req.user.uid;
    const { is_online, latitude, longitude } = parsed.data;
    try {
      const query = `
        UPDATE drivers
        SET is_online = $2, latitude = $3, longitude = $4, updated_at = NOW()
        WHERE uid = $1 RETURNING *;
      `;
      const result = await db.query(query, [uid, is_online, latitude, longitude]);
      io.emit('driver_location_update', { uid, is_online, latitude, longitude });
      res.json({ success: true, driver: result.rows[0] });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  return router;
};
