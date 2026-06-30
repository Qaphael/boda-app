const { z } = require('zod');

const pricingUpdateSchema = z.object({
  multiplier: z.number().positive().optional(),
  reason: z.string().optional(),
  base_fare: z.number().int().positive().optional(),
  rate_per_km: z.number().int().positive().optional(),
  rate_per_min: z.number().int().positive().optional(),
});

const riderCreditSchema = z.object({
  amount: z.number().positive(),
});

module.exports = { pricingUpdateSchema, riderCreditSchema };
