-- Adds the booking_accessories table — line items recording which
-- accessories (helmets, child seats, locks) a renter selected for a
-- specific booking, with the per-day price locked at booking time.
-- Fügt die booking_accessories-Tabelle hinzu — Positionszeilen, die
-- erfassen, welches Zubehör (Helme, Kindersitze, Schlösser) ein Mieter für
-- eine bestimmte Buchung ausgewählt hat, mit zum Buchungszeitpunkt
-- gesperrtem Tagespreis.
--
-- Stage 3 ("Business accounts"). One row per (booking, accessory) selection.
--
-- Stage 3 ("Business-Konten"). Eine Zeile pro (Buchung, Zubehör)-Auswahl.

CREATE TABLE booking_accessories (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id                  UUID NOT NULL REFERENCES bookings(id),
    accessory_id                UUID NOT NULL REFERENCES accessories(id),
    quantity                    INTEGER NOT NULL,
    price_per_day_at_booking    NUMERIC(10, 2) NOT NULL,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at                  TIMESTAMP
);

-- Fast "accessory line items for this booking" lookups (used to build BookingResponse).
-- Schnelle "Zubehör-Positionszeilen für diese Buchung"-Abfragen (wird zum Erstellen von BookingResponse verwendet).
CREATE INDEX idx_booking_accessories_booking_id ON booking_accessories(booking_id);
CREATE INDEX idx_booking_accessories_accessory_id ON booking_accessories(accessory_id);
