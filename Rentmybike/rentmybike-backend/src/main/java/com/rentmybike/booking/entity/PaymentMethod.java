package com.rentmybike.booking.entity;

/**
 * How the renter pays the owner for an accepted booking.
 * Wie der Mieter den Eigentümer für eine akzeptierte Buchung bezahlt.
 *
 * <p>Chosen by the owner at the moment they accept a PENDING booking (see
 * {@code BookingService.acceptBooking}) — the platform does not process any
 * of these payments itself; it only records which method both parties agreed
 * on so the rental contract (see {@code com.rentmybike.contract}) can name it.
 * <p>Wird vom Eigentümer im Moment der Annahme einer PENDING-Buchung gewählt
 * (siehe {@code BookingService.acceptBooking}) — die Plattform wickelt keine
 * dieser Zahlungen selbst ab; sie hält nur fest, auf welche Methode sich beide
 * Parteien geeinigt haben, damit der Mietvertrag (siehe {@code com.rentmybike.contract})
 * sie benennen kann.
 */
public enum PaymentMethod {

    /** Cash handed over in person / Barzahlung persönlich übergeben */
    CASH,

    /**
     * Owner sends a PayPal money request/invoice; renter pays and uploads a
     * receipt; owner confirms receipt of funds before handover.
     * Eigentümer sendet eine PayPal-Zahlungsanforderung/Rechnung; Mieter zahlt
     * und lädt eine Quittung hoch; Eigentümer bestätigt den Zahlungseingang
     * vor der Übergabe.
     */
    PAYPAL,

    /**
     * Paid by card at the owner's physical shop — only meaningful for
     * BUSINESS-role owners with a storefront.
     * Zahlung per Karte im Ladengeschäft des Eigentümers — nur sinnvoll für
     * Eigentümer mit der Rolle BUSINESS und einem Ladengeschäft.
     */
    CARD_ON_SITE
}
