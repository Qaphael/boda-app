const db = require('../db');

async function calculateFare(distance_km, duration_mins) {
  const result = await db.query('SELECT * FROM pricing_settings LIMIT 1');
  const ps = result.rows[0] || { base_fare: 1500, rate_per_km: 1000, rate_per_min: 150, surge_multiplier: 1.0, surge_reason: 'Normal' };
  const rawFare = ps.base_fare +
                  (parseFloat(distance_km || 0) * ps.rate_per_km) +
                  (parseInt(duration_mins || 0) * ps.rate_per_min);
  const surgeFare = Math.round(rawFare * ps.surge_multiplier);
  return {
    base_fare: ps.base_fare,
    distance_km,
    duration_mins,
    surge_multiplier: ps.surge_multiplier,
    surge_reason: ps.surge_reason,
    original_fare: rawFare,
    final_fare: surgeFare
  };
}

module.exports = { calculateFare };
