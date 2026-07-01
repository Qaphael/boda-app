const { z } = require('zod');

const syncUserSchema = z.object({
  phone: z.string().optional(),
  name: z.string().optional(),
  language: z.string().optional(),
  referral_code: z.string().optional(),
});

const fcmTokenSchema = z.object({
  fcm_token: z.string().min(1),
});

module.exports = { syncUserSchema, fcmTokenSchema };
