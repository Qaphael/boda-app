const db = require('../db');

function registerSocketHandlers(io) {
  io.on('connection', (socket) => {
    console.log(`🔌 New Web Socket Client Connected: ${socket.id}`);

    socket.on('join_trip_channel', (tripId) => {
      socket.join(`trip_${tripId}`);
      console.log(`Client joined real-time channel for trip_${tripId}`);
    });

    socket.on('update_live_gps', (data) => {
      io.to(`trip_${data.tripId}`).emit('live_gps_broadcast', data);
      db.query('UPDATE drivers SET latitude = $1, longitude = $2 WHERE uid = (SELECT driver_uid FROM trips WHERE id = $3)', [data.latitude, data.longitude, data.tripId])
        .catch(err => console.error('Silent postgis-log fail:', err.message));
    });

    socket.on('chat_message', async (data) => {
      const { tripId, senderUid, senderName, senderRole, message } = data;
      if (!tripId || !message || !senderUid) return;

      try {
        const result = await db.query(
          `INSERT INTO chat_messages (trip_id, sender_uid, sender_name, sender_role, message)
           VALUES ($1, $2, $3, $4, $5) RETURNING *`,
          [tripId, senderUid, senderName, senderRole, message]
        );
        const saved = result.rows[0];
        io.to(`trip_${tripId}`).emit('chat_message', {
          id: saved.id,
          tripId: saved.trip_id,
          senderUid: saved.sender_uid,
          senderName: saved.sender_name,
          senderRole: saved.sender_role,
          message: saved.message,
          createdAt: saved.created_at
        });
      } catch (err) {
        console.error('Chat message save failed:', err.message);
      }
    });

    socket.on('chat_typing', (data) => {
      const { tripId, senderUid, senderName, isTyping } = data;
      if (!tripId) return;
      socket.to(`trip_${tripId}`).emit('chat_typing', { tripId, senderUid, senderName, isTyping });
    });

    socket.on('disconnect', () => {
      console.log(`🔌 Client disconnected: ${socket.id}`);
    });
  });
}

module.exports = { registerSocketHandlers };
