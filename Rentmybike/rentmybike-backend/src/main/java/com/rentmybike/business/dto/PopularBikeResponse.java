package com.rentmybike.business.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One row in the business analytics "popular bikes" panel — mapped from
 * {@code PopularBikeProjection}.
 * Eine Zeile im "beliebte Fahrräder"-Panel der Business-Analytik —
 * gemappt von {@code PopularBikeProjection}.
 */
@Data
@Builder
public class PopularBikeResponse {

    private UUID bikeId;
    private String title;

    /** Public detail-page view count / Anzahl öffentlicher Detailseiten-Aufrufe */
    private long viewCount;

    /** Total non-deleted bookings for this bike / Gesamtanzahl nicht gelöschter Buchungen für dieses Fahrrad */
    private long bookingCount;

    /** Revenue from this bike's COMPLETED bookings / Umsatz aus COMPLETED-Buchungen dieses Fahrrads */
    private BigDecimal revenue;
}
