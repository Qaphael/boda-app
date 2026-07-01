const { z } = require('zod');

const createContactSchema = z.object({
  name: z.string().min(1),
  phone_number: z.string().min(1),
});

module.exports = { createContactSchema };
