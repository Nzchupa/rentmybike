package com.rentmybike.booking.dto;

import com.rentmybike.accessory.entity.AccessoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * An accessory line item within {@link BookingResponse}.
 * Eine Zubehör-Positionszeile innerhalb von {@link BookingResponse}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingAccessoryResponse {
    private UUID accessoryId;
    private AccessoryType type;
    private String name;
    private int quantity;
    private BigDecimal pricePerDayAtBooking;
    private BigDecimal lineTotal;
}
