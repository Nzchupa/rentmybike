package com.rentmybike.bike.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for admin bike rejection — POST /api/v1/admin/bikes/{id}/reject.
 * DTO für Admin-Fahrrad-Ablehnung — POST /api/v1/admin/bikes/{id}/reject.
 *
 * <p>The rejection reason is shown to the bike owner so they can fix issues.
 * <p>Der Ablehnungsgrund wird dem Fahrrad-Eigentümer angezeigt, damit er Probleme beheben kann.
 */
@Data
public class RejectBikeRequest {

    @NotBlank(message = "Rejection reason is required / Ablehnungsgrund ist erforderlich")
    @Size(min = 10, max = 500,
          message = "Reason must be 10–500 chars / Grund muss 10–500 Zeichen haben")
    private String reason;
}
