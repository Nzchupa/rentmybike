package com.rentmybike.contract.entity;

import com.rentmybike.booking.entity.Booking;
import com.rentmybike.booking.entity.PaymentMethod;
import com.rentmybike.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A frozen, per-booking rental contract snapshot — created automatically the
 * moment the owner accepts a booking (see
 * {@code BookingService.acceptBooking}).
 * Eine eingefrorene, buchungsbezogene Mietvertrags-Momentaufnahme — wird
 * automatisch in dem Moment erstellt, in dem der Eigentümer eine Buchung
 * akzeptiert (siehe {@code BookingService.acceptBooking}).
 *
 * <p>Every party/bike/price field here is a denormalized snapshot, same
 * rationale as {@code SupportTicket.userName}/{@code Report.reporterName}:
 * the contract text must stay legally stable and fully readable exactly as
 * it was at acceptance time, even if the bike is later edited/deleted or a
 * user later changes their name — none of that may retroactively alter what
 * either party agreed to.
 * <p>Jedes Partei-/Fahrrad-/Preisfeld hier ist eine denormalisierte
 * Momentaufnahme, gleiche Begründung wie bei {@code SupportTicket.userName}/
 * {@code Report.reporterName}: Der Vertragstext muss rechtlich stabil und
 * exakt so lesbar bleiben, wie er zum Zeitpunkt der Annahme war, selbst wenn
 * das Fahrrad später bearbeitet/gelöscht wird oder ein Benutzer später
 * seinen Namen ändert — nichts davon darf rückwirkend ändern, worauf sich
 * beide Parteien geeinigt haben.
 *
 * <p>Both parties must individually click-to-accept (see
 * {@code owner/renterAcceptedAt/Ip}) — the timestamp + IP address pair is the
 * closest a form-free, in-app agreement can get to a signature, and is what
 * would actually be shown to police/a court alongside the frozen text if a
 * dispute over a missing bike ever escalated that far. This is not legal
 * advice; it improves the contract's evidentiary weight but a German court's
 * assessment always depends on the concrete circumstances.
 * <p>Beide Parteien müssen einzeln per Klick zustimmen (siehe
 * {@code owner/renterAcceptedAt/Ip}) — das Paar aus Zeitstempel und
 * IP-Adresse kommt einer Unterschrift bei einer formfreien In-App-Vereinbarung
 * am nächsten und ist das, was bei einer Polizei-/Gerichtsvorlage neben dem
 * eingefrorenen Text tatsächlich gezeigt würde, falls ein Streit um ein nicht
 * zurückgegebenes Fahrrad jemals so weit eskalieren sollte. Dies ist keine
 * Rechtsberatung; es verbessert den Beweiswert des Vertrags, aber die
 * Einschätzung eines deutschen Gerichts hängt immer von den konkreten
 * Umständen ab.
 *
 * <p>Maps to PostgreSQL table {@code rental_contracts}.
 */
@Entity
@Table(name = "rental_contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalContract extends BaseEntity {

    /** 1:1 with the booking this contract belongs to / 1:1 mit der zugehörigen Buchung */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true, updatable = false)
    private Booking booking;

    // ── Parties (§1) / Parteien (§1) ────────────────────────────────────────
    @Column(name = "owner_name", nullable = false, updatable = false, length = 200)
    private String ownerName;

    @Column(name = "owner_email", nullable = false, updatable = false, length = 255)
    private String ownerEmail;

    @Column(name = "renter_name", nullable = false, updatable = false, length = 200)
    private String renterName;

    @Column(name = "renter_email", nullable = false, updatable = false, length = 255)
    private String renterEmail;

    // ── Rental object (§2) / Mietgegenstand (§2) ────────────────────────────
    @Column(name = "bike_title", nullable = false, updatable = false, length = 100)
    private String bikeTitle;

    @Column(name = "bike_model", updatable = false, length = 150)
    private String bikeModel;

    @Column(name = "bike_category", nullable = false, updatable = false, length = 50)
    private String bikeCategory;

    @Column(name = "bike_city", nullable = false, updatable = false, length = 100)
    private String bikeCity;

    @Column(name = "bike_address", updatable = false, length = 255)
    private String bikeAddress;

    // ── Rental period (§3) / Mietzeitraum (§3) ──────────────────────────────
    @Column(name = "start_date", nullable = false, updatable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false, updatable = false)
    private LocalDate endDate;

    @Column(name = "rental_days", nullable = false, updatable = false)
    private int rentalDays;

    // ── Price & payment (§4) / Preis & Zahlung (§4) ─────────────────────────
    @Column(name = "price_per_day", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal pricePerDay;

    @Column(name = "total_price", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, updatable = false, columnDefinition = "VARCHAR(20)")
    private PaymentMethod paymentMethod;

    /** Null unless a deposit is ever configured — §4's deposit line is omitted when null. */
    @Column(name = "deposit_amount", updatable = false, precision = 10, scale = 2)
    private BigDecimal depositAmount;

    // ── Two-sided click-to-accept (§12) / Zweiseitige Klick-Zustimmung (§12) ─
    @Column(name = "owner_accepted_at")
    private LocalDateTime ownerAcceptedAt;

    @Column(name = "owner_accepted_ip", length = 64)
    private String ownerAcceptedIp;

    @Column(name = "renter_accepted_at")
    private LocalDateTime renterAcceptedAt;

    @Column(name = "renter_accepted_ip", length = 64)
    private String renterAcceptedIp;

    /**
     * True once both parties have clicked to accept.
     * True, sobald beide Parteien per Klick zugestimmt haben.
     */
    public boolean isFullyAccepted() {
        return ownerAcceptedAt != null && renterAcceptedAt != null;
    }
}
