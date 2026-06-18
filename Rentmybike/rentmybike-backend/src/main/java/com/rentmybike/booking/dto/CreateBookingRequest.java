package com.rentmybike.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for creating a new booking — POST /api/v1/bookings.
 * DTO zum Erstellen einer neuen Buchung — POST /api/v1/bookings.
 */
@Data
public class CreateBookingRequest {

    @NotNull(message = "Bike ID is required / Fahrrad-ID ist erforderlich")
    private UUID bikeId;

    /**
     * Start date — must be today or in the future.
     * Startdatum — muss heute oder in der Zukunft liegen.
     */
    @NotNull(message = "Start date is required / Startdatum ist erforderlich")
    @FutureOrPresent(message = "Start date cannot be in the past / Startdatum darf nicht in der Vergangenheit liegen")
    private LocalDate startDate;

    /**
     * End date — must be after or equal to start date (validated in service).
     * Enddatum — muss nach oder gleich dem Startdatum sein (wird im Service geprüft).
     */
    @NotNull(message = "End date is required / Enddatum ist erforderlich")
    private LocalDate endDate;

    /** Optional message to the bike owner / Optionale Nachricht an den Fahrrad-Eigentümer */
    @Size(max = 500, message = "Message too long / Nachricht zu lang")
    private String message;

    /**
     * Optional accessory add-ons (helmets, child seats, locks) selected for
     * this rental — Stage 3 "Business accounts" feature. Each accessory must
     * belong to the same business that owns the bike.
     * Optionale Zubehör-Add-ons (Helme, Kindersitze, Schlösser), die für diese
     * Miete ausgewählt wurden — Stage-3-Feature "Business-Konten". Jedes
     * Zubehör muss demselben Unternehmen gehören, dem auch das Fahrrad gehört.
     */
    @Valid
    private List<AccessorySelectionRequest> accessories;
}
