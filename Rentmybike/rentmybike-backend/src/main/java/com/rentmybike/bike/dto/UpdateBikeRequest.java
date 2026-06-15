package com.rentmybike.bike.dto;

import com.rentmybike.bike.entity.BikeCategory;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for updating an existing bike listing — PUT /api/v1/bikes/{id}.
 * DTO zum Aktualisieren eines vorhandenen Fahrrad-Inserats — PUT /api/v1/bikes/{id}.
 *
 * <p>Updating a previously APPROVED bike resets it to PENDING for re-review.
 * <p>Das Aktualisieren eines zuvor APPROVED-Fahrrads setzt es auf PENDING für erneute Überprüfung zurück.
 */
@Data
public class UpdateBikeRequest {

    @NotBlank(message = "Title is required / Titel ist erforderlich")
    @Size(min = 5, max = 100)
    private String title;

    @NotBlank(message = "Description is required / Beschreibung ist erforderlich")
    @Size(min = 20, max = 3000)
    private String description;

    @NotNull(message = "Category is required / Kategorie ist erforderlich")
    private BikeCategory category;

    @NotNull(message = "Price per day is required / Preis pro Tag ist erforderlich")
    @DecimalMin(value = "1.00")
    @DecimalMax(value = "9999.99")
    @Digits(integer = 6, fraction = 2)
    private BigDecimal pricePerDay;

    @NotBlank(message = "City is required / Stadt ist erforderlich")
    @Size(max = 100)
    private String city;

    @Size(max = 255)
    private String address;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal longitude;

    /** Owner can toggle availability without triggering re-review / Eigentümer kann Verfügbarkeit ohne erneute Überprüfung umschalten */
    @NotNull
    private Boolean available;
}
