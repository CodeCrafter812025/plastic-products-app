-- =========================================================
-- down.sql
-- Rollback script
-- =========================================================

BEGIN;

DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS stock_histories;
DROP TABLE IF EXISTS price_histories;
DROP TABLE IF EXISTS order_status_histories;
DROP TABLE IF EXISTS order_assignments;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS account_deletion_requests;
DROP TABLE IF EXISTS otp_codes;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS system_settings;
DROP TABLE IF EXISTS users;

DROP FUNCTION IF EXISTS validate_order_status_transition();
DROP FUNCTION IF EXISTS sync_order_visitor();
DROP FUNCTION IF EXISTS insert_initial_product_history();
DROP FUNCTION IF EXISTS refresh_order_total_trigger();
DROP FUNCTION IF EXISTS recalculate_order_total(BIGINT);
DROP FUNCTION IF EXISTS set_updated_at();

DROP TYPE IF EXISTS notification_type_enum;
DROP TYPE IF EXISTS stock_reason_enum;
DROP TYPE IF EXISTS deletion_status_enum;
DROP TYPE IF EXISTS otp_purpose_enum;
DROP TYPE IF EXISTS product_quality_enum;
DROP TYPE IF EXISTS order_status_enum;
DROP TYPE IF EXISTS user_role_enum;

DROP EXTENSION IF EXISTS pg_trgm;

COMMIT;
