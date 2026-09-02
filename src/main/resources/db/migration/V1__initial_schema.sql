-- V1: Initial Relational Schema for PahadCarry Platform

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) UNIQUE NOT NULL,
    fcm_token VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS addresses (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label VARCHAR(50) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    village_or_town VARCHAR(100) NOT NULL,
    landmark VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS admin_users (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS drivers (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) UNIQUE NOT NULL,
    photo_url VARCHAR(500),
    vehicle_type VARCHAR(50) NOT NULL,
    vehicle_reg_number VARCHAR(30) NOT NULL,
    vehicle_capacity_kg INTEGER NOT NULL,
    vehicle_photo_url VARCHAR(500),
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    aadhaar_number VARCHAR(20),
    aadhaar_doc_url VARCHAR(500),
    license_number VARCHAR(30),
    license_doc_url VARCHAR(500),
    rejection_reason TEXT,
    reviewed_by VARCHAR(36) REFERENCES admin_users(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    availability_status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    home_base_area VARCHAR(100) NOT NULL,
    current_lat DOUBLE PRECISION,
    current_lng DOUBLE PRECISION,
    fcm_token VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hub_centers (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    serves_areas TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS batches (
    id VARCHAR(36) PRIMARY KEY,
    area_cluster VARCHAR(100) NOT NULL,
    batch_date DATE NOT NULL,
    driver_id VARCHAR(36) REFERENCES drivers(id),
    hub_center_id VARCHAR(36) NOT NULL REFERENCES hub_centers(id),
    status VARCHAR(30) NOT NULL DEFAULT 'FORMING',
    total_weight_kg NUMERIC(8,2) NOT NULL DEFAULT 0.0,
    driver_payout NUMERIC(10,2),
    payout_strategy_used VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    assigned_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL REFERENCES users(id),
    pickup_lat DOUBLE PRECISION NOT NULL,
    pickup_lng DOUBLE PRECISION NOT NULL,
    pickup_address TEXT NOT NULL,
    pickup_contact_name VARCHAR(100) NOT NULL,
    pickup_contact_phone VARCHAR(15) NOT NULL,
    drop_lat DOUBLE PRECISION NOT NULL,
    drop_lng DOUBLE PRECISION NOT NULL,
    drop_address TEXT NOT NULL,
    drop_contact_name VARCHAR(100) NOT NULL,
    drop_contact_phone VARCHAR(15) NOT NULL,
    goods_description TEXT NOT NULL,
    estimated_weight_kg NUMERIC(8,2) NOT NULL,
    quantity_note VARCHAR(255),
    order_type VARCHAR(30) NOT NULL DEFAULT 'STANDARD_POOL',
    status VARCHAR(30) NOT NULL DEFAULT 'PLACED',
    batch_id VARCHAR(36) REFERENCES batches(id),
    price_estimate NUMERIC(10,2) NOT NULL,
    final_price NUMERIC(10,2),
    payment_status VARCHAR(30) NOT NULL DEFAULT 'COD_PENDING',
    placed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    batched_at TIMESTAMP WITH TIME ZONE,
    picked_up_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS order_photos (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    photo_url VARCHAR(500) NOT NULL,
    photo_type VARCHAR(30) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS batch_stops (
    id VARCHAR(36) PRIMARY KEY,
    batch_id VARCHAR(36) NOT NULL REFERENCES batches(id) ON DELETE CASCADE,
    order_id VARCHAR(36) REFERENCES orders(id),
    sequence INTEGER NOT NULL,
    stop_type VARCHAR(20) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    address TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    proof_photo_url VARCHAR(500),
    cash_collected NUMERIC(10,2),
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS system_config (
    id INTEGER PRIMARY KEY,
    base_fare NUMERIC(10,2) NOT NULL DEFAULT 150.00,
    per_km_rate NUMERIC(10,2) NOT NULL DEFAULT 18.00,
    per_kg_rate NUMERIC(10,2) NOT NULL DEFAULT 4.00,
    max_standard_weight_kg NUMERIC(8,2) NOT NULL DEFAULT 100.00,
    batch_order_threshold INTEGER NOT NULL DEFAULT 8,
    batch_max_wait_hours INTEGER NOT NULL DEFAULT 4,
    payout_strategy VARCHAR(50) NOT NULL DEFAULT 'BASE_PLUS_PER_STOP',
    payout_params_json TEXT NOT NULL DEFAULT '{"baseBatchFee": 300, "perStopBonus": 50, "abortedStopFee": 100}',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
