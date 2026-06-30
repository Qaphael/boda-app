const { z } = require('zod');

const topupSchema = z.object({
  amount: z.number().positive(),
  payment_provider: z.string().optional(),
});

const paySchema = z.object({
  amount: z.number().positive(),
  payment_provider: z.string().min(1),
  trip_id: z.number().int().positive().optional(),
});

module.exports = { topupSchema, paySchema };
