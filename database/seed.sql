-- =========================================================
-- seed.sql
-- Initial seed data
-- =========================================================

BEGIN;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES (
    'factory_phone',
    '09120000000',
    'Main contact number of the factory shown in product details'
)
ON CONFLICT (setting_key)
DO UPDATE SET
    setting_value = EXCLUDED.setting_value,
    description = EXCLUDED.description,
    updated_at = NOW();

COMMIT;
