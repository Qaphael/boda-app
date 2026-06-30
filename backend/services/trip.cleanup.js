const db = require('../db');
const { sendPushToUser } = require('./push.service');

function startTripCleanup(io) {
  setInterval(async () => {
    try {
      const result = await db.query(
        `UPDATE trips SET status = 'cancelled'
         WHERE status = 'matching' AND driver_uid IS NULL
         AND created_at < NOW() - INTERVAL '90 seconds'
         RETURNING id, passenger_uid, pickup_name, dropoff_name`
      );
      for (const trip of result.rows) {
        io.to(`trip_${trip.id}`).emit('trip_unmatched', { tripId: trip.id });
        sendPushToUser(trip.passenger_uid, 'No Driver Found', `No driver accepted your ride from ${trip.pickup_name}. Please try again.`, {
          trip_id: String(trip.id), type: 'trip_unmatched'
        });
      }
      if (result.rows.length > 0) {
        console.log(`⏱️ Auto-cancelled ${result.rows.length} unclaimed trips`);
      }
    } catch (err) {
      console.error('Unclaimed trip cleanup error:', err.message);
    }
  }, 30000);
}

module.exports = { startTripCleanup };
