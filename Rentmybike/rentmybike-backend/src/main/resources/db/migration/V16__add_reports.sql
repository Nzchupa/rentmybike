-- Reports: user-filed complaints about bikes, other users, or reviews,
-- triaged and resolved by admins.
-- Meldungen: von Benutzern eingereichte Beschwerden über Fahrräder, andere
-- Benutzer oder Bewertungen, die von Admins bearbeitet und gelöst werden.
--
-- Unlike audit_log (immutable, append-only), reports are mutable: an admin
-- moves a report through PENDING -> UNDER_REVIEW -> RESOLVED/DISMISSED, so
-- the row gets updated_at bumped and resolution fields filled in over time.
-- target_type/target_id is a polymorphic reference (no FK) since it can
-- point at bikes, users, or reviews — same pattern as audit_log's
-- target_type/target_id.
--
-- Im Gegensatz zu audit_log (unveränderlich, nur anfügend) sind Meldungen
-- veränderlich: ein Admin bewegt eine Meldung durch PENDING -> UNDER_REVIEW
-- -> RESOLVED/DISMISSED, wodurch updated_at und die Auflösungsfelder im
-- Laufe der Zeit aktualisiert werden. target_type/target_id ist eine
-- polymorphe Referenz (kein FK), da sie auf Fahrräder, Benutzer oder
-- Bewertungen verweisen kann — gleiches Muster wie target_type/target_id
-- in audit_log.
CREATE TABLE reports (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id        UUID NOT NULL,
    reporter_name      VARCHAR(200) NOT NULL,
    target_type        VARCHAR(20) NOT NULL,
    target_id          UUID NOT NULL,
    reason             VARCHAR(40) NOT NULL,
    details            TEXT,
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolution_note    TEXT,
    resolved_by        UUID,
    resolved_by_name   VARCHAR(200),
    resolved_at        TIMESTAMP,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at         TIMESTAMP
);

CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_created_at ON reports(created_at DESC);
CREATE INDEX idx_reports_target ON reports(target_type, target_id);
CREATE INDEX idx_reports_reporter_id ON reports(reporter_id);
