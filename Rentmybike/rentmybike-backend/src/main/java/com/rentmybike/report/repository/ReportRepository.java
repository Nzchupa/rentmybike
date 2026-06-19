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
     */
    @Query("""
            SELECT r FROM Report r
            WHERE r.deletedAt IS NULL
              AND (:status IS NULL OR r.status = :status)
              AND (:targetType IS NULL OR r.targetType = :targetType)
              AND (:search IS NULL
                   OR LOWER(r.reporterName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(r.details)      LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(r.resolutionNote) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY r.createdAt DESC
            """)
    Page<Report> findAllForAdmin(
            @Param("status") ReportStatus status,
            @Param("targetType") ReportTargetType targetType,
            @Param("search") String search,
            Pageable pageable);

    long countByStatusAndDeletedAtIsNull(ReportStatus status);
}
