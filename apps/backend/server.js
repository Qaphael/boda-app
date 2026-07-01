const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
require('dotenv').config({ path: __dirname + '/.env' });
const { globalLimiter } = require('./config/rateLimits');
const { startTripCleanup } = require('./services/trip.cleanup');
const { registerSocketHandlers } = require('./sockets/socket.handler');
require('./config/env');
const usersRouter = require('./routes/users.routes');
const placesRouter = require('./routes/places.routes');
const createTripsRouter = require('./routes/trips.routes');
const createDriversRouter = require('./routes/drivers.routes');
const walletRouter = require('./routes/wallet.routes');
const contactsRouter = require('./routes/contacts.routes');
const referralsRouter = require('./routes/referrals.routes');
const createSosRouter = require('./routes/sos.routes');
const promosRouter = require('./routes/promos.routes');
const createAdminRouter = require('./routes/admin.routes');
require('./config/env');

const app = express();
app.set('trust proxy', 1);
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST']
  }
});

app.use(globalLimiter);
app.use(cors());
app.use(express.json());

// Serve Admin Dashboard static files
const path = require('path');
app.use('/admin', express.static(path.join(__dirname, '../admin')));

const PORT = process.env.PORT || 3000;

app.use('/api/users', usersRouter);
app.use('/api/saved-places', placesRouter);
app.use('/api/trips', createTripsRouter(io));
app.use('/api/drivers', createDriversRouter(io));
app.use('/api/wallet', walletRouter);
app.use('/api/emergency-contacts', contactsRouter);
app.use('/api/referrals', referralsRouter);
app.use('/api/sos', createSosRouter(io));
app.use('/api/promos', promosRouter);
app.use('/api/admin', createAdminRouter(io));

// Health Check Endpoint
app.get('/health', (req, res) => {
  res.status(200).json({ status: 'healthy', timestamp: new Date(), message: 'Boda Gulu API' });
});

registerSocketHandlers(io);

startTripCleanup(io);

// Start listening
server.listen(PORT, '0.0.0.0', () => {
  console.log(`===================================================`);
  console.log(`🚀 SafeBoda Gulu Backend Server Live at http://0.0.0.0:${PORT}`);
  console.log(`📡 Real-Time WebSockets configured on same port`);
  console.log(`🗄️ Connected to PostgreSQL pooling module`);
  console.log(`===================================================`);
});
