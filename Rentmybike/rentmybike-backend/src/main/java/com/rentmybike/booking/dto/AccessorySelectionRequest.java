package com.rentmybike.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * A single accessory choice within {@link CreateBookingRequest} — "I want
 * 2 of this helmet for the whole rental period".
 * Eine einzelne Zubehörauswahl innerhalb von {@link CreateBookingRequest} —
 * "Ich möchte 2 dieser Helme für die gesamte Mietdauer".
 */
@Data
public class AccessorySelectionRequest {

    @NotNull(message = "Accessory ID is required / Zubehör-ID ist erforderlich")
    private UUID accessoryId;

    @Min(value = 1, message = "Quantity must be at least 1 / Menge muss mindestens 1 sein")
    private int quantity;
}
