package com.rentmybike.booking.entity;

import com.rentmybike.bike.entity.Bike;
import com.rentmybike.common.entity.BaseEntity;
import com.rentmybike.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A rental booking — the central transaction entity of the RentMyBike marketplace.
 * Eine Mietbuchung — die zentrale Transaktions-Entity des RentMyBike-Marktplatzes.
 *
 * <p>Lifecycle / Lebenszyklus:
 * <ol>
 *   <li>Renter creates booking → PENDING</li>
 *   <li>Owner accepts → ACCEPTED (bike reserved for those dates)</li>
 *   <li>OR owner rejects → REJECTED</li>
 *   <li>OR renter cancels before/after acceptance → CANCELLED</li>
 *   <li>After end_date passes → admin/scheduler marks COMPLETED</li>
 *   <li>COMPLETED → both parties can leave reviews</li>
 * </ol>
 *
 * <p>Date conflict prevention: the DB index {@code idx_bookings_date_conflict} on
 * (bike_id, status, start_date, end_date) makes overlap queries fast.
 * <p>Datumskonflikt-Verhinderung: Der DB-Index {@code idx_bookings_date_conflict} auf
 * (bike_id, status, start_date, end_date) macht Überlapp-Abfragen schnell.
 *
 * <p>Maps to PostgreSQL table {@code bookings}.
 * <p>Entspricht der PostgreSQL-Tabelle {@code bookings}.
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    // ──────────────────────────────────────────────────────────────────────────
    // Participants / Teilnehmer
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * The bike being rented.
     * Das gemietete Fahrrad.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bike_id", nullable = false)
    private Bike bike;

    /**
     * The user renting the bike.
     * Der Benutzer, der das Fahrrad mietet.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "renter_id", nullable = false)
    private User renter;

    /**
     * The bike owner (denormalized for fast query — matches bike.owner).
     * Der Fahrrad-Eigentümer (denormalisiert für schnelle Abfragen — entspricht bike.owner).
     *
     * <p>Stored directly so we can query "all bookings where I am the owner"
     * without joining through the bikes table.
     * <p>Direkt gespeichert, damit wir "alle Buchungen wo ich Eigentümer bin"
     * abfragen können ohne über die bikes-Tabelle zu joinen.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // ──────────────────────────────────────────────────────────────────────────
    // Rental period / Mietzeit
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Start date of the rental (inclusive).
     * Startdatum der Miete (inklusiv).
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * End date of the rental (inclusive).
     * Enddatum der Miete (inklusiv).
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // ──────────────────────────────────────────────────────────────────────────
    // Pricing / Preisgestaltung
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Total price = pricePerDay × number of days, locked at booking time.
     * Gesamtpreis = Preis pro Tag × Anzahl Tage, gesperrt zum Buchungszeitpunkt.
     *
     * <p>Locked at booking creation so price changes don't affect existing bookings.
     * <p>Wird bei der Buchungserstellung gesperrt, damit Preisänderungen keine
     * bestehenden Buchungen beeinflussen.
     */
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    // ──────────────────────────────────────────────────────────────────────────
    // Status / Status
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Current booking status — see {@link BookingStatus} for the state machine.
     * Aktueller Buchungsstatus — siehe {@link BookingStatus} für die Zustandsmaschine.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    // ──────────────────────────────────────────────────────────────────────────
    // Communication / Kommunikation
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Optional message from renter to owner when submitting the booking request.
     * Optionale Nachricht des Mieters an den Eigentümer bei der Buchungsanfrage.
     */
    @Column(columnDefinition = "TEXT")
    private String message;

    // ──────────────────────────────────────────────────────────────────────────
    // Business logic helpers / Geschäftslogik-Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Calculates the number of rental days (end - start + 1, minimum 1).
     * Berechnet die Anzahl der Miettage (Ende - Start + 1, Minimum 1).
     */
    public long getRentalDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * Returns true if this booking can still be cancelled by the renter.
     * Gibt true zurück, wenn diese Buchung noch vom Mieter storniert werden kann.
     *
     * <p>PENDING and ACCEPTED bookings can be cancelled; COMPLETED/REJECTED/CANCELLED cannot.
     */
    public boolean isCancellable() {
        return status == BookingStatus.PENDING || status == BookingStatus.ACCEPTED;
    }
}
