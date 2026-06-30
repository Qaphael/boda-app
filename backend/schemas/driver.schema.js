const { z } = require('zod');

const registerDriverSchema = z.object({
  full_name: z.string().min(1),
  phone: z.string().min(1),
  plate_number: z.string().min(1),
  helmet_number: z.string().optional(),
});

const driverStatusSchema = z.object({
  is_online: z.boolean(),
  latitude: z.number().min(-90).max(90).optional(),
  longitude: z.number().min(-180).max(180).optional(),
});

module.exports = { registerDriverSchema, driverStatusSchema };
