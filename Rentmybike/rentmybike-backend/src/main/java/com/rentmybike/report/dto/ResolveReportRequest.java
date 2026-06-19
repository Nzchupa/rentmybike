package com.rentmybike.report.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for an admin resolving, dismissing, or warning on a report.
 * Request-Body für einen Admin, der eine Meldung löst, ablehnt oder eine
 * Verwarnung darauf ausspricht.
 *
 * <p>resolutionNote is optional context the admin leaves behind, e.g. "listing
 * removed, owner warned" — surfaced in the report detail and audit log.
 */
@Data
public class ResolveReportRequest {

    @Size(max = 2000, message = "Resolution note must be 2000 characters or fewer / Auflösungsnotiz darf maximal 2000 Zeichen lang sein")
    private String resolutionNote;
}
