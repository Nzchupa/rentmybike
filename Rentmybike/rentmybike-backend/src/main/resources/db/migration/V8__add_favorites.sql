-- Adds the favorites table so users can bookmark bikes they're interested in.
-- Fügt die favorites-Tabelle hinzu, damit Benutzer Fahrräder merken können, die sie interessieren.
--
-- Stage 2 ("Beta launch") trust feature, alongside ratings/reviews. A simple
-- join table between users and bikes: one row per (user, bike) favorite,
-- enforced unique so a user can't favorite the same bike twice. No soft-delete
-- needed here — favoriting is a toggle, not a record worth keeping history of,
-- so removing a favorite just deletes the row.
--
-- Stage-2-Feature ("Beta-Start") für Vertrauen, neben Bewertungen/Rezensionen.
-- Eine einfache Verknüpfungstabelle zwischen Benutzern und Fahrrädern: eine
-- Zeile pro (Benutzer, Fahrrad)-Favorit, eindeutig erzwungen, damit ein
-- Benutzer dasselbe Fahrrad nicht zweimal favorisieren kann. Kein Soft-Delete
-- nötig — Favorisieren ist ein Umschalter, kein historienwürdiger Datensatz,
-- daher löscht das Entfernen eines Favoriten einfach die Zeile.

CREATE TABLE favorites (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    bike_id     UUID NOT NULL REFERENCES bikes(id),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP,
    CONSTRAINT uq_favorites_user_bike UNIQUE (user_id, bike_id)
);

-- Fast "is this bike favorited by me" checks and "list my favorites" queries.
-- Schnelle "Ist dieses Fahrrad von mir favorisiert"-Prüfungen und "Meine Favoriten auflisten"-Abfragen.
CREATE INDEX idx_favorites_user_id ON favorites(user_id);
CREATE INDEX idx_favorites_bike_id ON favorites(bike_id);
