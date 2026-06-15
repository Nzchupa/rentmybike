-- ============================================================
-- V1: RentMyBike — Initial Database Schema
-- V1: RentMyBike — Initiales Datenbankschema
--
-- Tables / Tabellen:
--   users, bikes, bike_photos, bookings, reviews
--
-- ENUMs / Aufzählungen:
--   user_role, bike_category, booking_status, review_type
--
-- All IDs are UUID (gen_random_uuid()) — prevents enumeration attacks.
-- Alle IDs sind UUID (gen_random_uuid()) — verhindert Enumerierungsangriffe.
--
-- Soft delete via deleted_at column on users, bikes, bookings.
-- Soft-Delete über deleted_at-Spalte für users, bikes, bookings.
-- ============================================================


-- ── ENUMs ────────────────────────────────────────────────────────────────────
-- Create ENUMs before tables that use them
-- ENUMs vor den Tabellen erstellen, die sie verwenden

CREATE TYPE user_role AS ENUM (
    'USER',     -- Regular user (owner + renter) / Regulärer Benutzer (Eigentümer + Mieter)
    'ADMIN'     -- Platform administrator / Plattform-Administrator
);

CREATE TYPE bike_category AS ENUM (
    'CITY',       -- City / comfort bikes / Stadt- / Komfortfahrräder
    'MOUNTAIN',   -- Mountain bikes / Mountainbikes
    'ROAD',       -- Road / racing bikes / Rennräder
    'ELECTRIC',   -- E-bikes / E-Bikes
    'CARGO',      -- Cargo bikes / Lastenräder
    'KIDS'        -- Kids bikes / Kinderfahrräder
);

CREATE TYPE booking_status AS ENUM (
    'PENDING',    -- Waiting for owner approval / Wartet auf Eigentümergenehmigung
    'ACCEPTED',   -- Owner approved / Vom Eigentümer genehmigt
    'REJECTED',   -- Owner rejected / Vom Eigentümer abgelehnt
    'CANCELLED',  -- Cancelled by renter or owner / Von Mieter oder Eigentümer storniert
    'COMPLETED'   -- Rental period ended successfully / Mietzeit erfolgreich beendet
);

CREATE TYPE review_type AS ENUM (
    'RENTER_TO_OWNER',  -- Renter reviews the bike owner / Mieter bewertet den Fahrrad-Eigentümer
    'OWNER_TO_RENTER'   -- Owner reviews the renter / Eigentümer bewertet den Mieter
);


-- ── USERS ────────────────────────────────────────────────────────────────────
-- One account = both owner and renter simultaneously
-- Ein Konto = sowohl Eigentümer als auch Mieter gleichzeitig

