package com.rentmybike.bike.repository;

import com.rentmybike.bike.entity.ApprovalStatus;
import com.rentmybike.bike.entity.Bike;
import com.rentmybike.bike.entity.BikeCategory;
import com.rentmybike.common.projection.DailyCountProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for bike listings.
 * Repository für Fahrrad-Inserate.
 */
@Repository
public interface BikeRepository extends JpaRepository<Bike, UUID> {

    // ──────────────────────────────────────────────────────────────────────────
    // Public search / Öffentliche Suche
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Full-featured bike search for the public listing page.
     * Vollständige Fahrradsuche für die öffentliche Übersichtsseite.
     *
     * <p>Returns only APPROVED + available + non-deleted bikes.
     * All filter params are optional (null = ignore that filter).
     *
     * <p>Gibt nur APPROVED + verfügbare + nicht gelöschte Fahrräder zurück.
     * Alle Filterparameter sind optional (null = Filter ignorieren).
     */
    // Note: no LEFT JOIN FETCH b.photos here — combining a collection fetch
    // join with Pageable causes Hibernate to paginate in memory (and, with
    // Spring Data's auto-derived COUNT query, can throw at runtime). Photos
    // are lazily loaded per-bike instead — fine at these page sizes.
    // Hinweis: kein LEFT JOIN FETCH b.photos hier — die Kombination eines
    // Collection-Fetch-Joins mit Pageable führt dazu, dass Hibernate im
    // Speicher paginiert (und mit der automatisch abgeleiteten COUNT-Abfrage
    // von Spring Data zur Laufzeit eine Exception auslösen kann). Fotos
    // werden stattdessen pro Fahrrad lazy geladen — bei diesen Seitengrößen unbedenklich.
    @Query("""
            SELECT b FROM Bike b
            JOIN FETCH b.owner o
            WHERE b.deletedAt IS NULL
              AND b.approvalStatus = 'APPROVED'
              AND b.available = true
              AND (:city       IS NULL OR LOWER(b.city) LIKE LOWER(CONCAT('%', CAST(:city AS string), '%')))
              AND (:category   IS NULL OR b.category = :category)
              AND (:model      IS NULL OR LOWER(b.model) LIKE LOWER(CONCAT('%', CAST(:model AS string), '%')))
              AND (:minPrice   IS NULL OR b.pricePerDay >= :minPrice)
              AND (:maxPrice   IS NULL OR b.pricePerDay <= :maxPrice)
            ORDER BY b.createdAt DESC
            """)
    Page<Bike> searchPublic(
            @Param("city")      String city,
            @Param("category")  BikeCategory category,
            @Param("model")     String model,
            @Param("minPrice")  BigDecimal minPrice,
            @Param("maxPrice")  BigDecimal maxPrice,
            Pageable pageable
    );

    // ──────────────────────────────────────────────────────────────────────────
    // Owner queries / Eigentümer-Abfragen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * All bikes owned by a user (for "My Bikes" page), including pending/rejected.
     * Alle Fahrräder eines Benutzers (für "Meine Fahrräder"-Seite), inkl. pending/rejected.
     */
    // No collection fetch join — see comment on searchPublic() above for why.
    @Query("""
            SELECT b FROM Bike b
            WHERE b.owner.id = :ownerId
              AND b.deletedAt IS NULL
            ORDER BY b.createdAt DESC
            """)
    Page<Bike> findByOwnerIdAndDeletedAtIsNull(
            @Param("ownerId") UUID ownerId,
            Pageable pageable
    );

    /**
     * All non-deleted bikes owned by a user, unpaginated — used by AdminService
     * to cascade-soft-delete a user's listings when their account is deleted.
     * Alle nicht gelöschten Fahrräder eines Benutzers, ohne Paginierung — wird
     * von AdminService verwendet, um die Inserate eines Benutzers bei
     * Kontolöschung kaskadiert soft zu löschen.
     */
    @Query("""
            SELECT b FROM Bike b
            WHERE b.owner.id = :ownerId
              AND b.deletedAt IS NULL
            """)
    List<Bike> findActiveBikesByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Count of non-deleted bikes owned by a user — used for the business
     * dashboard summary's "active bikes" figure.
     * Anzahl nicht gelöschter Fahrräder eines Benutzers — wird für die Kennzahl
     * "aktive Fahrräder" im Business-Dashboard verwendet.
     */
    long countByOwnerIdAndDeletedAtIsNull(@Param("ownerId") UUID ownerId);

