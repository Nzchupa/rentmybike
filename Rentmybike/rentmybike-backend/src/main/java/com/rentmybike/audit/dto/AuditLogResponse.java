package com.rentmybike.audit.dto;

import com.rentmybike.audit.entity.AuditAction;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Flat DTO mirroring the AuditLog entity, for the admin audit log endpoint.
 * Flaches DTO, das die AuditLog-Entity spiegelt, für den Admin-Audit-Log-Endpunkt.
 */
@Data
@Builder
public class AuditLogResponse {

    private UUID id;

    /** Null for system events / Null bei Systemereignissen */
    private UUID actorId;

    /** Denormalized snapshot of the actor's name at event time / Momentaufnahme des Akteursnamens */
    private String actorName;

    private AuditAction action;

    /** e.g. "USER" / "BIKE" / "BOOKING" */
    private String targetType;

    /** Nullable — the affected entity's id */
    private UUID targetId;

    /** Free-text context, nullable */
    private String details;

    private LocalDateTime createdAt;
}
