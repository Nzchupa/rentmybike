package com.rentmybike.report.service;

import com.rentmybike.admin.dto.AdminUserResponse;
import com.rentmybike.admin.service.AdminService;
import com.rentmybike.audit.entity.AuditAction;
import com.rentmybike.audit.service.AuditLogService;
import com.rentmybike.bike.entity.Bike;
import com.rentmybike.bike.repository.BikeRepository;
import com.rentmybike.common.exception.BusinessException;
import com.rentmybike.common.exception.ResourceNotFoundException;
import com.rentmybike.common.response.PageResponse;
import com.rentmybike.notification.service.NotificationService;
import com.rentmybike.report.dto.CreateReportRequest;
import com.rentmybike.report.dto.ReportResponse;
import com.rentmybike.report.dto.ResolveReportRequest;
import com.rentmybike.report.entity.Report;
import com.rentmybike.report.entity.ReportStatus;
import com.rentmybike.report.entity.ReportTargetType;
import com.rentmybike.report.repository.ReportRepository;
import com.rentmybike.review.entity.Review;
import com.rentmybike.review.repository.ReviewRepository;
import com.rentmybike.user.entity.User;
import com.rentmybike.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reports service — filing complaints about bikes/users/reviews, and the
 * admin triage workflow (review / resolve / dismiss / warn / ban) that acts
 * on them.
 * Meldungs-Service — Einreichen von Beschwerden über Fahrräder/Benutzer/
 * Bewertungen sowie der Admin-Triage-Workflow (prüfen / lösen / ablehnen /
 * verwarnen / sperren), der darauf reagiert.
 *
 * <p>Ban/warn act on the *responsible* user for a report's target — the
 * bike's owner for a BIKE report, the review's author for a REVIEW report,
 * or the user directly for a USER report. Banning reuses
 * {@code AdminService.banUser} rather than duplicating its admin-cannot-be-
 * banned / cannot-ban-self guards.
 * <p>Sperren/Verwarnen wirken auf den *verantwortlichen* Benutzer des Ziels
 * einer Meldung — den Fahrrad-Eigentümer bei einer BIKE-Meldung, den Autor
 * der Bewertung bei einer REVIEW-Meldung, oder direkt den Benutzer bei einer
 * USER-Meldung. Sperren nutzt {@code AdminService.banUser} wieder, statt
 * dessen Schutzmechanismen (Admin kann nicht gesperrt werden / sich nicht
 * selbst sperren) zu duplizieren.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final BikeRepository bikeRepository;
    private final ReviewRepository reviewRepository;
    private final AuditLogService auditLogService;
    private final AdminService adminService;
    private final NotificationService notificationService;

    // ──────────────────────────────────────────────────────────────────────────
    // Filing / Einreichen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Files a new report. The target is not validated against its source
     * table beyond existing — a report about an already-deleted bike/user/
     * review is still useful context for admins, so we don't 404 here.
     * Reicht eine neue Meldung ein. Das Ziel wird über die reine Existenz
     * hinaus nicht gegen seine Quelltabelle validiert — eine Meldung über ein
     * bereits gelöschtes Fahrrad/Benutzer/Bewertung ist für Admins weiterhin
     * nützlicher Kontext, daher wird hier kein 404 ausgelöst.
     */
    public ReportResponse createReport(UUID reporterId, CreateReportRequest request) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reporterId));

        Report report = Report.builder()
                .reporterId(reporterId)
                .reporterName(reporter.getFullName())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .details(request.getDetails())
                .status(ReportStatus.PENDING)
                .build();

        report = reportRepository.save(report);
        log.info("User {} filed a {} report against {} {} / "
                + "Benutzer {} hat eine {}-Meldung gegen {} {} eingereicht",
                reporterId, request.getReason(), request.getTargetType(), request.getTargetId(),
                reporterId, request.getReason(), request.getTargetType(), request.getTargetId());

        notificationService.notifyAdminsOfNewReport(report);

        return toResponse(report);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Admin: list / detail / Admin: Liste / Detail
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> list(ReportStatus status, ReportTargetType targetType, String search, Pageable pageable) {
        String effectiveSearch = (search != null && search.isBlank()) ? null : search;
        Page<Report> page = reportRepository.findAllForAdmin(status, targetType, effectiveSearch, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ReportResponse getById(UUID id) {
        return toResponse(findActiveReport(id));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Admin: triage actions / Admin: Triage-Aktionen
    // ──────────────────────────────────────────────────────────────────────────

    /** Marks a report as being actively worked / Markiert eine Meldung als aktiv bearbeitet */
    public ReportResponse markUnderReview(UUID id, UUID adminId) {
        Report report = findActiveReport(id);
        requireOpen(report);
        report.setStatus(ReportStatus.UNDER_REVIEW);
        log.info("Admin {} marked report {} as under review / Admin {} hat Meldung {} als in Prüfung markiert",
                adminId, id, adminId, id);
        return toResponse(report);
    }

    /** Resolves a report with no further account action / Löst eine Meldung ohne weitere Kontoaktion */
    public ReportResponse resolve(UUID id, UUID adminId, ResolveReportRequest request) {
        Report report = findActiveReport(id);
        requireOpen(report);
        close(report, ReportStatus.RESOLVED, adminId, request.getResolutionNote());

        auditLogService.record(adminId, adminName(adminId), AuditAction.REPORT_RESOLVED,
                report.getTargetType().name(), report.getTargetId(), request.getResolutionNote());

        return toResponse(report);
    }

    /** Dismisses a report as unfounded / Lehnt eine Meldung als unbegründet ab */
    public ReportResponse dismiss(UUID id, UUID adminId, ResolveReportRequest request) {
        Report report = findActiveReport(id);
        requireOpen(report);
        close(report, ReportStatus.DISMISSED, adminId, request.getResolutionNote());

        auditLogService.record(adminId, adminName(adminId), AuditAction.REPORT_DISMISSED,
                report.getTargetType().name(), report.getTargetId(), request.getResolutionNote());

        return toResponse(report);
    }

    /**
     * Issues a warning to the user responsible for the reported target, and
     * resolves the report. There's no persistent "warning count" in this
     * MVP — the audit log entry is the durable record of the warning.
     * Spricht eine Verwarnung gegenüber dem für das gemeldete Ziel
     * verantwortlichen Benutzer aus und löst die Meldung. Es gibt in diesem
     * MVP keinen dauerhaften "Verwarnungszähler" — der Audit-Log-Eintrag ist
     * der dauerhafte Nachweis der Verwarnung.
     */
    public ReportResponse warn(UUID id, UUID adminId, ResolveReportRequest request) {
        Report report = findActiveReport(id);
        requireOpen(report);

        User responsible = resolveResponsibleUser(report);
        String note = request.getResolutionNote();

        auditLogService.record(adminId, adminName(adminId), AuditAction.USER_WARNED,
                "USER", responsible.getId(), note);
        log.info("Admin {} warned user {} over report {} / Admin {} hat Benutzer {} wegen Meldung {} verwarnt",
                adminId, responsible.getId(), id, adminId, responsible.getId(), id);

        close(report, ReportStatus.RESOLVED, adminId, note);
        auditLogService.record(adminId, adminName(adminId), AuditAction.REPORT_RESOLVED,
                report.getTargetType().name(), report.getTargetId(), note);

        return toResponse(report);
    }

    /**
     * Bans the user responsible for the reported target (reusing
     * {@code AdminService.banUser} so the admin-cannot-ban-admin/self rules
     * stay in one place), and resolves the report.
     * Sperrt den für das gemeldete Ziel verantwortlichen Benutzer (nutzt
     * {@code AdminService.banUser} wieder, damit die Regeln
     * "Admin kann keinen Admin/sich selbst sperren" an einer Stelle bleiben)
     * und löst die Meldung.
     */
    public ReportResponse banResponsibleUser(UUID id, UUID adminId, ResolveReportRequest request) {
        Report report = findActiveReport(id);
        requireOpen(report);

        User responsible = resolveResponsibleUser(report);
        AdminUserResponse banned = adminService.banUser(adminId, responsible.getId());
        log.info("Admin {} banned user {} over report {} / Admin {} hat Benutzer {} wegen Meldung {} gesperrt",
                adminId, banned.getId(), id, adminId, banned.getId(), id);

        close(report, ReportStatus.RESOLVED, adminId, request.getResolutionNote());
        auditLogService.record(adminId, adminName(adminId), AuditAction.REPORT_RESOLVED,
                report.getTargetType().name(), report.getTargetId(), request.getResolutionNote());

        return toResponse(report);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    private Report findActiveReport(UUID id) {
        return reportRepository.findById(id)
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
    }

    private void requireOpen(Report report) {
        if (report.isClosed()) {
            throw new BusinessException(
                    "Report has already been " + report.getStatus().name().toLowerCase()
                            + " / Meldung wurde bereits " + report.getStatus().name().toLowerCase());
        }
    }

    private void close(Report report, ReportStatus finalStatus, UUID adminId, String resolutionNote) {
        report.setStatus(finalStatus);
        report.setResolutionNote(resolutionNote);
        report.setResolvedBy(adminId);
        report.setResolvedByName(adminName(adminId));
        report.setResolvedAt(LocalDateTime.now());
    }

    /**
     * Resolves the user accountable for a report's target: the bike owner
     * for BIKE, the review's author for REVIEW, or the user directly for
     * USER reports.
     * Löst den für das Ziel einer Meldung verantwortlichen Benutzer auf: den
     * Fahrrad-Eigentümer bei BIKE, den Autor der Bewertung bei REVIEW, oder
     * direkt den Benutzer bei USER-Meldungen.
     */
    private User resolveResponsibleUser(Report report) {
        return switch (report.getTargetType()) {
            case USER -> userRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", report.getTargetId()));
            case BIKE -> {
                Bike bike = bikeRepository.findById(report.getTargetId())
                        .orElseThrow(() -> new ResourceNotFoundException("Bike", report.getTargetId()));
                yield bike.getOwner();
            }
            case REVIEW -> {
                Review review = reviewRepository.findById(report.getTargetId())
                        .orElseThrow(() -> new ResourceNotFoundException("Review", report.getTargetId()));
                yield review.getReviewer();
            }
        };
    }

    private String adminName(UUID adminId) {
        if (adminId == null) {
            return null;
        }
        return userRepository.findById(adminId).map(User::getFullName).orElse(null);
    }

    private ReportResponse toResponse(Report r) {
        return ReportResponse.builder()
                .id(r.getId())
                .reporterId(r.getReporterId())
                .reporterName(r.getReporterName())
                .targetType(r.getTargetType())
                .targetId(r.getTargetId())
                .targetLabel(resolveTargetLabel(r))
                .reason(r.getReason())
                .details(r.getDetails())
                .status(r.getStatus())
                .resolutionNote(r.getResolutionNote())
                .resolvedBy(r.getResolvedBy())
                .resolvedByName(r.getResolvedByName())
                .resolvedAt(r.getResolvedAt())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    /** Best-effort label lookup — never throws, returns null if the target is gone */
    private String resolveTargetLabel(Report r) {
        try {
            return switch (r.getTargetType()) {
                case USER -> userRepository.findById(r.getTargetId()).map(User::getFullName).orElse(null);
                case BIKE -> bikeRepository.findById(r.getTargetId()).map(Bike::getTitle).orElse(null);
                case REVIEW -> reviewRepository.findById(r.getTargetId())
                        .map(rev -> rev.getComment() != null && rev.getComment().length() > 60
                                ? rev.getComment().substring(0, 60) + "…"
                                : rev.getComment())
                        .orElse(null);
            };
        } catch (Exception e) {
            return null;
        }
    }
}
