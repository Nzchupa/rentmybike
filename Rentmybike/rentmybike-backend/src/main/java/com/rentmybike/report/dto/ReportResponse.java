package com.rentmybike.report.dto;

import com.rentmybike.report.entity.ReportReason;
import com.rentmybike.report.entity.ReportStatus;
import com.rentmybike.report.entity.ReportTargetType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReportResponse {

    private UUID id;
    private UUID reporterId;
    private String reporterName;
    private ReportTargetType targetType;
    private UUID targetId;

    /**
     * Best-effort human-readable label for the reported target (bike title /
     * user name / review snippet), resolved at read time — null if the
     * target was since deleted.
     * Bestmögliches lesbares Label für das gemeldete Ziel (Fahrradtitel /
     * Benutzername / Bewertungsausschnitt), zum Lesezeitpunkt aufgelöst —
     * null, falls das Ziel inzwischen gelöscht wurde.
     */
    private String targetLabel;

    private ReportReason reason;
    private String details;
    private ReportStatus status;
    private String resolutionNote;
    private UUID resolvedBy;
    private String resolvedByName;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
