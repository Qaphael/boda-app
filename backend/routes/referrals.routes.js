const express = require('express');
const router = express.Router();
const db = require('../db');
const { verifyFirebaseToken } = require('../middleware/auth');
const { createReferralSchema } = require('../schemas/referral.schema');

router.get('/', verifyFirebaseToken, async (req, res) => {
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

router.post('/', verifyFirebaseToken, async (req, res) => {
  const parsed = createReferralSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ errors: parsed.error.flatten().fieldErrors });
  }
  const { referred_name, referred_phone, referral_code } = parsed.data;
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

router.post('/:id/complete', verifyFirebaseToken, async (req, res) => {
  const referralId = parseInt(req.params.id);
  try {
    const refResult = await db.query('SELECT * FROM referrals WHERE id = $1', [referralId]);
    if (refResult.rows.length === 0) return res.status(404).json({ error: 'Referral not found' });
    const ref = refResult.rows[0];
    if (ref.status === 'completed') return res.json({ success: true, message: 'Already completed' });

    if (!ref.referrer_uid) {
      return res.status(422).json({ error: 'Referral record has no referrer — cannot credit reward' });
    }

    await db.query('BEGIN');
    await db.query(
      'UPDATE referrals SET status = \'completed\', updated_at = NOW() WHERE id = $1',
      [referralId]
    );
    const rewardAmt = parseFloat(ref.reward_amount) || 3000.00;
    console.log(`[REFERRAL] id=${referralId} crediting uid=${ref.referrer_uid} amount=${rewardAmt}`);
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

    const newUserResult = await db.query(
      'SELECT uid FROM users WHERE phone = $1',
      [ref.referred_phone]
    );
    if (newUserResult.rows.length > 0) {
      const newUserUid = newUserResult.rows[0].uid;
      const newUserTxRef = `REF-NEWUSER-${referralId}-${Date.now()}`;
      await db.query(
        `INSERT INTO transactions (user_uid, transaction_ref, type, amount, payment_provider, status)
         VALUES ($1, $2, 'bonus', $3, 'Wallet', 'completed')`,
        [newUserUid, newUserTxRef, rewardAmt]
      );
      await db.query(
        'UPDATE users SET wallet_balance = wallet_balance + $1, updated_at = NOW() WHERE uid = $2',
        [rewardAmt, newUserUid]
      );
    }

    await db.query('COMMIT');
    res.json({ success: true, reward: rewardAmt, referrer_uid: ref.referrer_uid });
  } catch (error) {
    await db.query('ROLLBACK').catch(() => {});
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
