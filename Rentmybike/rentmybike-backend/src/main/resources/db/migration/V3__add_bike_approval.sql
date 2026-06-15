-- ============================================================
-- V3: Add bike approval workflow columns
-- V3: Fahrrad-Genehmigungsworkflow-Spalten hinzufügen
--
-- Idempotent: all statements use IF NOT EXISTS / DO blocks
-- so this migration is safe to re-run in any environment.
-- Idempotent: alle Anweisungen verwenden IF NOT EXISTS / DO-Blöcke.
-- ============================================================

-- 1. Create approval_status ENUM type if it doesn't exist
--    approval_status ENUM-Typ erstellen falls nicht vorhanden
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'approval_status') THEN
        CREATE TYPE approval_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
    END IF;
END $$;

-- 2. Add approval_status column to bikes table
--    approval_status-Spalte zur bikes-Tabelle hinzufügen
ALTER TABLE bikes
    ADD COLUMN IF NOT EXISTS approval_status approval_status NOT NULL DEFAULT 'PENDING';

-- 3. Add rejection_reason column (text, nullable)
--    rejection_reason-Spalte hinzufügen (Text, nullable)
ALTER TABLE bikes
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

-- 4. Add is_primary column to bike_photos if not present
--    is_primary-Spalte zu bike_photos hinzufügen falls nicht vorhanden
ALTER TABLE bike_photos
    ADD COLUMN IF NOT EXISTS is_primary BOOLEAN NOT NULL DEFAULT FALSE;

-- 5. Add display_order column to bike_photos if not present
--    display_order-Spalte zu bike_photos hinzufügen falls nicht vorhanden
ALTER TABLE bike_photos
    ADD COLUMN IF NOT EXISTS display_order INTEGER NOT NULL DEFAULT 0;

-- 6. Index for fast admin moderation queue queries (PENDING first)
--    Index für schnelle Admin-Moderationswarteschlange-Abfragen (PENDING zuerst)
CREATE INDEX IF NOT EXISTS idx_bikes_approval_status
    ON bikes (approval_status)
    WHERE deleted_at IS NULL;

-- 7. Index for public search: only APPROVED + available bikes
--    Index für öffentliche Suche: nur APPROVED + verfügbare Fahrräder
CREATE INDEX IF NOT EXISTS idx_bikes_public_search
    ON bikes (approval_status, is_available, city, created_at DESC)
    WHERE deleted_at IS NULL;
