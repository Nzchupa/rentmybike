package com.rentmybike.user.repository;

import com.rentmybike.user.entity.User;
import com.rentmybike.user.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * @param search null means no filter / null bedeutet kein Filter
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.deletedAt IS NULL
              AND (:search IS NULL
                   OR LOWER(u.email)     LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY u.createdAt DESC
            """)
    Page<User> findAllForAdmin(@Param("search") String search, Pageable pageable);

    /** Total active (non-deleted) users / Gesamtanzahl aktiver (nicht gelöschter) Benutzer */
    long countByDeletedAtIsNull();

    /** Total banned (and non-deleted) users / Gesamtanzahl gesperrter (und nicht gelöschter) Benutzer */
    long countByBannedAtIsNotNullAndDeletedAtIsNull();

    /** Total users with a specific role / Gesamtanzahl Benutzer mit einer bestimmten Rolle */
    long countByRoleAndDeletedAtIsNull(UserRole role);
}
