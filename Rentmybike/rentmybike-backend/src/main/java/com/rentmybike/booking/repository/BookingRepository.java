package com.rentmybike.booking.repository;

import com.rentmybike.booking.entity.Booking;
import com.rentmybike.booking.entity.BookingStatus;
import com.rentmybike.common.projection.DailyAmountProjection;
import com.rentmybike.common.projection.DailyCountProjection;
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

    /**
     * Checks whether a bike has any active (PENDING/ACCEPTED) bookings.
     * Used to block deletion of a bike that still has outstanding rentals.
     *
     * Überprüft, ob ein Fahrrad aktive (PENDING/ACCEPTED) Buchungen hat.
     * Wird verwendet, um die Löschung eines Fahrrads mit ausstehenden Mieten zu verhindern.
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.bike.id = :bikeId
              AND b.status IN (com.rentmybike.booking.entity.BookingStatus.PENDING, com.rentmybike.booking.entity.BookingStatus.ACCEPTED)
              AND b.deletedAt IS NULL
            """)
    boolean existsActiveBookingsForBike(@Param("bikeId") UUID bikeId);

    /**
     * All active (PENDING/ACCEPTED) bookings where the given user is either the
     * renter or the owner — used to cancel a user's outstanding bookings when
     * their account is deleted (admin cascade).
     *
     * Alle aktiven (PENDING/ACCEPTED) Buchungen, bei denen der gegebene Benutzer
     * entweder Mieter oder Eigentümer ist — wird verwendet, um die ausstehenden
     * Buchungen eines Benutzers bei Kontolöschung zu stornieren (Admin-Kaskade).
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE (b.renter.id = :userId OR b.owner.id = :userId)
              AND b.status IN (com.rentmybike.booking.entity.BookingStatus.PENDING, com.rentmybike.booking.entity.BookingStatus.ACCEPTED)
              AND b.deletedAt IS NULL
            """)
    List<Booking> findActiveBookingsByUserId(@Param("userId") UUID userId);

    // ──────────────────────────────────────────────────────────────────────────
    // Renter queries / Mieter-Abfragen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * All bookings made by a renter (their rental history).
     * Alle Buchungen eines Mieters (seine Miethistorie).
     *
     * <p>Same Hibernate pitfall as previously fixed in {@code BikeRepository}:
     * a {@code LEFT JOIN FETCH} on a {@code @OneToMany}/collection together
     * with {@code Pageable} makes Hibernate paginate in-memory (after loading
     * ALL matching rows) instead of in the database, because the generated
     * COUNT query can't reuse the fetch join. An explicit {@code countQuery}
     * without the collection fetch fixes the count, while the main query
     * keeps the fetch join for efficient photo loading.
     * <p>Dieselbe Hibernate-Falle wie zuvor in {@code BikeRepository}
     * behoben: ein {@code LEFT JOIN FETCH} auf eine
     * {@code @OneToMany}/Collection zusammen mit {@code Pageable} lässt
     * Hibernate im Speicher paginieren (nach Laden ALLER passenden Zeilen),
     * da die generierte COUNT-Abfrage den Fetch-Join nicht wiederverwenden
     * kann. Eine explizite {@code countQuery} ohne den Collection-Fetch
     * behebt die Zählung, während die Hauptabfrage den Fetch-Join für
     * effizientes Foto-Laden behält.
     */
    @Query(
            value = """
            SELECT b FROM Booking b
            JOIN FETCH b.bike bike
            JOIN FETCH bike.owner
            LEFT JOIN FETCH bike.photos p
            WHERE b.renter.id = :renterId
              AND b.deletedAt IS NULL
            ORDER BY b.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(b) FROM Booking b
            WHERE b.renter.id = :renterId
              AND b.deletedAt IS NULL
            """
    )
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
     *
     * <p>Same collection-fetch + Pageable pitfall as
     * {@link #findByRenterIdOrderByCreatedAtDesc} above — fixed the same way
     * with an explicit {@code countQuery}.
     * <p>Dieselbe Collection-Fetch + Pageable-Falle wie
     * {@link #findByRenterIdOrderByCreatedAtDesc} oben — auf die gleiche
     * Weise mit einer expliziten {@code countQuery} behoben.
     */
    @Query(
            value = """
            SELECT b FROM Booking b
            JOIN FETCH b.bike bike
            JOIN FETCH b.renter
            LEFT JOIN FETCH bike.photos p
            WHERE b.owner.id = :ownerId
              AND b.deletedAt IS NULL
              AND (:status IS NULL OR b.status = :status)
            ORDER BY b.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(b) FROM Booking b
            WHERE b.owner.id = :ownerId
              AND b.deletedAt IS NULL
              AND (:status IS NULL OR b.status = :status)
            """
    )
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

    /**
     * Non-deleted booking count for a specific bike, optionally filtered by
     * status — building block for the per-bike stats panel (owner/admin).
     * Anzahl nicht gelöschter Buchungen für ein bestimmtes Fahrrad, optional
     * nach Status gefiltert — Baustein für das Pro-Fahrrad-Statistikpanel
     * (Eigentümer/Admin).
     */
    long countByBikeIdAndDeletedAtIsNull(UUID bikeId);

    /** Same as above, filtered to a specific status / Wie oben, gefiltert nach einem bestimmten Status */
    long countByBikeIdAndStatusAndDeletedAtIsNull(UUID bikeId, BookingStatus status);

    /**
     * Gross revenue (sum of totalPrice) from COMPLETED bookings for a
     * specific bike — the per-bike counterpart to {@link
     * #sumTotalPriceOfCompleted}.
     * Bruttoumsatz (Summe von totalPrice) aus COMPLETED-Buchungen für ein
     * bestimmtes Fahrrad — das Pro-Fahrrad-Gegenstück zu {@link
     * #sumTotalPriceOfCompleted}.
     */
    @Query("""
            SELECT COALESCE(SUM(b.totalPrice), 0)
            FROM Booking b
            WHERE b.bike.id = :bikeId
              AND b.status = com.rentmybike.booking.entity.BookingStatus.COMPLETED
              AND b.deletedAt IS NULL
            """)
    BigDecimal sumTotalPriceOfCompletedByBikeId(@Param("bikeId") UUID bikeId);

    /**
     * Active (PENDING/ACCEPTED) bookings for a bike, used to expose occupied
     * date ranges on the public bike detail page so renters can see — and the
     * client-side calendar can disable — dates that are already taken.
     * Aktive (PENDING/ACCEPTED) Buchungen für ein Fahrrad, um belegte
     * Datumsbereiche auf der öffentlichen Fahrrad-Detailseite offenzulegen,
     * damit Mieter sehen — und der Client-seitige Kalender deaktivieren —
     * kann, welche Termine bereits vergeben sind.
     *
     * <p>Unlike {@link #findByBikeIdOrderByStartDateDesc}, this intentionally
     * does not fetch the renter — it's used behind a public endpoint and must
     * not leak who booked which dates.
     * <p>Im Gegensatz zu {@link #findByBikeIdOrderByStartDateDesc} wird hier
     * bewusst nicht der Mieter geladen — wird hinter einem öffentlichen
     * Endpunkt verwendet und darf nicht offenlegen, wer welche Termine
     * gebucht hat.
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.bike.id = :bikeId
              AND b.status IN (com.rentmybike.booking.entity.BookingStatus.PENDING, com.rentmybike.booking.entity.BookingStatus.ACCEPTED)
              AND b.deletedAt IS NULL
            ORDER BY b.startDate ASC
            """)
    List<Booking> findActiveBookingsByBikeId(@Param("bikeId") UUID bikeId);

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
     * Count of non-deleted bookings made by a user as renter — used by
     * AdminService to populate the per-user "bookingCount" figure in the
     * admin user list.
     * Anzahl nicht gelöschter Buchungen eines Benutzers als Mieter — wird von
     * AdminService verwendet, um die Kennzahl "bookingCount" pro Benutzer in
     * der Admin-Benutzerliste zu befüllen.
     */
    long countByRenterIdAndDeletedAtIsNull(@Param("renterId") UUID renterId);

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

    // ──────────────────────────────────────────────────────────────────────────
    // Admin analytics time-series / Admin-Analyse-Zeitreihe
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Daily new-booking counts since {@code from}, one row per day that had
     * at least one new booking (any status — this tracks demand/activity,
     * not just successful rentals). Native query, mirrors {@code
     * UserRepository.countDailySignupsSince}.
     * Tägliche Anzahl neuer Buchungen seit {@code from}, eine Zeile pro Tag
     * mit mindestens einer neuen Buchung (jeder Status — dies erfasst
     * Nachfrage/Aktivität, nicht nur erfolgreiche Vermietungen). Native
     * Abfrage, entspricht {@code UserRepository.countDailySignupsSince}.
     */
    @Query(value = """
            SELECT DATE_TRUNC('day', created_at)::date AS day, COUNT(*) AS count
            FROM bookings
            WHERE created_at >= :from
              AND deleted_at IS NULL
            GROUP BY day
            ORDER BY day
            """, nativeQuery = true)
    List<DailyCountProjection> countDailyBookingsSince(@Param("from") LocalDateTime from);

    /**
     * Daily revenue (sum of totalPrice for COMPLETED bookings) since {@code
     * from}, one row per day that had at least one completed booking.
     * Revenue is attributed to the day the booking was created, not
     * completed — completion date isn't tracked separately, and creation
     * date is what the rest of the admin dashboard's time-series already
     * buckets by.
     * Täglicher Umsatz (Summe von totalPrice für COMPLETED-Buchungen) seit
     * {@code from}, eine Zeile pro Tag mit mindestens einer abgeschlossenen
     * Buchung. Der Umsatz wird dem Erstellungstag der Buchung zugeordnet,
     * nicht dem Abschlussdatum — ein separates Abschlussdatum wird nicht
     * erfasst, und das Erstellungsdatum ist das, wonach die restliche
     * Zeitreihe des Admin-Dashboards bereits gruppiert.
     */
    @Query(value = """
            SELECT DATE_TRUNC('day', created_at)::date AS day, SUM(total_price) AS amount
            FROM bookings
            WHERE created_at >= :from
              AND deleted_at IS NULL
              AND status = 'COMPLETED'
            GROUP BY day
            ORDER BY day
            """, nativeQuery = true)
    List<DailyAmountProjection> sumDailyRevenueSince(@Param("from") LocalDateTime from);

    // ──────────────────────────────────────────────────────────────────────────
    // Business dashboard (Stage 3 "Business accounts") / Business-Dashboard
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Total non-deleted bookings received by a business (as bike owner).
     * Gesamtanzahl nicht gelöschter Buchungen, die ein Unternehmen (als
     * Fahrrad-Eigentümer) erhalten hat.
     */
    long countByOwnerIdAndDeletedAtIsNull(@Param("ownerId") UUID ownerId);

    /**
     * Total revenue earned by a business — sum of totalPrice for all
     * COMPLETED bookings where the business is the bike owner.
     * Gesamtumsatz eines Unternehmens — Summe von totalPrice aller
     * COMPLETED-Buchungen, bei denen das Unternehmen der Fahrrad-Eigentümer ist.
     *
     * <p>COALESCE returns 0.00 when no completed bookings exist yet.
     * <p>COALESCE gibt 0.00 zurück, wenn noch keine abgeschlossenen Buchungen existieren.
     */
    @Query("""
            SELECT COALESCE(SUM(b.totalPrice), 0)
            FROM Booking b
            WHERE b.owner.id = :ownerId
              AND b.status = com.rentmybike.booking.entity.BookingStatus.COMPLETED
              AND b.deletedAt IS NULL
            """)
    BigDecimal sumTotalPriceOfCompletedByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Daily booking counts for one business's bikes since {@code from} —
     * the owner-scoped counterpart to {@link #countDailyBookingsSince}, used
     * by the business analytics chart instead of the admin one.
     * Tägliche Buchungszählungen für die Fahrräder eines Unternehmens seit
     * {@code from} — das eigentümerbezogene Gegenstück zu {@link
     * #countDailyBookingsSince}, von der Business-Analytics-Grafik anstelle
     * der Admin-Grafik verwendet.
     */
    @Query(value = """
            SELECT DATE_TRUNC('day', created_at)::date AS day, COUNT(*) AS count
            FROM bookings
            WHERE owner_id = :ownerId
              AND created_at >= :from
              AND deleted_at IS NULL
            GROUP BY day
            ORDER BY day
            """, nativeQuery = true)
    List<DailyCountProjection> countDailyBookingsByOwnerSince(@Param("ownerId") UUID ownerId, @Param("from") LocalDateTime from);

    /**
     * Daily COMPLETED-booking revenue for one business's bikes since
     * {@code from} — owner-scoped counterpart to {@link #sumDailyRevenueSince}.
     * Tägliche COMPLETED-Buchungsumsätze für die Fahrräder eines Unternehmens
     * seit {@code from} — eigentümerbezogenes Gegenstück zu {@link #sumDailyRevenueSince}.
     */
    @Query(value = """
            SELECT DATE_TRUNC('day', created_at)::date AS day, SUM(total_price) AS amount
            FROM bookings
            WHERE owner_id = :ownerId
              AND created_at >= :from
              AND deleted_at IS NULL
              AND status = 'COMPLETED'
            GROUP BY day
            ORDER BY day
            """, nativeQuery = true)
    List<DailyAmountProjection> sumDailyRevenueByOwnerSince(@Param("ownerId") UUID ownerId, @Param("from") LocalDateTime from);

    /**
     * Average rental length (in days, inclusive of both endpoints) across a
     * business's COMPLETED bookings — backs the "avg. booking duration"
     * analytics metric. 0.0 when there are no completed bookings yet.
     * Durchschnittliche Mietdauer (in Tagen, beide Endpunkte inklusive) über
     * die COMPLETED-Buchungen eines Unternehmens — Grundlage für die
     * Analytics-Kennzahl "durchschnittliche Buchungsdauer". 0.0, wenn noch
     * keine abgeschlossenen Buchungen existieren.
     */
    @Query(value = """
            SELECT COALESCE(AVG((end_date - start_date) + 1), 0)
            FROM bookings
            WHERE owner_id = :ownerId
              AND deleted_at IS NULL
              AND status = 'COMPLETED'
            """, nativeQuery = true)
    double avgCompletedRentalDaysByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Bookings for a business's bikes whose date range overlaps [from, to] —
     * used to render the business rental calendar.
     * Buchungen für die Fahrräder eines Unternehmens, deren Datumsbereich sich
     * mit [from, to] überschneidet — wird zum Rendern des
     * Business-Mietkalenders verwendet.
     *
     * <p>Overlap condition mirrors {@link #existsDateConflict} — same Allen's
     * interval algebra, just without the status restriction (calendar shows
     * all non-cancelled bookings, not just pending/accepted).
     * <p>Überschneidungsbedingung entspricht {@link #existsDateConflict} —
     * gleiche Intervallalgebra, nur ohne Statusbeschränkung (Kalender zeigt
     * alle nicht stornierten Buchungen, nicht nur ausstehende/akzeptierte).
     */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.bike bike
            JOIN FETCH b.renter
            WHERE b.owner.id = :ownerId
              AND b.deletedAt IS NULL
              AND b.status != com.rentmybike.booking.entity.BookingStatus.CANCELLED
              AND b.startDate <= :to
              AND b.endDate   >= :from
            ORDER BY b.startDate ASC
            """)
    List<Booking> findByOwnerIdAndDateRange(
            @Param("ownerId") UUID ownerId,
            @Param("from")    LocalDate from,
            @Param("to")      LocalDate to
    );

    /**
     * Upcoming ACCEPTED bookings for an owner's bikes, soonest start date
     * first — backs the business overview's "upcoming bookings" panel. Pass
     * a {@link Pageable} (e.g. {@code PageRequest.of(0, 5)}, unsorted) to
     * cap the result size; this query already has its own explicit
     * {@code ORDER BY}, so the Pageable must not carry a Sort — see the
     * Pageable/Sort pitfall documented on {@link #findByOwnerIdAndStatus}
     * above.
     * Anstehende ACCEPTED-Buchungen für die Fahrräder eines Eigentümers,
     * frühestes Startdatum zuerst — Grundlage für das
     * "anstehende Buchungen"-Panel der Business-Übersicht. Ein
     * {@link Pageable} übergeben (z. B. {@code PageRequest.of(0, 5)},
     * unsortiert), um die Ergebnisgröße zu begrenzen; diese Abfrage hat
     * bereits ein eigenes explizites {@code ORDER BY}, daher darf das
     * Pageable keinen Sort tragen.
     */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.bike bike
            JOIN FETCH b.renter
            WHERE b.owner.id = :ownerId
              AND b.deletedAt IS NULL
              AND b.status = com.rentmybike.booking.entity.BookingStatus.ACCEPTED
              AND b.startDate >= :from
            ORDER BY b.startDate ASC
            """)
    List<Booking> findUpcomingByOwnerId(
            @Param("ownerId") UUID ownerId,
            @Param("from")    LocalDate from,
            Pageable pageable
    );
}
