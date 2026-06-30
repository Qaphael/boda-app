const rateLimit = require('express-rate-limit');

const globalLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 200,
  standardHeaders: true,
  legacyHeaders: false,
  validate: false,
  message: { error: 'Too many requests, slow down.' },
  skip: (req) => req.path.startsWith('/api/admin') || req.path.startsWith('/admin')
});

const bookingLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 5,
  validate: false,
  message: { error: 'Too many booking attempts. Please wait.' }
});

const walletLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 10,
  validate: false,
  message: { error: 'Too many wallet requests. Please wait.' }
});

const syncLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 10,
  validate: false,
  message: { error: 'Too many sync requests.' }
});

module.exports = { globalLimiter, bookingLimiter, walletLimiter, syncLimiter };
