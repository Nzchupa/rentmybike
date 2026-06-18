package com.rentmybike.accessory.dto;

import com.rentmybike.accessory.entity.AccessoryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for creating a new accessory — POST /api/v1/accessories.
 * DTO zum Erstellen eines neuen Zubehörs — POST /api/v1/accessories.
 */
@Data
public class CreateAccessoryRequest {

    @NotNull(message = "Accessory type is required / Zubehörtyp ist erforderlich")
    private AccessoryType type;

    @NotBlank(message = "Name is required / Name ist erforderlich")
    @Size(max = 100, message = "Name too long / Name zu lang")
    private String name;

    @Min(value = 1, message = "Quantity must be at least 1 / Menge muss mindestens 1 sein")
    private int quantityTotal;

    @NotNull(message = "Price per day is required / Preis pro Tag ist erforderlich")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive / Preis muss positiv sein")
    private BigDecimal pricePerDay;
}
