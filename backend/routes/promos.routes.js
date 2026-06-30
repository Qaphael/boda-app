const express = require('express');
const router = express.Router();
const db = require('../db');
const { validatePromoSchema } = require('../schemas/promo.schema');

router.post('/validate', async (req, res) => {
  const parsed = validatePromoSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ errors: parsed.error.flatten().fieldErrors });
  }
  const { code, original_fare } = parsed.data;
  try {
    const result = await db.query('SELECT * FROM promo_codes WHERE UPPER(code) = UPPER($1) AND active = true', [code]);
    const promo = result.rows[0];
    if (!promo) {
      return res.status(404).json({ valid: false, message: 'Invalid or expired promo code' });
    }
    let discount = 0;
    if (promo.discount_type === 'percent') {
      discount = Math.round((parseFloat(original_fare) * parseFloat(promo.value)) / 100);
    } else {
      discount = parseFloat(promo.value);
    }
    discount = Math.min(discount, original_fare);
    res.json({
      valid: true,
      code: promo.code,
      discount_amount: discount,
      final_fare: original_fare - discount
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
