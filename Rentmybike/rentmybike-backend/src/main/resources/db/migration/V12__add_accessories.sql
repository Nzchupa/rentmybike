-- Adds the accessories table for BUSINESS accounts to offer rentable
-- add-ons (helmets, child seats, locks) alongside their bikes.
-- Fügt die accessories-Tabelle hinzu, damit BUSINESS-Konten vermietbare
-- Zusatzartikel (Helme, Kindersitze, Schlösser) zu ihren Fahrrädern anbieten können.
--
-- Stage 3 ("Business accounts"). One row per accessory type/listing the
-- business stocks; quantity_total is the unit count owned (not tied to a
-- single bike). Soft-delete via deleted_at, same pattern as every other
-- entity (see BaseEntity).
--
-- Stage 3 ("Business-Konten"). Eine Zeile pro Zubehörtyp/-eintrag, den das
-- Unternehmen vorrätig hat; quantity_total ist die Stückzahl im Besitz
-- (nicht an ein einzelnes Fahrrad gebunden). Soft-Delete über deleted_at,
-- gleiches Muster wie bei jeder anderen Entität (siehe BaseEntity).

CREATE TABLE accessories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID NOT NULL REFERENCES users(id),
    type            VARCHAR(20) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    quantity_total  INTEGER NOT NULL,
    price_per_day   NUMERIC(10, 2) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP
);

-- Fast "accessories offered by this business" lookups (management list + public booking-time browse).
-- Schnelle "von diesem Unternehmen angebotenes Zubehör"-Abfragen (Verwaltungsliste + öffentliches Durchsuchen zur Buchungszeit).
CREATE INDEX idx_accessories_owner_id ON accessories(owner_id);
