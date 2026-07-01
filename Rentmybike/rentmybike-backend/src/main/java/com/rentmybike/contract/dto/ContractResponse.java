package com.rentmybike.contract.dto;

import com.rentmybike.booking.entity.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for a rental contract in API responses — structured data fields plus
 * the fully rendered legal text as an ordered list of sections.
 * DTO für einen Mietvertrag in API-Antworten — strukturierte Datenfelder
 * plus der vollständig gerenderte Rechtstext als geordnete Liste von
 * Abschnitten.
 */
@Data
@Builder
public class ContractResponse {

    private UUID id;
    private UUID bookingId;

    private String ownerName;
    private String ownerEmail;
    private String renterName;
    private String renterEmail;

    private String bikeTitle;
    private String bikeModel;
    private String bikeCategory;
    private String bikeCity;
    private String bikeAddress;

    private LocalDate startDate;
    private LocalDate endDate;
    private int rentalDays;

    private BigDecimal pricePerDay;
    private BigDecimal totalPrice;
    private PaymentMethod paymentMethod;
    private BigDecimal depositAmount;

    private LocalDateTime ownerAcceptedAt;
    private LocalDateTime renterAcceptedAt;
    private boolean fullyAccepted;

    /** Whether the requesting user still needs to click "I accept". */
    private boolean acceptedByMe;

    private LocalDateTime createdAt;

    /** Fully rendered legal text, §1 first / Vollständig gerenderter Rechtstext, §1 zuerst */
    private List<ContractSectionResponse> sections;
}
