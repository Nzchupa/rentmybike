package com.rentmybike.booking.entity;

/**
 * Lifecycle states of a bike rental booking.
 * Lebenszyklus-Zustände einer Fahrrad-Mietbuchung.
 *
 * <p>State machine / Zustandsmaschine:
 * <pre>
 *  PENDING ──► ACCEPTED ──► COMPLETED
 *     │            │
 *     ▼            ▼
 *  REJECTED    CANCELLED
 *     │
 *     ▼
 *  CANCELLED  (by renter before owner responds)
 * </pre>
 *
 * <p>Maps to PostgreSQL ENUM type {@code booking_status} created in V1 migration.
 * <p>Entspricht dem PostgreSQL-ENUM-Typ {@code booking_status} aus der V1-Migration.
 */
public enum BookingStatus {

    /**
     * Booking request submitted — waiting for owner to accept or reject.
     * Buchungsanfrage eingereicht — wartet auf Akzeptanz oder Ablehnung des Eigentümers.
     *
     * <p>Pending bookings older than 48h are lazily expired on next access.
     * <p>Ausstehende Buchungen älter als 48h werden beim nächsten Zugriff lazy abgelaufen.
     */
    PENDING,

    /**
     * Owner accepted — bike is reserved for this renter on the specified dates.
     * Eigentümer akzeptiert — Fahrrad ist für diesen Mieter an den angegebenen Terminen reserviert.
     */
    ACCEPTED,

    /**
     * Owner rejected the request.
     * Eigentümer hat die Anfrage abgelehnt.
     */
    REJECTED,

    /**
     * Cancelled by the renter (before or after owner acceptance) or by the owner.
     * Storniert vom Mieter (vor oder nach Eigentümerakzeptanz) oder vom Eigentümer.
     */
    CANCELLED,

    /**
     * Rental period ended successfully — both parties can now leave reviews.
     * Mietzeit erfolgreich beendet — beide Parteien können jetzt Bewertungen hinterlassen.
     *
     * <p>Only COMPLETED bookings unlock the review flow.
     * <p>Nur COMPLETED-Buchungen schalten den Bewertungsfluss frei.
     */
    COMPLETED
}
