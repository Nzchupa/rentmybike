-- ============================================================
-- V5: Convert PostgreSQL native ENUMs to VARCHAR
-- V5: Konvertierung von PostgreSQL nativen ENUMs zu VARCHAR
--
-- Reason: Hibernate 6 (Spring Boot 3) cannot write to PostgreSQL
-- custom ENUM types via JDBC setObject(). VARCHAR is required.
--
-- Order matters:
-- 1. Drop column DEFAULTs that reference enum types
-- 2. Convert column types to VARCHAR
-- 3. Restore plain-string DEFAULTs
-- 4. DROP enum types
-- ============================================================

-- ── Step 1: Drop defaults that reference enum types ──────────────────────────
ALTER TABLE users         ALTER COLUMN role            DROP DEFAULT;
ALTER TABLE bikes         ALTER COLUMN category        DROP DEFAULT;
ALTER TABLE bikes         ALTER COLUMN approval_status DROP DEFAULT;
ALTER TABLE bookings      ALTER COLUMN status          DROP DEFAULT;

-- ── Step 2: Convert columns to VARCHAR ───────────────────────────────────────
ALTER TABLE users
    ALTER COLUMN role TYPE VARCHAR(20) USING role::VARCHAR;

ALTER TABLE bikes
    ALTER COLUMN category TYPE VARCHAR(50) USING category::VARCHAR;

ALTER TABLE bikes
    ALTER COLUMN approval_status TYPE VARCHAR(20) USING approval_status::VARCHAR;

ALTER TABLE bookings
    ALTER COLUMN status TYPE VARCHAR(20) USING status::VARCHAR;

ALTER TABLE reviews
    ALTER COLUMN type TYPE VARCHAR(20) USING type::VARCHAR;

-- ── Step 3: Restore plain-string defaults ────────────────────────────────────
ALTER TABLE users    ALTER COLUMN role            SET DEFAULT 'USER';
ALTER TABLE bikes    ALTER COLUMN approval_status SET DEFAULT 'PENDING';
ALTER TABLE bookings ALTER COLUMN status          SET DEFAULT 'PENDING';

-- ── Step 4: Drop enum types (CASCADE removes any remaining dependencies) ─────
DROP TYPE IF EXISTS user_role       CASCADE;
DROP TYPE IF EXISTS bike_category   CASCADE;
DROP TYPE IF EXISTS booking_status  CASCADE;
DROP TYPE IF EXISTS review_type     CASCADE;
DROP TYPE IF EXISTS approval_status CASCADE;
