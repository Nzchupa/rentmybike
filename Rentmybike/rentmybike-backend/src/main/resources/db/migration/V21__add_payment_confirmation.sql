-- Manual PayPal payment confirmation trail — only populated for PAYPAL
-- bookings. CASH/CARD_ON_SITE bookings are settled in person and leave
-- these columns null.
-- Manuelle PayPal-Zahlungsbestätigungs-Historie — wird nur für
-- PAYPAL-Buchungen befüllt. CASH/CARD_ON_SITE-Buchungen werden persönlich
-- beglichen und lassen diese Spalten null.
ALTER TABLE bookings ADD COLUMN payment_status VARCHAR(20);
ALTER TABLE bookings ADD COLUMN payment_receipt_url VARCHAR(500);
ALTER TABLE bookings ADD COLUMN payment_receipt_submitted_at TIMESTAMP;
ALTER TABLE bookings ADD COLUMN payment_confirmed_at TIMESTAMP;
