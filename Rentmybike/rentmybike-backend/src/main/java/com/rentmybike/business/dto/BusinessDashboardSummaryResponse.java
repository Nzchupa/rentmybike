package com.rentmybike.business.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Summary statistics for a business account's dashboard — Stage 3
 * "Business accounts". Deliberately simple aggregate numbers, no charts.
 * Zusammenfassende Statistiken für das Dashboard eines Geschäftskontos —
 * Stage 3 "Business-Konten". Bewusst einfache aggregierte Zahlen, keine
 * Diagramme.
 *
 * <p>GET /api/v1/business/dashboard/summary
 */
@Data
@Builder
public class BusinessDashboardSummaryResponse {

    /** Sum of totalPrice for all COMPLETED bookings of this business's bikes / Summe aller COMPLETED-Buchungen */
    private BigDecimal totalRevenue;

    /** Count of this business's non-deleted bike listings / Anzahl nicht gelöschter Fahrrad-Inserate */
    private long activeBikes;

    /** Count of all non-deleted bookings ever received / Anzahl aller jemals erhaltenen Buchungen */
    private long totalBookings;

    /** Average star rating (1.0–5.0) received as an owner, 0.0 if none yet / Durchschnittliche Sternebewertung */
    private double averageRating;
}
