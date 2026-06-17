-- Adds the notifications table for in-app notifications (Bug 5).
-- Fügt die notifications-Tabelle für In-App-Benachrichtigungen hinzu (Bug 5).
--
-- Previously, when a renter created a booking request, the bike owner had no
-- way to find out short of manually checking "As Owner" bookings — no email,
-- no in-app signal at all. This table backs a simple per-user notification
-- feed (e.g. "new rental request for your bike"), with a read/unread flag for
-- the bell-icon badge, and a nullable reference back to the booking that
-- triggered it so the frontend can deep-link straight to it.
--
-- Vorher hatte der Fahrrad-Eigentümer keine Möglichkeit zu erfahren, wenn ein
-- Mieter eine Buchungsanfrage erstellte, außer manuell unter "Als Eigentümer"
-- nachzusehen — keine E-Mail, kein In-App-Signal. Diese Tabelle unterstützt
-- einen einfachen Benachrichtigungs-Feed pro Benutzer (z.B. "neue Mietanfrage
-- für Ihr Fahrrad") mit einem Gelesen/Ungelesen-Flag für das Glocken-Icon-
-- Badge und einer nullable Referenz zurück auf die auslösende Buchung, damit
-- das Frontend direkt darauf verlinken kann.

CREATE TABLE notifications (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    booking_id  UUID REFERENCES bookings(id),
    type        VARCHAR(40) NOT NULL,
    title       VARCHAR(200) NOT NULL,
    message     TEXT NOT NULL,
    read_at     TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);

-- Fast "unread count for bell badge" and "list my notifications" queries.
-- Schnelle Abfragen für "Ungelesen-Zähler für Glocken-Badge" und "Meine Benachrichtigungen auflisten".
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, read_at);
