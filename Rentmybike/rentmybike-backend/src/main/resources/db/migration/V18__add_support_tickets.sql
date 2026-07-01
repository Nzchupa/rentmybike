-- Support tickets: a user-facing help desk so questions/problems go through
-- a tracked thread instead of only the single-developer contact-page email.
-- Support-Tickets: ein nutzerseitiges Support-System, damit Fragen/Probleme
-- über einen nachverfolgbaren Thread laufen statt nur über die
-- Kontaktseiten-E-Mail an den einzelnen Entwickler.
--
-- Two tables: support_tickets (the ticket header — one per issue) and
-- support_messages (the back-and-forth thread on a ticket, one row per
-- message from either the filing user or an admin). This mirrors the
-- reports table's polymorphic/mutable style (status moves through a small
-- state machine, updated_at bumped over time) but adds a real message
-- thread since support conversations are back-and-forth, unlike a report's
-- single resolution note.
--
-- Zwei Tabellen: support_tickets (der Ticket-Kopf — einer pro Anliegen) und
-- support_messages (der Nachrichtenverlauf zu einem Ticket, eine Zeile pro
-- Nachricht vom einreichenden Benutzer oder einem Admin). Folgt dem
-- veränderlichen Stil der reports-Tabelle (Status durchläuft eine kleine
-- Zustandsmaschine, updated_at wird laufend aktualisiert), fügt aber einen
-- echten Nachrichtenverlauf hinzu, da Support-Gespräche hin- und hergehen,
-- anders als die einzelne Auflösungsnotiz eines Reports.
CREATE TABLE support_tickets (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL,
    user_name          VARCHAR(200) NOT NULL,
    user_email         VARCHAR(255) NOT NULL,
    subject            VARCHAR(200) NOT NULL,
    category           VARCHAR(30) NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at         TIMESTAMP
);

CREATE INDEX idx_support_tickets_status ON support_tickets(status);
CREATE INDEX idx_support_tickets_user_id ON support_tickets(user_id);
CREATE INDEX idx_support_tickets_created_at ON support_tickets(created_at DESC);

CREATE TABLE support_messages (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id          UUID NOT NULL REFERENCES support_tickets(id),
    sender_id          UUID NOT NULL,
    sender_name        VARCHAR(200) NOT NULL,
    from_admin         BOOLEAN NOT NULL DEFAULT FALSE,
    body               TEXT NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at         TIMESTAMP
);

CREATE INDEX idx_support_messages_ticket_id ON support_messages(ticket_id);
CREATE INDEX idx_support_messages_created_at ON support_messages(created_at);
