-- ============================================================
-- V4: Add missing columns to bikes table
-- V4: Fehlende Spalten zur bikes-Tabelle hinzufügen
--
-- V1 schema did not include latitude/longitude for bikes.
-- V1-Schema hatte keine latitude/longitude für Fahrräder.
-- ============================================================

-- GPS coordinates — optional, used for map view
-- GPS-Koordinaten — optional, für Kartenansicht
ALTER TABLE bikes
    ADD COLUMN IF NOT EXISTS latitude  DECIMAL(9, 6);

ALTER TABLE bikes
    ADD COLUMN IF NOT EXISTS longitude DECIMAL(9, 6);
