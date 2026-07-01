const express = require('express');
const router = express.Router();
const db = require('../db');
const { verifyFirebaseToken } = require('../middleware/auth');
const { walletLimiter } = require('../config/rateLimits');
const { topupSchema, paySchema } = require('../schemas/wallet.schema');

router.get('/transactions', verifyFirebaseToken, async (req, res) => {
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

router.post('/topup', verifyFirebaseToken, walletLimiter, async (req, res) => {
  const parsed = topupSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ errors: parsed.error.flatten().fieldErrors });
  }
  const { amount, payment_provider } = parsed.data;
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

router.post('/pay', verifyFirebaseToken, walletLimiter, async (req, res) => {
  const parsed = paySchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ errors: parsed.error.flatten().fieldErrors });
  }
  const { amount, payment_provider, trip_id } = parsed.data;
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

module.exports = router;
