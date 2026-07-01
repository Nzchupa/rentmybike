package com.rentmybike.bike.dto;

import com.rentmybike.bike.entity.BikeCategory;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for creating a new bike listing — POST /api/v1/bikes.
 * DTO zum Erstellen eines neuen Fahrrad-Inserats — POST /api/v1/bikes.
 *
 * <p>After creation, bike starts in PENDING approval status and is invisible
 * in public search until an admin approves it.
 * <p>Nach der Erstellung startet das Fahrrad im PENDING-Genehmigungsstatus und ist
 * in der öffentlichen Suche unsichtbar, bis ein Admin es genehmigt.
 */
@Data
public class CreateBikeRequest {

    @NotBlank(message = "Title is required / Titel ist erforderlich")
    @Size(min = 5, max = 100, message = "Title must be 5–100 chars / Titel muss 5–100 Zeichen haben")
    private String title;

    @NotBlank(message = "Description is required / Beschreibung ist erforderlich")
    @Size(min = 20, max = 3000, message = "Description must be 20–3000 chars / Beschreibung muss 20–3000 Zeichen haben")
    private String description;

    @NotNull(message = "Category is required / Kategorie ist erforderlich")
    private BikeCategory category;

    /** Optional brand/model, e.g. "Trek FX2 Disc" / Optionale Marke/Modell, z. B. "Trek FX2 Disc" */
    @Size(max = 150, message = "Model too long / Modell zu lang")
    private String model;

    @NotNull(message = "Price per day is required / Preis pro Tag ist erforderlich")
    @DecimalMin(value = "1.00", message = "Minimum price is €1.00 / Mindestpreis ist 1,00 €")
    @DecimalMax(value = "9999.99", message = "Maximum price is €9999.99 / Höchstpreis ist 9999,99 €")
    @Digits(integer = 6, fraction = 2)
    private BigDecimal pricePerDay;

    @NotBlank(message = "City is required / Stadt ist erforderlich")
    @Size(max = 100, message = "City too long / Stadt zu lang")
    private String city;

    /** Optional full address — only shown to renters after booking confirmation */
    @Size(max = 255, message = "Address too long / Adresse zu lang")
    private String address;

    /** Optional GPS latitude / Optionaler GPS-Breitengrad */
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal latitude;

    /** Optional GPS longitude / Optionaler GPS-Längengrad */
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal longitude;
}
