package com.rentmybike.report.repository;

import com.rentmybike.report.entity.Report;
import com.rentmybike.report.entity.ReportStatus;
import com.rentmybike.report.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    /**
     * Admin list/filter query — null-coalescing filters, same pattern as
     * AuditLogRepository.findAllForAdmin. Search matches reporter name and
     * the free-text details/resolution note.
     * Admin-Listen-/Filterabfrage — null-koaleszierende Filter, gleiches
     * Muster wie AuditLogRepository.findAllForAdmin. Die Suche durchsucht
     * den Namen des Meldenden sowie die Freitext-Details/Auflösungsnotiz.
     *
     * <p>{@code CAST(:search AS string)} is required — see the detailed
     * explanation on {@code UserRepository.findAllForAdmin}. Without it, a
     * null search bound only inside CONCAT/LIKE got sent as bytea and every
     * admin/reports request 500'd.
     * <p>{@code CAST(:search AS string)} ist erforderlich — siehe die
     * ausführliche Erklärung bei {@code UserRepository.findAllForAdmin}. Ohne
     * ihn wurde ein null-Suchparameter, der nur innerhalb von CONCAT/LIKE
     * gebunden war, als bytea gesendet, und jede admin/reports-Anfrage schlug
     * mit 500 fehl.
     */
    @Query("""
            SELECT r FROM Report r
            WHERE r.deletedAt IS NULL
              AND (:status IS NULL OR r.status = :status)
              AND (:targetType IS NULL OR r.targetType = :targetType)
              AND (:search IS NULL
                   OR LOWER(r.reporterName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(r.details)      LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(r.resolutionNote) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            ORDER BY r.createdAt DESC
            """)
    Page<Report> findAllForAdmin(
            @Param("status") ReportStatus status,
            @Param("targetType") ReportTargetType targetType,
            @Param("search") String search,
            Pageable pageable);

    long countByStatusAndDeletedAtIsNull(ReportStatus status);
}
