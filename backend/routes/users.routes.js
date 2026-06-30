const express = require('express');
const router = express.Router();
const db = require('../db');
const { verifyFirebaseToken } = require('../middleware/auth');
const { syncLimiter } = require('../config/rateLimits');
const { syncUserSchema, fcmTokenSchema } = require('../schemas/user.schema');

router.post('/sync', verifyFirebaseToken, syncLimiter, async (req, res) => {
  const parsed = syncUserSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ errors: parsed.error.flatten().fieldErrors });
  }
  const { uid } = req.user;
  const { phone, name, language, referral_code } = parsed.data;
  const fullName = name || req.user.name || 'Gulu Passenger';
  const phoneNum = phone || req.user.phone_number || null;
  console.log(`[SYNC] uid=${uid} phone=${phoneNum} name=${fullName}`);
  try {
    const query = `
      INSERT INTO users (uid, phone, full_name, email, wallet_balance, language, referral_code)
      VALUES ($1, $2, $3, $4, 0.00, $5, $6)
      ON CONFLICT (uid) DO UPDATE
      SET full_name = $3,
          phone = COALESCE(EXCLUDED.phone, users.phone),
          language = $5,
          referral_code = $6,
          updated_at = NOW()
      RETURNING *;
    `;
    const result = await db.query(query, [
      uid,
      phoneNum,
      fullName,
      req.user.email,
      language || 'en',
      referral_code || null
    ]);
    console.log(`[SYNC] Success: ${JSON.stringify(result.rows[0])}`);
    res.json({ success: true, user: result.rows[0] });
  } catch (error) {
    console.error('[SYNC] Error:', error);
    res.status(500).json({ error: 'Database sync failure: ' + error.message });
  }
});

router.post('/fcm-token', verifyFirebaseToken, async (req, res) => {
  const parsed = fcmTokenSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ errors: parsed.error.flatten().fieldErrors });
  }
  const { fcm_token } = parsed.data;
  const uid = req.user.uid;
  try {
    await db.query('UPDATE users SET fcm_token = $1, updated_at = NOW() WHERE uid = $2', [fcm_token, uid]);
    res.json({ success: true });
  } catch (error) {
    console.error('Error storing FCM token:', error);
    res.status(500).json({ error: error.message });
  }
});

router.get('/me', verifyFirebaseToken, async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM users WHERE uid = $1', [req.user.uid]);
    if (result.rows.length === 0) return res.status(404).json({ error: 'User not found' });
    res.json(result.rows[0]);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.delete('/me', verifyFirebaseToken, async (req, res) => {
  try {
    const uid = req.user.uid;
    await db.query('DELETE FROM transactions WHERE user_uid = $1', [uid]);
    await db.query('DELETE FROM trips WHERE passenger_uid = $1', [uid]);
    await db.query('DELETE FROM emergency_contacts WHERE user_uid = $1', [uid]);
    await db.query('DELETE FROM referrals WHERE referrer_uid = $1', [uid]);
    await db.query('DELETE FROM saved_places WHERE user_uid = $1', [uid]);
    await db.query('DELETE FROM users WHERE uid = $1', [uid]);
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
