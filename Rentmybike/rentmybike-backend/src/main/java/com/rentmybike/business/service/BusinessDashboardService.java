package com.rentmybike.business.service;

import com.rentmybike.bike.repository.BikeRepository;
import com.rentmybike.booking.repository.BookingRepository;
import com.rentmybike.business.dto.BusinessDashboardSummaryResponse;
import com.rentmybike.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Aggregates the simple dashboard summary numbers for a business account —
 * Stage 3 "Business accounts". No charts, just totals (per the user's
 * "Прості агреговані цифри" decision).
 * Aggregiert die einfachen Dashboard-Kennzahlen für ein Geschäftskonto —
 * Stage 3 "Business-Konten". Keine Diagramme, nur Summen.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessDashboardService {

    private final BikeRepository bikeRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;

    /**
     * Builds the dashboard summary for a business owner.
     * Erstellt die Dashboard-Zusammenfassung für einen Business-Eigentümer.
     */
    public BusinessDashboardSummaryResponse getDashboardSummary(UUID ownerId) {
        return BusinessDashboardSummaryResponse.builder()
                .totalRevenue(bookingRepository.sumTotalPriceOfCompletedByOwnerId(ownerId))
                .activeBikes(bikeRepository.countByOwnerIdAndDeletedAtIsNull(ownerId))
                .totalBookings(bookingRepository.countByOwnerIdAndDeletedAtIsNull(ownerId))
                .averageRating(reviewRepository.findAverageRatingByRevieweeId(ownerId))
                .build();
    }
}
