-- ============================================================
-- V2: Add email verification and ban fields to users table
-- V2: E-Mail-Verifizierungs- und Sperrfelder zur Benutzertabelle hinzufügen
--
-- This migration is IDEMPOTENT using ADD COLUMN IF NOT EXISTS.
-- Diese Migration ist IDEMPOTENT durch ADD COLUMN IF NOT EXISTS.
-- Safe to run even if V1 already includes these columns.
-- Kann sicher ausgeführt werden, auch wenn V1 diese Spalten bereits enthält.
-- ============================================================

-- Email verification token (UUID string, cleared after verification)
-- E-Mail-Verifizierungstoken (UUID-String, nach Verifizierung gelöscht)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verification_token     VARCHAR(100),
    ADD COLUMN IF NOT EXISTS email_verification_token_expiry TIMESTAMP,
    ADD COLUMN IF NOT EXISTS email_verified               BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS banned_at                    TIMESTAMP;

-- Index for fast token lookup during verification
-- Index für schnelle Token-Suche während der Verifizierung
CREATE INDEX IF NOT EXISTS idx_users_email_verification_token
    ON users (email_verification_token)
    WHERE email_verification_token IS NOT NULL;

-- ============================================================
-- Notes / Hinweise:
--
-- email_verified = FALSE by default until the user clicks the link
-- email_verified = FALSE standardmäßig bis der Benutzer auf den Link klickt
--
-- email_verification_token is NULLed after successful verification
-- email_verification_token wird nach erfolgreicher Verifizierung auf NULL gesetzt
--
-- banned_at IS NULL = not banned; IS NOT NULL = banned by admin
-- banned_at IS NULL = nicht gesperrt; IS NOT NULL = von Admin gesperrt
-- ============================================================
