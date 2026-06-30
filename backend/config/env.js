const { z } = require('zod');

const envSchema = z.object({
  PORT: z.string().default('3000'),
  DB_HOST: z.string().default('localhost'),
  DB_PORT: z.string().default('5432'),
  DB_USER: z.string().default('postgres'),
  DB_PASSWORD: z.string().default('password'),
  DB_NAME: z.string().default('boda_gulu'),
  DB_SSL: z.string().default('false'),
  FIREBASE_SERVICE_ACCOUNT_PATH: z.string().optional(),
  ADMIN_SECRET_KEY: z.string().optional(),
});

const env = envSchema.parse(process.env);
module.exports = { env };