    // ──────────────────────────────────────────────────────────────────────────
    // Admin queries / Admin-Abfragen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * All bikes filtered by approval status (for admin moderation queue).
     * Alle Fahrräder gefiltert nach Genehmigungsstatus (für die Admin-Moderationswarteschlange).
     */
    // No collection fetch join — see comment on searchPublic() above for why.
    @Query("""
            SELECT b FROM Bike b
            JOIN FETCH b.owner o
            WHERE b.deletedAt IS NULL
              AND (:status IS NULL OR b.approvalStatus = :status)
            ORDER BY b.createdAt ASC
            """)
    Page<Bike> findAllForAdmin(
            @Param("status") ApprovalStatus status,
            Pageable pageable
    );

    // ──────────────────────────────────────────────────────────────────────────
    // Single bike fetches / Einzelne Fahrrad-Abfragen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Loads a bike with its owner and photos in a single query.
     * Lädt ein Fahrrad mit Eigentümer und Fotos in einer einzigen Abfrage.
     */
    @Query("""
            SELECT b FROM Bike b
            JOIN FETCH b.owner o
            LEFT JOIN FETCH b.photos p
            WHERE b.id = :id
              AND b.deletedAt IS NULL
            """)
    Optional<Bike> findByIdWithDetails(@Param("id") UUID id);

    /**
     * Locks the bike row (SELECT ... FOR UPDATE) for the duration of the
     * caller's transaction. Used by BookingService.createBooking() to
     * serialize concurrent booking attempts for the same bike — without this,
     * two requests can both pass the date-conflict check before either
     * commits, resulting in a double-booking.
     *
     * Sperrt die Fahrrad-Zeile (SELECT ... FOR UPDATE) für die Dauer der
     * Transaktion des Aufrufers. Wird von BookingService.createBooking()
     * verwendet, um gleichzeitige Buchungsversuche für dasselbe Fahrrad zu
     * serialisieren — ohne dies können zwei Anfragen die
     * Datumskonflikt-Prüfung bestehen, bevor eine von beiden committet,
     * was zu einer Doppelbuchung führt.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bike b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Bike> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Verify ownership without loading full entity (used for auth checks).
     * Eigentümerschaft prüfen ohne vollständige Entity zu laden (für Auth-Prüfungen).
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Bike b
            WHERE b.id = :bikeId
              AND b.owner.id = :ownerId
              AND b.deletedAt IS NULL
            """)
    boolean existsByIdAndOwnerIdAndDeletedAtIsNull(
            @Param("bikeId")  UUID bikeId,
            @Param("ownerId") UUID ownerId
    );

    // ──────────────────────────────────────────────────────────────────────────
    // Admin stats counts / Admin-Statistikzählungen
    // ──────────────────────────────────────────────────────────────────────────

    /** Total non-deleted bikes / Gesamtanzahl nicht gelöschter Fahrräder */
    long countByDeletedAtIsNull();

    /**
     * Non-deleted bikes filtered by approval status.
     * Nicht gelöschte Fahrräder gefiltert nach Genehmigungsstatus.
     */
    long countByApprovalStatusAndDeletedAtIsNull(ApprovalStatus approvalStatus);

