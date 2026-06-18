package com.rentmybike.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for upgrading the authenticated account to a Business account —
 * POST /api/v1/business/upgrade.
 * DTO zum Upgrade des authentifizierten Kontos auf ein Geschäftskonto —
 * POST /api/v1/business/upgrade.
 */
@Data
public class UpgradeToBusinessRequest {

    @NotBlank(message = "Business name is required / Firmenname ist erforderlich")
    @Size(max = 150, message = "Business name too long / Firmenname zu lang")
    private String businessName;
}
