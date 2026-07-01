const { z } = require('zod');

const createReferralSchema = z.object({
  referred_name: z.string().min(1),
  referred_phone: z.string().min(1),
  referral_code: z.string().min(1),
});

module.exports = { createReferralSchema };
