package com.rentmybike.user.repository;

import com.rentmybike.common.projection.DailyCountProjection;
import com.rentmybike.user.entity.User;
import com.rentmybike.user.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity — provides database access methods.
 * Repository für die User-Entität — bietet Datenbankzugriffsmethoden.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by their email address (case-insensitive via DB collation).
     * Benutzer anhand der E-Mail-Adresse finden (Groß-/Kleinschreibung je nach DB-Collation).
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if an email is already registered (used during registration validation).
     * Prüfen, ob eine E-Mail bereits registriert ist (wird bei der Registrierungsvalidierung verwendet).
     */
    boolean existsByEmail(String email);

    /**
     * Find a user by their email verification token.
     * Benutzer anhand des E-Mail-Verifizierungstokens finden.
     * Excludes soft-deleted users / Soft-gelöschte Benutzer ausschließen.
     */
    @Query("SELECT u FROM User u WHERE u.emailVerificationToken = :token AND u.deletedAt IS NULL")
    Optional<User> findByEmailVerificationToken(@Param("token") String token);

    // ──────────────────────────────────────────────────────────────────────────
    // Admin user management / Admin-Benutzerverwaltung
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Paginated user list for admin panel — optional search by name or email.
     * Paginierte Benutzerliste für Admin-Panel — optionale Suche nach Name oder E-Mail.
     *
     * <p>Search is case-insensitive LIKE across email, firstName, lastName.
     * <p>Suche ist Groß-/Kleinschreibungsunabhängig LIKE über email, firstName, lastName.
     *
     * <p>The explicit {@code CAST(:search AS string)} matters: when {@code search}
     * is null (no filter applied) and it only ever appears inside {@code CONCAT}/
     * {@code LIKE}, Postgres has no other clue about its type and the driver was
     * sending it as {@code bytea} — every admin/users request 500'd with
     * "function lower(bytea) does not exist". Forcing the cast pins the bind
     * parameter to text regardless of whether a value is present.
     * <p>Der explizite {@code CAST(:search AS string)} ist wichtig: Wenn
     * {@code search} null ist (kein Filter aktiv) und nur innerhalb von
     * {@code CONCAT}/{@code LIKE} auftaucht, hat Postgres keinen anderen Hinweis
     * auf den Typ, und der Treiber sendete ihn als {@code bytea} — jede
     * admin/users-Anfrage schlug mit 500 fehl ("function lower(bytea) does not
     * exist"). Der erzwungene Cast legt den Bind-Parameter unabhängig davon, ob
     * ein Wert vorhanden ist, auf Text fest.
     *
     * @param search null means no filter / null bedeutet kein Filter
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.deletedAt IS NULL
              AND (:search IS NULL
                   OR LOWER(u.email)     LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            ORDER BY u.createdAt DESC
            """)
    Page<User> findAllForAdmin(@Param("search") String search, Pageable pageable);

    /** Total active (non-deleted) users / Gesamtanzahl aktiver (nicht gelöschter) Benutzer */
    long countByDeletedAtIsNull();

    /** Total banned (and non-deleted) users / Gesamtanzahl gesperrter (und nicht gelöschter) Benutzer */
    long countByBannedAtIsNotNullAndDeletedAtIsNull();

    /** Total users with a specific role / Gesamtanzahl Benutzer mit einer bestimmten Rolle */
    long countByRoleAndDeletedAtIsNull(UserRole role);

    /**
     * All active users with a given role — used to fan out admin-facing
     * notifications (e.g. new pending bike, new report) to every admin.
     * Alle aktiven Benutzer mit einer bestimmten Rolle — wird verwendet, um
     * an Admins gerichtete Benachrichtigungen (z. B. neues ausstehendes
     * Fahrrad, neue Meldung) an jeden Admin zu verteilen.
     */
    java.util.List<User> findAllByRoleAndDeletedAtIsNull(UserRole role);

    // ──────────────────────────────────────────────────────────────────────────
    // Admin analytics time-series / Admin-Analyse-Zeitreihe
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Daily new-signup counts since {@code from}, one row per day that had
     * at least one signup. Native query — {@code DATE_TRUNC} has no JPQL
     * equivalent. Gaps (days with zero signups) are filled in by {@code
     * AdminService.getAnalytics}, not here.
     * Tägliche Neuanmeldungs-Zählungen seit {@code from}, eine Zeile pro Tag
     * mit mindestens einer Anmeldung. Native Abfrage — {@code DATE_TRUNC}
     * hat keine JPQL-Entsprechung. Lücken (Tage ohne Anmeldungen) werden von
     * {@code AdminService.getAnalytics} aufgefüllt, nicht hier.
     */
    @Query(value = """
            SELECT DATE_TRUNC('day', created_at)::date AS day, COUNT(*) AS count
            FROM users
            WHERE created_at >= :from
              AND deleted_at IS NULL
            GROUP BY day
            ORDER BY day
            """, nativeQuery = true)
    List<DailyCountProjection> countDailySignupsSince(@Param("from") LocalDateTime from);
}
