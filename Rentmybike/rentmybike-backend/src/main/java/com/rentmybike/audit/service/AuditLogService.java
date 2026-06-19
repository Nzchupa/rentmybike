package com.rentmybike.audit.service;

import com.rentmybike.audit.dto.AuditLogResponse;
import com.rentmybike.audit.entity.AuditAction;
import com.rentmybike.audit.entity.AuditLog;
import com.rentmybike.audit.repository.AuditLogRepository;
import com.rentmybike.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for recording and browsing the admin/moderation/account audit log.
 * Service zum Erfassen und Durchsuchen des Admin-/Moderations-/Konto-Audit-Logs.
 *
 * <p>Deliberately dependency-free beyond {@link AuditLogRepository} — this
 * service is injected into AdminService, BikeService, AuthService, and
 * BookingService to record events, so it must never depend back on any of
 * those packages (or anything that transitively does), to avoid a circular
 * bean dependency.
 * <p>Bewusst frei von Abhängigkeiten außer {@link AuditLogRepository} —
 * dieser Service wird in AdminService, BikeService, AuthService und
 * BookingService injiziert, um Ereignisse aufzuzeichnen, daher darf er
 * niemals auf eines dieser Pakete (oder etwas, das transitiv davon abhängt)
 * zurückgreifen, um eine zirkuläre Bean-Abhängigkeit zu vermeiden.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Builds and saves a single audit log entry.
     * Erstellt und speichert einen einzelnen Audit-Log-Eintrag.
     *
     * @param actorId    the admin/user who triggered the event, null for system events /
     *                   der Admin/Benutzer, der das Ereignis ausgelöst hat, null bei Systemereignissen
     * @param actorName  denormalized snapshot of the actor's name / Momentaufnahme des Akteursnamens
     * @param action     what happened / was passiert ist
     * @param targetType e.g. "USER" / "BIKE" / "BOOKING"
     * @param targetId   the affected entity's id, nullable / die ID der betroffenen Entity, nullable
     * @param details    free-text context, nullable / Freitext-Kontext, nullable
     */
    public void record(UUID actorId, String actorName, AuditAction action,
                        String targetType, UUID targetId, String details) {
        AuditLog log = AuditLog.builder()
                .actorId(actorId)
                .actorName(actorName)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .build();

        auditLogRepository.save(log);
        AuditLogService.log.debug("Audit event recorded: {} on {} {} by {} ({}) / "
                + "Audit-Ereignis aufgezeichnet: {} auf {} {} von {} ({})",
                action, targetType, targetId, actorName, actorId,
                action, targetType, targetId, actorName, actorId);
    }

    /**
     * Paginated, filterable list of audit log entries for the admin panel.
     * Paginierte, filterbare Liste von Audit-Log-Einträgen für das Admin-Panel.
     *
     * @param action     exact match, null = no filter / exakte Übereinstimmung, null = kein Filter
     * @param targetType exact match, null = no filter / exakte Übereinstimmung, null = kein Filter
     * @param search     free-text across actorName/details, null = no filter /
     *                   Freitext über actorName/details, null = kein Filter
     */
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(AuditAction action, String targetType, String search, Pageable pageable) {
        String effectiveTargetType = (targetType != null && targetType.isBlank()) ? null : targetType;
        String effectiveSearch = (search != null && search.isBlank()) ? null : search;

        Page<AuditLog> page = auditLogRepository.findAllForAdmin(action, effectiveTargetType, effectiveSearch, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .actorId(log.getActorId())
                .actorName(log.getActorName())
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
