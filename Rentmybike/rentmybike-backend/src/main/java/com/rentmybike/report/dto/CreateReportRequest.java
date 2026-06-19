package com.rentmybike.report.dto;

import com.rentmybike.report.entity.ReportReason;
import com.rentmybike.report.entity.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/**
 * Request body for a user filing a new report.
 * Request-Body zum Einreichen einer neuen Meldung durch einen Benutzer.
 *
 * <p>POST /api/v1/reports
 */
@Data
public class CreateReportRequest {

    @NotNull(message = "Target type is required / Zieltyp ist erforderlich")
    private ReportTargetType targetType;

    @NotNull(message = "Target id is required / Ziel-ID ist erforderlich")
    private UUID targetId;

    @NotNull(message = "Reason is required / Grund ist erforderlich")
    private ReportReason reason;

    @Size(max = 2000, message = "Details must be 2000 characters or fewer / Details dürfen maximal 2000 Zeichen lang sein")
    private String details;
}
