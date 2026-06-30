const express = require('express');
const router = express.Router();
const db = require('../db');
const { verifyFirebaseToken } = require('../middleware/auth');

router.get('/', verifyFirebaseToken, async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM saved_places WHERE user_uid = $1 ORDER BY id DESC', [req.user.uid]);
    res.json(result.rows);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.post('/', verifyFirebaseToken, async (req, res) => {
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

router.delete('/:id', verifyFirebaseToken, async (req, res) => {
  try {
    await db.query('DELETE FROM saved_places WHERE id = $1 AND user_uid = $2', [req.params.id, req.user.uid]);
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
