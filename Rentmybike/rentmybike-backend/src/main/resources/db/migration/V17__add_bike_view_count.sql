-- ============================================================
-- V17: Bike view count
-- V17: Fahrrad-Aufrufzähler
--
-- Backs the bike detail page's "N views" badge and the owner/business
-- per-bike stats panel. Incremented (atomically, via UPDATE ... SET
-- view_count = view_count + 1) once per public detail-page fetch in
-- BikeService.getPublicBike — not deduplicated per visitor, since there is
-- no session/analytics infrastructure to dedupe against; this is a simple
-- popularity signal, not an exact unique-visitor count.
--
-- Unterstützt das "N Aufrufe"-Badge der Fahrrad-Detailseite und das
-- Pro-Fahrrad-Statistikpanel für Eigentümer/Unternehmen. Wird (atomisch,
-- über UPDATE ... SET view_count = view_count + 1) einmal pro öffentlichem
-- Detailseiten-Abruf in BikeService.getPublicBike erhöht — nicht pro
-- Besucher dedupliziert, da keine Sitzungs-/Analytics-Infrastruktur zur
-- Deduplizierung existiert; dies ist ein einfaches Popularitätssignal,
-- kein exakter Unique-Visitor-Zähler.
-- ============================================================

ALTER TABLE bikes
    ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;
