package com.rentmybike.accessory.dto;

import com.rentmybike.accessory.entity.AccessoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Public-facing view of an accessory, returned by all accessory endpoints.
 * Öffentliche Ansicht eines Zubehörs, zurückgegeben von allen Zubehör-Endpunkten.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessoryResponse {
    private UUID id;
    private UUID ownerId;
    private String ownerName;
    private AccessoryType type;
    private String name;
    private int quantityTotal;
    private BigDecimal pricePerDay;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
