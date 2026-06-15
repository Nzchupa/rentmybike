package com.rentmybike.booking.repository;

import com.rentmybike.booking.entity.Booking;
import com.rentmybike.booking.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for booking lifecycle queries.
 * Repository für Buchungslebenszyklus-Abfragen.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    // ──────────────────────────────────────────────────────────────────────────
    // Date conflict detection / Datumskonflikt-Erkennung
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Checks whether any PENDING or ACCEPTED booking overlaps with the requested dates
     * for a given bike. Used to prevent double-booking.
     *
     * Überprüft, ob eine PENDING oder ACCEPTED Buchung mit den angeforderten Terminen
     * für ein bestimmtes Fahrrad überschneidet. Zur Verhinderung von Doppelbuchungen.
     *
     * <p>Overlap condition (Allen's interval algebra):
     * <pre>
     *   existing.start <= requested.end
     *   AND existing.end >= requested.start
     * </pre>
     *
     * <p>Uses index {@code idx_bookings_date_conflict} — keep status filter on PENDING/ACCEPTED.
     * <p>Verwendet Index {@code idx_bookings_date_conflict} — Statusfilter auf PENDING/ACCEPTED beibehalten.
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.bike.id = :bikeId
              AND b.status IN (com.rentmybike.booking.entity.BookingStatus.PENDING, com.rentmybike.booking.entity.BookingStatus.ACCEPTED)
              AND b.deletedAt IS NULL
              AND b.startDate <= :endDate
              AND b.endDate   >= :startDate
            """)
    boolean existsDateConflict(
            @Param("bikeId")    UUID bikeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate
    );

    /**
     * Same as above but excludes a specific booking ID (for rescheduling).
     * Gleich wie oben, aber schließt eine bestimmte Buchungs-ID aus (für Umbuchungen).
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.bike.id = :bikeId
              AND b.id != :excludeId
              AND b.status IN (com.rentmybike.booking.entity.BookingStatus.PENDING, com.rentmybike.booking.entity.BookingStatus.ACCEPTED)
              AND b.deletedAt IS NULL
              AND b.startDate <= :endDate
              AND b.endDate   >= :startDate
            """)
    boolean existsDateConflictExcluding(
            @Param("bikeId")    UUID bikeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate,
            @Param("excludeId") UUID excludeId
    );

    // ──────────────────────────────────────────────────────────────────────────
    // Renter queries / Mieter-Abfragen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * All bookings made by a renter (their rental history).
     * Alle Buchungen eines Mieters (seine Miethistorie).
     */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.bike bike
            JOIN FETCH bike.owner
            LEFT JOIN FETCH bike.photos p
            WHERE b.renter.id = :renterId
              AND b.deletedAt IS NULL
            ORDER BY b.createdAt DESC
            """)
    Page<Booking> findByRenterIdOrderByCreatedAtDesc(
            @Param("renterId") UUID renterId,
            Pageable pageable
    );

    // ──────────────────────────────────────────────────────────────────────────
    // Owner queries / Eigentümer-Abfragen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * All incoming booking requests for a bike owner.
     * Alle eingehenden Buchungsanfragen für einen Fahrrad-Eigentümer.
     */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.bike bike
            JOIN FETCH b.renter
            LEFT JOIN FETCH bike.photos p
            WHERE b.owner.id = :ownerId
              AND b.deletedAt IS NULL
              AND (:status IS NULL OR b.status = :status)
            ORDER BY b.createdAt DESC
            """)
    Page<Booking> findByOwnerIdAndStatus(
            @Param("ownerId") UUID ownerId,
            @Param("status")  BookingStatus status,
            Pageable pageable
    );

    // ──────────────────────────────────────────────────────────────────────────
    // Single booking with full details / Einzelne Buchung mit vollständigen Details
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Load a booking with all associated entities (bike, photos, renter, owner).
     * Buchung mit allen verknüpften Entitäten laden (Fahrrad, Fotos, Mieter, Eigentümer).
     */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.bike bike
            JOIN FETCH bike.owner
            JOIN FETCH b.renter
            JOIN FETCH b.owner
            LEFT JOIN FETCH bike.photos
            WHERE b.id = :id
              AND b.deletedAt IS NULL
            """)
    Optional<Booking> findByIdWithDetails(@Param("id") UUID id);

    // ──────────────────────────────────────────────────────────────────────────
    // Lazy expiry (48h) / Lazy-Ablauf (48h)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Bulk-cancels PENDING bookings that were created more than 48 hours ago.
     * Storniert PENDING-Buchungen massenhaft, die vor mehr als 48 Stunden erstellt wurden.
     *
     * <p>Called lazily on each booking list/detail request — no scheduler needed in dev.
     * <p>Wird lazy bei jeder Buchungslisten-/Detailanfrage aufgerufen — kein Scheduler in dev nötig.
     */
    @Modifying
    @Query("""
            UPDATE Booking b SET b.status = com.rentmybike.booking.entity.BookingStatus.CANCELLED
            WHERE b.status = com.rentmybike.booking.entity.BookingStatus.PENDING
              AND b.createdAt < :expiryCutoff
              AND b.deletedAt IS NULL
            """)
    int expireStaleBookings(@Param("expiryCutoff") LocalDateTime expiryCutoff);

    // ──────────────────────────────────────────────────────────────────────────
    // Review eligibility / Bewertungsberechtigung
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Checks if a user (renter or owner) participated in a COMPLETED booking.
     * Überprüft, ob ein Benutzer (Mieter oder Eigentümer) an einer COMPLETED-Buchung teilgenommen hat.
     *
     * <p>Used by ReviewService to verify that a review is allowed.
     * <p>Wird von ReviewService verwendet, um zu überprüfen, ob eine Bewertung erlaubt ist.
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.id = :bookingId
              AND b.status = com.rentmybike.booking.entity.BookingStatus.COMPLETED
              AND (b.renter.id = :userId OR b.owner.id = :userId)
              AND b.deletedAt IS NULL
            """)
    boolean isUserParticipantOfCompletedBooking(
            @Param("bookingId") UUID bookingId,
            @Param("userId")    UUID userId
    );

    /**
     * All bookings for a specific bike (for admin / bike detail page).
     * Alle Buchungen für ein bestimmtes Fahrrad (für Admin / Fahrrad-Detailseite).
     */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.renter
            WHERE b.bike.id = :bikeId
              AND b.deletedAt IS NULL
            ORDER BY b.startDate DESC
            """)
    List<Booking> findByBikeIdOrderByStartDateDesc(@Param("bikeId") UUID bikeId);

    // ──────────────────────────────────────────────────────────────────────────
    // Admin stats counts / Admin-Statistikzählungen
    // ──────────────────────────────────────────────────────────────────────────

    /** Total non-deleted bookings / Gesamtanzahl nicht gelöschter Buchungen */
    long countByDeletedAtIsNull();

    /**
     * Non-deleted bookings filtered by status.
     * Nicht gelöschte Buchungen gefiltert nach Status.
     */
    long countByStatusAndDeletedAtIsNull(BookingStatus status);

    /**
     * Gross transaction volume — sum of totalPrice for all COMPLETED bookings.
     * Brutto-Transaktionsvolumen — Summe von totalPrice aller COMPLETED-Buchungen.
     *
     * <p>COALESCE returns 0.00 when no completed bookings exist yet.
     * <p>COALESCE gibt 0.00 zurück, wenn noch keine abgeschlossenen Buchungen existieren.
     */
    @Query("""
            SELECT COALESCE(SUM(b.totalPrice), 0)
            FROM Booking b
            WHERE b.status = com.rentmybike.booking.entity.BookingStatus.COMPLETED
              AND b.deletedAt IS NULL
            """)
    BigDecimal sumTotalPriceOfCompleted();
}
