const express = require('express');
const db = require('../db');
const { verifyFirebaseToken } = require('../middleware/auth');

module.exports = function createSosRouter(io) {
  const router = express.Router();

  router.post('/', verifyFirebaseToken, async (req, res) => {
    const { latitude, longitude, trip_id, description } = req.body;
    const alertId = `SOS-${Date.now()}-${Math.floor(1000 + Math.random() * 9000)}`;
    try {
      const result = await db.query(
        `INSERT INTO sos_alerts (id, user_uid, latitude, longitude, trip_id, description)
         VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
        [alertId, req.user.uid, latitude || null, longitude || null, trip_id || null, description || 'SOS Emergency']
      );
      io.emit('sos_alert_created', result.rows[0]);
      res.json({ success: true, alert: result.rows[0] });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  return router;
};