    // ──────────────────────────────────────────────────────────────────────────
    // Admin analytics time-series / Admin-Analyse-Zeitreihe
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Daily new-listing counts since {@code from}, one row per day that had
     * at least one new bike listing. Native query, mirrors {@code
     * UserRepository.countDailySignupsSince} — see that Javadoc for why
     * native is required and where gap-filling happens.
     * Tägliche Anzahl neuer Inserate seit {@code from}, eine Zeile pro Tag
     * mit mindestens einem neuen Fahrrad-Inserat. Native Abfrage, entspricht
     * {@code UserRepository.countDailySignupsSince} — siehe dortiges
     * Javadoc, warum native erforderlich ist und wo die Lückenfüllung
     * stattfindet.
     */
    @Query(value = """
            SELECT DATE_TRUNC('day', created_at)::date AS day, COUNT(*) AS count
            FROM bikes
            WHERE created_at >= :from
              AND deleted_at IS NULL
            GROUP BY day
            ORDER BY day
            """, nativeQuery = true)
    List<DailyCountProjection> countDailyListingsSince(@Param("from") LocalDateTime from);

    // ──────────────────────────────────────────────────────────────────────────
    // View count / Aufrufzähler
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Atomically increments a bike's view count by 1. Issued as a single
     * {@code UPDATE ... SET view_count = view_count + 1}, not a
     * read-modify-write, so concurrent viewers never lose an increment to a
     * race condition.
     * Erhöht den Aufrufzähler eines Fahrrads atomisch um 1. Als einzelnes
     * {@code UPDATE ... SET view_count = view_count + 1} ausgeführt, nicht
     * als Lesen-Ändern-Schreiben, damit gleichzeitige Betrachter nie eine
     * Erhöhung durch eine Race Condition verlieren.
     */
    @Modifying
    @Query("UPDATE Bike b SET b.viewCount = b.viewCount + 1 WHERE b.id = :id")
    void incrementViewCount(@Param("id") UUID id);

    // ──────────────────────────────────────────────────────────────────────────
    // Business analytics / Business-Analytik
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Sum of viewCount across all of a business's non-deleted bikes — the
     * denominator for the "conversion rate" analytics metric (bookings ÷ views).
     * Summe des Aufrufzählers über alle nicht gelöschten Fahrräder eines
     * Unternehmens — der Nenner für die Analytics-Kennzahl "Konversionsrate"
     * (Buchungen ÷ Aufrufe).
     */
    @Query("SELECT COALESCE(SUM(b.viewCount), 0) FROM Bike b WHERE b.owner.id = :ownerId AND b.deletedAt IS NULL")
    long sumViewCountByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Top bikes for a business, ranked by booking count (ties broken by view
     * count) — backs the "popular bikes" analytics panel. Pass a {@link
     * Pageable} (e.g. {@code PageRequest.of(0, 5)}, unsorted) to cap the
     * result size; native queries only use the Pageable's
     * offset/limit, not its Sort, so the explicit ORDER BY below is what
     * actually determines ranking.
     * Top-Fahrräder eines Unternehmens, gerankt nach Buchungsanzahl
     * (Gleichstand nach Aufrufzähler) — Grundlage für das
     * "beliebte Fahrräder"-Analytics-Panel. Ein {@link Pageable} übergeben
     * (z. B. {@code PageRequest.of(0, 5)}, unsortiert), um die Ergebnisgröße
     * zu begrenzen; native Abfragen verwenden vom Pageable nur
     * Offset/Limit, nicht dessen Sort, daher bestimmt das explizite ORDER BY
     * unten die tatsächliche Rangfolge.
     */
    @Query(value = """
            SELECT b.id AS bikeId, b.title AS title, b.view_count AS viewCount,
                   COUNT(bk.id) AS bookingCount,
                   COALESCE(SUM(CASE WHEN bk.status = 'COMPLETED' THEN bk.total_price ELSE 0 END), 0) AS revenue
            FROM bikes b
            LEFT JOIN bookings bk ON bk.bike_id = b.id AND bk.deleted_at IS NULL
            WHERE b.owner_id = :ownerId
              AND b.deleted_at IS NULL
            GROUP BY b.id, b.title, b.view_count
            ORDER BY bookingCount DESC, viewCount DESC
            """, nativeQuery = true)
    List<PopularBikeProjection> findPopularBikesByOwnerId(@Param("ownerId") UUID ownerId, Pageable pageable);
}
