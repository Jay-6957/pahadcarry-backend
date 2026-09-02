-- V2: Seed Data for Haldwani Central Hub and Baseline Rate Card Config

INSERT INTO hub_centers (id, name, lat, lng, serves_areas)
VALUES (
    'hub_haldwani_central',
    'Haldwani Central Hub',
    29.2183,
    79.5130,
    '["Nainital", "Bhowali", "Bhimtal", "Almora", "Kathgodam"]'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO admin_users (id, name, email, password_hash, role, created_at)
VALUES (
    'admin_haldwani_super',
    'Kumaon Ops Commander',
    'ops@pahadcarry.in',
    '$2a$10$wE8wY5zB84uL2Oq.7bL1O.O2iZ6H4Wc4YI.e2Q0O9R1t9Qx2y5q.i', -- Hash of 'admin123'
    'SUPER_ADMIN',
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

INSERT INTO system_config (id, base_fare, per_km_rate, per_kg_rate, max_standard_weight_kg, batch_order_threshold, batch_max_wait_hours, payout_strategy, payout_params_json, updated_at)
VALUES (
    1,
    150.00,
    18.00,
    4.00,
    100.00,
    8,
    4,
    'BASE_PLUS_PER_STOP',
    '{"baseBatchFee": 300, "perStopBonus": 50, "abortedStopFee": 100}',
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
