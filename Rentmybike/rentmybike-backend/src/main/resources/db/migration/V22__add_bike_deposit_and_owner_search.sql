-- Optional refundable security deposit (Kaution), set by the owner when
-- listing the bike — frozen into the rental contract when a booking is
-- accepted. Null means no deposit required.
-- Optionale rückzahlbare Kaution, vom Eigentümer beim Inserieren des
-- Fahrrads festgelegt — wird bei Buchungsannahme in den Mietvertrag
-- eingefroren. Null bedeutet, dass keine Kaution erforderlich ist.
ALTER TABLE bikes ADD COLUMN deposit_amount NUMERIC(10,2);
