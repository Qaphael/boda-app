-- Boda Gulu Production PostgreSQL Database Schema
-- Run these commands on your VPS PostgreSQL instance to prepare the database.

-- Enable PostGIS extension if you plan to use advanced spatial queries
-- CREATE EXTENSION IF NOT EXISTS postgis;

-- 1. Users (Riders/Passengers)
CREATE TABLE IF NOT EXISTS users (
    uid VARCHAR(128) PRIMARY KEY, -- Matches Firebase Auth UID
    phone VARCHAR(20) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    wallet_balance DECIMAL(12,2) DEFAULT 0.00,
    status VARCHAR(20) DEFAULT 'Active',
    fcm_token VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Drivers (Boda Operators)
CREATE TABLE IF NOT EXISTS drivers (
    uid VARCHAR(128) PRIMARY KEY, -- Matches Firebase Auth UID (if driver app exists) or a unique system ID
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    plate_number VARCHAR(15) UNIQUE NOT NULL,
    helmet_number VARCHAR(15) UNIQUE NOT NULL,
    is_online BOOLEAN DEFAULT false,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    rating DECIMAL(3,2) DEFAULT 5.00,
    completed_trips INTEGER DEFAULT 0,
    earnings DECIMAL(12,2) DEFAULT 0.00,
    verified BOOLEAN DEFAULT false,
    fcm_token VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Saved Places
CREATE TABLE IF NOT EXISTS saved_places (
    id SERIAL PRIMARY KEY,
    user_uid VARCHAR(128) REFERENCES users(uid) ON DELETE CASCADE,
    label VARCHAR(50) NOT NULL, -- e.g., 'Home', 'Work', 'Lacor'
    name VARCHAR(255) NOT NULL, -- Full descriptive address
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Trips
CREATE TABLE IF NOT EXISTS trips (
    id SERIAL PRIMARY KEY,
    trip_code VARCHAR(15) UNIQUE NOT NULL, -- e.g., 'TRP-4569-GULU'
    passenger_uid VARCHAR(128) REFERENCES users(uid),
    driver_uid VARCHAR(128) REFERENCES drivers(uid),
    pickup_name VARCHAR(255) NOT NULL,
    pickup_lat DOUBLE PRECISION NOT NULL,
    pickup_lon DOUBLE PRECISION NOT NULL,
    dropoff_name VARCHAR(255) NOT NULL,
    dropoff_lat DOUBLE PRECISION NOT NULL,
    dropoff_lon DOUBLE PRECISION NOT NULL,
    distance_km DECIMAL(6,2) NOT NULL,
    duration_mins INTEGER NOT NULL,
    fare DECIMAL(10,2) NOT NULL,
    promo_applied VARCHAR(30),
    payment_method VARCHAR(20) NOT NULL, -- 'MTN', 'Airtel', 'Wallet'
    status VARCHAR(20) NOT NULL, -- 'pending', 'matching', 'en_route', 'active', 'completed', 'cancelled'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

-- 5. Wallet & Mobile Money Transactions
CREATE TABLE IF NOT EXISTS transactions (
    id SERIAL PRIMARY KEY,
    user_uid VARCHAR(128) REFERENCES users(uid) ON DELETE CASCADE,
    transaction_ref VARCHAR(50) UNIQUE NOT NULL, -- Mobile money reference ID
    type VARCHAR(20) NOT NULL, -- 'deposit', 'withdrawal', 'payment', 'payout'
    amount DECIMAL(10,2) NOT NULL,
    payment_provider VARCHAR(20) NOT NULL, -- 'MTN', 'Airtel', 'InternalWallet'
    status VARCHAR(20) NOT NULL, -- 'pending', 'completed', 'failed'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for spatial query optimization (if PostGIS isn't installed, we can still query lats/lons)
CREATE INDEX IF NOT EXISTS idx_drivers_coords ON drivers(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_trips_passenger ON trips(passenger_uid);
CREATE INDEX IF NOT EXISTS idx_trips_driver ON trips(driver_uid);

-- 6. Chat Messages (Rider ↔ Driver real-time chat per trip)
CREATE TABLE IF NOT EXISTS chat_messages (
    id SERIAL PRIMARY KEY,
    trip_id INTEGER NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    sender_uid VARCHAR(128) NOT NULL,
    sender_name VARCHAR(100) NOT NULL,
    sender_role VARCHAR(10) NOT NULL, -- 'rider' or 'driver'
    message TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_chat_trip ON chat_messages(trip_id);

-- 7. Emergency Contacts
CREATE TABLE IF NOT EXISTS emergency_contacts (
    id SERIAL PRIMARY KEY,
    user_uid VARCHAR(128) NOT NULL REFERENCES users(uid) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ec_user ON emergency_contacts(user_uid);

-- 8. Referrals
CREATE TABLE IF NOT EXISTS referrals (
    id SERIAL PRIMARY KEY,
    referrer_uid VARCHAR(128) NOT NULL REFERENCES users(uid),
    referred_name VARCHAR(100) NOT NULL,
    referred_phone VARCHAR(20) NOT NULL,
    referral_code VARCHAR(50),
    status VARCHAR(20) DEFAULT 'pending',
    reward_amount DECIMAL(10,2) DEFAULT 3000.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ref_user ON referrals(referrer_uid);

-- Migration: add columns if not present
DO $$ BEGIN
  ALTER TABLE users ADD COLUMN IF NOT EXISTS fcm_token VARCHAR(512);
EXCEPTION WHEN duplicate_column THEN NULL;
END $$;
DO $$ BEGIN
  ALTER TABLE drivers ADD COLUMN IF NOT EXISTS fcm_token VARCHAR(512);
EXCEPTION WHEN duplicate_column THEN NULL;
END $$;
DO $$ BEGIN
  ALTER TABLE trips ADD COLUMN IF NOT EXISTS rating INTEGER DEFAULT 0;
EXCEPTION WHEN duplicate_column THEN NULL;
END $$;
DO $$ BEGIN
  ALTER TABLE trips ADD COLUMN IF NOT EXISTS comment TEXT;
EXCEPTION WHEN duplicate_column THEN NULL;
END $$;
DO $$ BEGIN
  ALTER TABLE trips ADD COLUMN IF NOT EXISTS dispute_reason TEXT;
EXCEPTION WHEN duplicate_column THEN NULL;
END $$;
DO $$ BEGIN
  ALTER TABLE trips ADD COLUMN IF NOT EXISTS dispute_evidence TEXT;
EXCEPTION WHEN duplicate_column THEN NULL;
END $$;
DO $$ BEGIN
  ALTER TABLE users ADD COLUMN IF NOT EXISTS language VARCHAR(5) DEFAULT 'en';
EXCEPTION WHEN duplicate_column THEN NULL;
END $$;
DO $$ BEGIN
  ALTER TABLE users ADD COLUMN IF NOT EXISTS referral_code VARCHAR(50);
EXCEPTION WHEN duplicate_column THEN NULL;
END $$;

-- 9. Pricing Settings (singleton row)
CREATE TABLE IF NOT EXISTS pricing_settings (
    id SERIAL PRIMARY KEY,
    base_fare INTEGER NOT NULL DEFAULT 1500,
    rate_per_km INTEGER NOT NULL DEFAULT 1000,
    rate_per_min INTEGER NOT NULL DEFAULT 150,
    surge_multiplier DECIMAL(4,2) NOT NULL DEFAULT 1.00,
    surge_reason VARCHAR(50) DEFAULT 'Normal',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
INSERT INTO pricing_settings DEFAULT VALUES ON CONFLICT DO NOTHING;

-- 10. Promo Codes
CREATE TABLE IF NOT EXISTS promo_codes (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) UNIQUE NOT NULL,
    discount_type VARCHAR(10) NOT NULL,
    value DECIMAL(10,2) NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 11. SOS Alerts
CREATE TABLE IF NOT EXISTS sos_alerts (
    id VARCHAR(50) PRIMARY KEY,
    user_uid VARCHAR(128) REFERENCES users(uid),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    trip_id INTEGER REFERENCES trips(id),
    description TEXT,
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
