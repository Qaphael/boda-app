const { z } = require('zod');

const validatePromoSchema = z.object({
  code: z.string().min(1),
  original_fare: z.number().positive(),
});

const createPromoSchema = z.object({
  code: z.string().min(1),
  discount_type: z.enum(['percent', 'fixed']),
  value: z.number().positive(),
});

module.exports = { validatePromoSchema, createPromoSchema };
