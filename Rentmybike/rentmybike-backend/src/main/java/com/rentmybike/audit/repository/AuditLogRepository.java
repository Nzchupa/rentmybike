package com.rentmybike.audit.repository;

import com.rentmybike.audit.entity.AuditAction;
import com.rentmybike.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for the immutable audit log.
 * Repository für das unveränderliche Audit-Log.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Paginated admin browse/filter/search over the audit log.
     * Paginiertes Admin-Durchsuchen/-Filtern/-Suchen über das Audit-Log.
     *
     * <p>All three filters are optional — same null-coalescing
     * {@code (:param IS NULL OR ...)} style as {@code UserRepository.findAllForAdmin}.
     * {@code search} matches case-insensitively against actorName and details.
     * <p>Alle drei Filter sind optional — gleicher Null-Koaleszenz-Stil
     * {@code (:param IS NULL OR ...)} wie {@code UserRepository.findAllForAdmin}.
     * {@code search} vergleicht groß-/kleinschreibungsunabhängig mit actorName und details.
     *
     * @param action     exact match, null = no filter / exakte Übereinstimmung, null = kein Filter
     * @param targetType exact match, null = no filter / exakte Übereinstimmung, null = kein Filter
     * @param search     free-text across actorName/details, null = no filter /
     *                   Freitext über actorName/details, null = kein Filter
     *
     * <p>{@code CAST(:search AS string)} is required — see the detailed
     * explanation on {@code UserRepository.findAllForAdmin}. Without it, a
     * null search bound only inside CONCAT/LIKE got sent as bytea and the
     * query 500'd.
     * <p>{@code CAST(:search AS string)} ist erforderlich — siehe die
     * ausführliche Erklärung bei {@code UserRepository.findAllForAdmin}. Ohne
     * ihn wurde ein null-Suchparameter, der nur innerhalb von CONCAT/LIKE
     * gebunden war, als bytea gesendet, und die Abfrage schlug mit 500 fehl.
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:action IS NULL OR a.action = :action)
              AND (:targetType IS NULL OR a.targetType = :targetType)
              AND (:search IS NULL
                   OR LOWER(a.actorName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(a.details)   LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> findAllForAdmin(
            @Param("action") AuditAction action,
            @Param("targetType") String targetType,
            @Param("search") String search,
            Pageable pageable);
}
