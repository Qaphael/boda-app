const express = require('express');
const router = express.Router();
const db = require('../db');
const { verifyFirebaseToken } = require('../middleware/auth');
const { createContactSchema } = require('../schemas/contact.schema');

router.get('/', verifyFirebaseToken, async (req, res) => {
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

router.post('/', verifyFirebaseToken, async (req, res) => {
  const parsed = createContactSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ errors: parsed.error.flatten().fieldErrors });
  }
  const { name, phone_number } = parsed.data;
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

router.delete('/:id', verifyFirebaseToken, async (req, res) => {
  try {
    await db.query('DELETE FROM emergency_contacts WHERE id = $1 AND user_uid = $2', [req.params.id, req.user.uid]);
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
