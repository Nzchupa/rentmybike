-- Adds the audit_log table — an immutable, append-only record of admin and
-- moderation actions (bans, suspensions, promotions, bike approvals/rejections,
-- business verification, etc.) plus a couple of user-triggered events
-- (registration, booking cancellation) worth keeping a trail of.
--
-- Unlike notifications, this table has no updated_at/deleted_at — rows are
-- never modified or removed once written. actor_name is a denormalized
-- snapshot of the acting user's display name at write time, so the log stays
-- readable even after the actor's account is later soft-deleted or renamed.
-- actor_id is nullable to allow for future system-triggered events that have
-- no human actor.
--
-- Fügt die audit_log-Tabelle hinzu — ein unveränderliches, nur anfügbares
-- Protokoll von Admin- und Moderationsaktionen (Sperrungen, Suspendierungen,
-- Befoerderungen, Fahrrad-Genehmigungen/-Ablehnungen, Geschäftsverifizierung
-- usw.) sowie einiger benutzerausgelöster Ereignisse (Registrierung,
-- Buchungsstornierung), die es wert sind, nachverfolgt zu werden.
--
-- Anders als bei notifications gibt es hier kein updated_at/deleted_at —
-- Zeilen werden nach dem Schreiben nie verändert oder entfernt. actor_name ist
-- eine denormalisierte Momentaufnahme des Anzeigenamens des Akteurs zum
-- Schreibzeitpunkt, damit das Protokoll auch dann lesbar bleibt, wenn das
-- Konto des Akteurs später soft-gelöscht oder umbenannt wird. actor_id ist
-- nullable, um künftige systemausgelöste Ereignisse ohne menschlichen Akteur
-- zu ermöglichen.

CREATE TABLE audit_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID,                  -- nullable: null = system event / null = Systemereignis
    actor_name  VARCHAR(200),
    action      VARCHAR(50) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id   UUID,
    details     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Newest-first listing is the default admin view — backs ORDER BY created_at DESC.
-- Neueste-zuerst-Auflistung ist die Standard-Admin-Ansicht — unterstützt ORDER BY created_at DESC.
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at DESC);

-- Exact-match filter by action type (e.g. "show only USER_BANNED events").
-- Exakter Filter nach Aktionstyp (z.B. "nur USER_BANNED-Ereignisse anzeigen").
CREATE INDEX idx_audit_log_action ON audit_log(action);
