package com.rentmybike.bike.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * DTO for bulk-creating bike listings — POST /api/v1/bikes/bulk.
 * DTO zum massenhaften Erstellen von Fahrrad-Inseraten — POST /api/v1/bikes/bulk.
 *
 * <p>BUSINESS-account only — lets bike shops onboard their fleet in one
 * request instead of submitting each bike individually.
 * <p>Nur für BUSINESS-Konten — ermöglicht es Fahrradläden, ihre Flotte in
 * einer Anfrage anzulegen, statt jedes Fahrrad einzeln einzureichen.
 */
@Data
public class BulkCreateBikeRequest {

    @NotEmpty(message = "At least one bike is required / Mindestens ein Fahrrad ist erforderlich")
    @Size(max = 50, message = "Maximum 50 bikes per batch / Maximal 50 Fahrräder pro Stapel")
    @Valid
    private List<CreateBikeRequest> bikes;
}
