package com.rentmybike.business.dto;

import com.rentmybike.booking.dto.BookingResponse;
import com.rentmybike.review.dto.ReviewResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Supplementary "at a glance" lists for the business overview page, beyond
 * the plain totals in {@code BusinessDashboardSummaryResponse} and the chart
 * data in {@code BusinessAnalyticsResponse}: bookings awaiting the owner's
 * action, the soonest upcoming confirmed bookings, and the most recent
 * reviews received.
 * Ergänzende "auf einen Blick"-Listen für die Business-Übersichtsseite,
 * über die reinen Summen in {@code BusinessDashboardSummaryResponse} und die
 * Diagrammdaten in {@code BusinessAnalyticsResponse} hinaus: Buchungen, die
 * auf die Aktion des Eigentümers warten, die nächsten anstehenden
 * bestätigten Buchungen und die zuletzt erhaltenen Bewertungen.
 */
@Data
@Builder
public class BusinessOverviewExtrasResponse {

    /** PENDING bookings awaiting accept/reject, most recent first / PENDING-Buchungen, die auf Annahme/Ablehnung warten, neueste zuerst */
    private List<BookingResponse> pendingActionBookings;

    /** ACCEPTED bookings with the soonest start date / ACCEPTED-Buchungen mit dem frühesten Startdatum */
    private List<BookingResponse> upcomingBookings;

    /** Most recently received reviews / Zuletzt erhaltene Bewertungen */
    private List<ReviewResponse> recentReviews;
}