CREATE TABLE users (
    id                              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Authentication / Authentifizierung
    email                           VARCHAR(255)    NOT NULL UNIQUE,
    password                        VARCHAR(255)    NOT NULL,

    -- Profile / Profil
    first_name                      VARCHAR(100)    NOT NULL,
    last_name                       VARCHAR(100)    NOT NULL,
    phone                           VARCHAR(30),
    avatar_url                      VARCHAR(500),

    -- Role & status / Rolle und Status
    role                            user_role       NOT NULL DEFAULT 'USER',
    email_verified                  BOOLEAN         NOT NULL DEFAULT FALSE,
    email_verification_token        VARCHAR(100),
    email_verification_token_expiry TIMESTAMP,
    banned_at                       TIMESTAMP,      -- NULL = active / NULL = aktiv

    -- Audit / Prüfung
    deleted_at                      TIMESTAMP,      -- Soft delete / Soft-Delete
    created_at                      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Fast login lookups / Schnelle Anmelde-Lookups
CREATE INDEX idx_users_email
    ON users (email);

-- Fast verification token lookup / Schnelle Verifizierungstoken-Lookups
CREATE INDEX idx_users_email_verification_token
    ON users (email_verification_token)
    WHERE email_verification_token IS NOT NULL;

-- Filter out soft-deleted users efficiently / Soft-gelöschte Benutzer effizient filtern
CREATE INDEX idx_users_deleted_at
    ON users (deleted_at)
    WHERE deleted_at IS NULL;


-- ── BIKES ────────────────────────────────────────────────────────────────────
-- Bike listings created by owners
-- Fahrrad-Inserate, die von Eigentümern erstellt wurden

CREATE TABLE bikes (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Ownership / Eigentümerschaft
    owner_id        UUID            NOT NULL REFERENCES users(id),

    -- Listing details / Inserat-Details
    title           VARCHAR(255)    NOT NULL,
    description     TEXT,
    category        bike_category   NOT NULL,
    price_per_day   DECIMAL(10, 2)  NOT NULL CHECK (price_per_day > 0),

    -- Location / Standort
    city            VARCHAR(100)    NOT NULL,
    address         VARCHAR(500),

    -- Status / Status
    is_available    BOOLEAN         NOT NULL DEFAULT TRUE,
    is_approved     BOOLEAN         NOT NULL DEFAULT FALSE,  -- Admin must approve / Admin muss genehmigen

    -- Audit / Prüfung
    deleted_at      TIMESTAMP,      -- Soft delete / Soft-Delete
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Search by owner / Nach Eigentümer suchen
CREATE INDEX idx_bikes_owner_id
    ON bikes (owner_id);

-- Public listing query (available + approved + not deleted)
-- Öffentliche Inserate-Abfrage (verfügbar + genehmigt + nicht gelöscht)
CREATE INDEX idx_bikes_listing
    ON bikes (is_available, is_approved, deleted_at)
    WHERE deleted_at IS NULL;

-- Category filter / Kategorie-Filter
CREATE INDEX idx_bikes_category
    ON bikes (category)
    WHERE deleted_at IS NULL;

-- City filter / Stadt-Filter
CREATE INDEX idx_bikes_city
    ON bikes (city)
    WHERE deleted_at IS NULL;


-- ── BIKE PHOTOS ──────────────────────────────────────────────────────────────
-- Up to 5 photos per bike, stored as Cloudinary URLs
-- Bis zu 5 Fotos pro Fahrrad, als Cloudinary-URLs gespeichert

CREATE TABLE bike_photos (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    bike_id         UUID        NOT NULL REFERENCES bikes(id) ON DELETE CASCADE,

    -- Photo data / Fotodaten
    url             VARCHAR(500) NOT NULL,
    is_primary      BOOLEAN     NOT NULL DEFAULT FALSE,
    display_order   INT         NOT NULL DEFAULT 0,

    -- Audit / Prüfung
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Lookup photos by bike / Fotos nach Fahrrad nachschlagen
CREATE INDEX idx_bike_photos_bike_id
    ON bike_photos (bike_id);

-- Ensure only one primary photo per bike / Stellt sicher, dass nur ein Hauptfoto pro Fahrrad existiert
CREATE UNIQUE INDEX idx_bike_photos_primary
    ON bike_photos (bike_id)
    WHERE is_primary = TRUE;


-- ── BOOKINGS ─────────────────────────────────────────────────────────────────
-- Rental requests and their lifecycle
-- Mietanfragen und ihr Lebenszyklus

CREATE TABLE bookings (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Participants / Teilnehmer
    bike_id         UUID            NOT NULL REFERENCES bikes(id),
    renter_id       UUID            NOT NULL REFERENCES users(id),
    owner_id        UUID            NOT NULL REFERENCES users(id),

    -- Rental period / Mietzeit
    start_date      DATE            NOT NULL,
    end_date        DATE            NOT NULL,

    -- Pricing / Preisgestaltung
    total_price     DECIMAL(10, 2)  NOT NULL CHECK (total_price > 0),

    -- Status / Status
    status          booking_status  NOT NULL DEFAULT 'PENDING',

    -- Optional renter message to owner / Optionale Mieternachricht an Eigentümer
    message         TEXT,

    -- Audit / Prüfung
    deleted_at      TIMESTAMP,      -- Soft delete / Soft-Delete
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    -- ── Business rule constraints / Geschäftsregel-Einschränkungen ──────────
    -- Owner cannot rent their own bike / Eigentümer kann nicht sein eigenes Fahrrad mieten
    CONSTRAINT no_self_booking
        CHECK (renter_id != owner_id),

    -- End date must be >= start date / Enddatum muss >= Startdatum sein
    CONSTRAINT valid_dates
        CHECK (end_date >= start_date)
);

-- Renter's booking history / Buchungshistorie des Mieters
CREATE INDEX idx_bookings_renter_id
    ON bookings (renter_id);

-- Owner's incoming requests / Eingehende Anfragen des Eigentümers
CREATE INDEX idx_bookings_owner_id
    ON bookings (owner_id);

-- Bike booking history / Fahrrads Buchungshistorie
CREATE INDEX idx_bookings_bike_id
    ON bookings (bike_id);

-- ── Date conflict check index (CRITICAL for performance) ──────────────────
-- ── Datumskonflikt-Prüfindex (KRITISCH für Performance) ─────────────────
-- Used by the date availability check query in BookingService:
-- Wird von der Verfügbarkeitsprüfabfrage in BookingService verwendet:
--   SELECT COUNT(*) FROM bookings
--   WHERE bike_id = :bikeId
--     AND status IN ('PENDING', 'ACCEPTED')
--     AND start_date <= :endDate
--     AND end_date >= :startDate;
CREATE INDEX idx_bookings_date_conflict
    ON bookings (bike_id, status, start_date, end_date)
    WHERE deleted_at IS NULL;


-- ── REVIEWS ──────────────────────────────────────────────────────────────────
-- Bidirectional reviews: renter reviews owner AND owner reviews renter
-- Bidirektionale Bewertungen: Mieter bewertet Eigentümer UND Eigentümer bewertet Mieter
-- Both reviews are optional; triggered after booking COMPLETED
-- Beide Bewertungen sind optional; werden nach COMPLETED-Buchung ausgelöst

CREATE TABLE reviews (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Context / Kontext
    booking_id      UUID        NOT NULL REFERENCES bookings(id),
    reviewer_id     UUID        NOT NULL REFERENCES users(id),
    reviewee_id     UUID        NOT NULL REFERENCES users(id),

    -- Review content / Bewertungsinhalt
    rating          INT         NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment         TEXT,
    type            review_type NOT NULL,

    -- Audit / Prüfung
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW(),

    -- ── Constraints / Einschränkungen ────────────────────────────────────
    -- One review per booking per direction / Eine Bewertung pro Buchung pro Richtung
    CONSTRAINT unique_review_per_booking_type
        UNIQUE (booking_id, type)
);

-- Look up reviews written about a user (for profile ratings)
-- Bewertungen über einen Benutzer nachschlagen (für Profilbewertungen)
CREATE INDEX idx_reviews_reviewee_id
    ON reviews (reviewee_id);

-- Look up reviews for a bike (via booking join)
-- Bewertungen für ein Fahrrad nachschlagen (über Buchungs-Join)
CREATE INDEX idx_reviews_booking_id
    ON reviews (booking_id);


-- ══════════════════════════════════════════════════════════════════════════════
-- Summary / Zusammenfassung:
--
-- Tables:  users, bikes, bike_photos, bookings, reviews
-- ENUMs:   user_role, bike_category, booking_status, review_type
-- Indexes: 13 performance indexes + 2 unique constraints
-- Rules:   no_self_booking, valid_dates, rating 1-5, unique review per booking+type
-- ══════════════════════════════════════════════════════════════════════════════
