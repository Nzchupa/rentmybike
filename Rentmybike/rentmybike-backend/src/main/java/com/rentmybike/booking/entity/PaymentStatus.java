package com.rentmybike.booking.entity;

/**
 * Manual PayPal payment confirmation state — only meaningful when
 * {@code Booking.paymentMethod == PAYPAL}. CASH and CARD_ON_SITE are settled
 * in person, so they never enter this state machine (paymentStatus stays
 * null for them).
 * Manueller PayPal-Zahlungsbestätigungsstatus — nur relevant, wenn
 * {@code Booking.paymentMethod == PAYPAL} ist. CASH und CARD_ON_SITE werden
 * persönlich beglichen, daher durchlaufen sie diese Zustandsmaschine nie
 * (paymentStatus bleibt für sie null).
 *
 * <p>Flow / Ablauf: owner accepts with PAYPAL → AWAITING_TRANSFER → renter
 * sends money via PayPal outside the platform, uploads a receipt →
 * RECEIPT_SUBMITTED → owner reviews the receipt and confirms → CONFIRMED.
 * The platform never touches the money itself, only this status trail.
 * <p>Eigentümer akzeptiert mit PAYPAL → AWAITING_TRANSFER → Mieter überweist
 * das Geld außerhalb der Plattform per PayPal, lädt eine Quittung hoch →
 * RECEIPT_SUBMITTED → Eigentümer prüft die Quittung und bestätigt →
 * CONFIRMED. Die Plattform fasst das Geld selbst nie an, nur diese
 * Status-Historie.
 */
public enum PaymentStatus {

    /** Owner accepted with PAYPAL; waiting for the renter to pay and upload a receipt. */
    AWAITING_TRANSFER,

    /** Renter uploaded a receipt and marked the payment as sent; waiting for owner confirmation. */
    RECEIPT_SUBMITTED,

    /** Owner confirmed the payment was received. */
    CONFIRMED
}
