-- Payment method chosen by the owner when accepting a booking.
-- Zahlungsmethode, die der Eigentümer bei der Annahme einer Buchung wählt.
ALTER TABLE bookings ADD COLUMN payment_method VARCHAR(20);

-- Rental contract — a frozen, per-booking snapshot of the legal rental
-- agreement text's variable data (parties, bike, dates, price, payment
-- method), plus the two-sided click-to-accept record (timestamp + IP per
-- role). One contract per booking, created automatically the moment the
-- owner accepts.
-- Mietvertrag — eine eingefrorene, buchungsbezogene Momentaufnahme der
-- variablen Daten des rechtlichen Mietvertragstexts (Parteien, Fahrrad,
-- Termine, Preis, Zahlungsmethode) sowie der zweiseitigen
-- Zustimmungs-Erfassung (Zeitstempel + IP pro Rolle). Ein Vertrag pro
-- Buchung, automatisch erstellt in dem Moment, in dem der Eigentümer
-- akzeptiert.
CREATE TABLE rental_contracts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id          UUID NOT NULL UNIQUE REFERENCES bookings(id),

    owner_name          VARCHAR(200) NOT NULL,
    owner_email         VARCHAR(255) NOT NULL,
    renter_name         VARCHAR(200) NOT NULL,
    renter_email        VARCHAR(255) NOT NULL,

    bike_title          VARCHAR(100) NOT NULL,
    bike_model          VARCHAR(150),
    bike_category       VARCHAR(50) NOT NULL,
    bike_city           VARCHAR(100) NOT NULL,
    bike_address        VARCHAR(255),

    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    rental_days         INT NOT NULL,

    price_per_day       NUMERIC(10,2) NOT NULL,
    total_price         NUMERIC(10,2) NOT NULL,
    payment_method      VARCHAR(20) NOT NULL,
    deposit_amount      NUMERIC(10,2),

    owner_accepted_at   TIMESTAMP,
    owner_accepted_ip   VARCHAR(64),
    renter_accepted_at  TIMESTAMP,
    renter_accepted_ip  VARCHAR(64),

    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMP
);

CREATE INDEX idx_rental_contracts_booking_id ON rental_contracts(booking_id);
