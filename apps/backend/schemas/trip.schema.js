const { z } = require('zod');

const bookTripSchema = z.object({
  pickup_name: z.string().min(2),
  pickup_lat: z.number().min(-90).max(90),
  pickup_lon: z.number().min(-180).max(180),
  dropoff_name: z.string().min(2),
  dropoff_lat: z.number().min(-90).max(90),
  dropoff_lon: z.number().min(-180).max(180),
  distance_km: z.number().positive(),
  duration_mins: z.number().int().positive(),
  fare: z.number().positive(),
  payment_method: z.enum(['MTN', 'Airtel', 'Wallet']),
  promo_applied: z.string().optional(),
});

const tripStatusSchema = z.object({
  status: z.enum(['pending','matching','en_route','active','completed','cancelled','disputed']),
  driver_uid: z.string().optional(),
  rating: z.number().int().min(1).max(5).optional(),
  comment: z.string().optional(),
  dispute_reason: z.string().optional(),
  dispute_evidence: z.string().optional(),
});

const calculateFareSchema = z.object({
  distance_km: z.number().min(0),
  duration_mins: z.number().int().min(0),
});

module.exports = { bookTripSchema, tripStatusSchema, calculateFareSchema };
