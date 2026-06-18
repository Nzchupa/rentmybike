-- Adds the booking_photos table for before/after rental condition photos.
-- Fügt die booking_photos-Tabelle für Vorher/Nachher-Mietzustandsfotos hinzu.
--
-- Either the renter or the owner of a specific booking can attach photos
-- documenting the bike's condition at pickup (BEFORE) and return (AFTER) —
-- protects both parties in case of damage disputes. Photos are tied to one
-- booking only (not the bike listing itself), since condition is specific
-- to that rental period.
--
-- Entweder der Mieter oder der Eigentümer einer bestimmten Buchung kann
-- Fotos anhängen, die den Zustand des Fahrrads bei Abholung (BEFORE) und
-- Rückgabe (AFTER) dokumentieren — schützt beide Parteien bei
-- Schadensstreitigkeiten. Fotos sind nur an eine Buchung gebunden (nicht an
-- das Fahrrad-Inserat selbst), da der Zustand spezifisch für diesen
-- Mietzeitraum ist.

CREATE TABLE booking_photos (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id  UUID NOT NULL REFERENCES bookings(id),
    uploader_id UUID NOT NULL REFERENCES users(id),
    phase       VARCHAR(10) NOT NULL,
    photo_url   VARCHAR(500) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);

-- Fast "list photos for this booking" query, the only access pattern needed.
-- Schnelle Abfrage "Fotos für diese Buchung auflisten", das einzig benötigte Zugriffsmuster.
CREATE INDEX idx_booking_photos_booking_id ON booking_photos(booking_id);
