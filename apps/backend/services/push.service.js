const db = require('../db');
const { getFirebaseAdmin } = require('../middleware/auth');

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

module.exports = { sendPushToUser, sendPushToDriver };
