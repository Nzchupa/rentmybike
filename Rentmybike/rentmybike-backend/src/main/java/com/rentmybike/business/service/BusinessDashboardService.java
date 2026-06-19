package com.rentmybike.business.service;

import com.rentmybike.bike.repository.BikeRepository;
import com.rentmybike.bike.repository.PopularBikeProjection;
import com.rentmybike.booking.dto.BookingResponse;
import com.rentmybike.booking.entity.BookingStatus;
import com.rentmybike.booking.repository.BookingRepository;
import com.rentmybike.booking.service.BookingService;
import com.rentmybike.business.dto.BusinessAnalyticsResponse;
import com.rentmybike.business.dto.BusinessDashboardSummaryResponse;
import com.rentmybike.business.dto.BusinessOverviewExtrasResponse;
import com.rentmybike.business.dto.BusinessTimeSeriesPoint;
import com.rentmybike.business.dto.PopularBikeResponse;
import com.rentmybike.common.projection.DailyAmountProjection;
import com.rentmybike.common.projection.DailyCountProjection;
import com.rentmybike.review.dto.ReviewResponse;
import com.rentmybike.review.repository.ReviewRepository;
import com.rentmybike.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final BookingService bookingService;
    private final ReviewService reviewService;

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

    /**
     * Builds the richer analytics payload for a business owner: a zero-filled
     * daily booking/revenue series, the top 5 bikes by booking count, average
     * completed-rental duration, and a views-to-bookings conversion rate.
     * Mirrors {@code AdminService.getAnalytics}'s gap-filling approach, scoped
     * to a single owner.
     * Erstellt das umfangreichere Analytics-Payload für einen
     * Business-Eigentümer: eine nullaufgefüllte tägliche
     * Buchungs-/Umsatzreihe, die Top-5-Fahrräder nach Buchungsanzahl,
     * durchschnittliche Mietdauer abgeschlossener Buchungen und eine
     * Konversionsrate von Aufrufen zu Buchungen. Entspricht dem
     * Lückenfüllungs-Ansatz von {@code AdminService.getAnalytics}, beschränkt
     * auf einen einzelnen Eigentümer.
     *
     * @param days trailing window size, e.g. 7/30/90 — callers should clamp this / Größe des zurückliegenden Fensters — Aufrufer sollten dies begrenzen
     */
    public BusinessAnalyticsResponse getAnalytics(UUID ownerId, int days) {
        LocalDate today = LocalDate.now();
        LocalDate rangeStart = today.minusDays(days - 1L);
        LocalDateTime from = rangeStart.atStartOfDay();

        Map<LocalDate, Long> bookingsByDay = toCountMap(bookingRepository.countDailyBookingsByOwnerSince(ownerId, from));
        Map<LocalDate, BigDecimal> revenueByDay = toAmountMap(bookingRepository.sumDailyRevenueByOwnerSince(ownerId, from));

        List<BusinessTimeSeriesPoint> series = rangeStart.datesUntil(today.plusDays(1))
                .map(day -> BusinessTimeSeriesPoint.builder()
                        .date(day)
                        .newBookings(bookingsByDay.getOrDefault(day, 0L))
                        .revenue(revenueByDay.getOrDefault(day, BigDecimal.ZERO))
                        .build())
                .toList();

        List<PopularBikeResponse> popularBikes = bikeRepository.findPopularBikesByOwnerId(ownerId, PageRequest.of(0, 5))
                .stream()
                .map(this::toPopularBikeResponse)
                .toList();

        long totalBookings = bookingRepository.countByOwnerIdAndDeletedAtIsNull(ownerId);
        long totalViews = bikeRepository.sumViewCountByOwnerId(ownerId);
        // Guard divide-by-zero — a brand-new business with no bike views yet
        // has nothing to convert, not an infinite/undefined rate.
        // Division durch Null verhindern — ein neues Geschäftskonto ohne
        // Fahrrad-Aufrufe hat nichts zu konvertieren, keine
        // unendliche/undefinierte Rate.
        double conversionRate = totalViews == 0 ? 0.0 : (totalBookings * 100.0) / totalViews;

        return BusinessAnalyticsResponse.builder()
                .rangeDays(days)
                .series(series)
                .popularBikes(popularBikes)
                .averageBookingDurationDays(bookingRepository.avgCompletedRentalDaysByOwnerId(ownerId))
                .conversionRate(conversionRate)
                .build();
    }

    private PopularBikeResponse toPopularBikeResponse(PopularBikeProjection row) {
        return PopularBikeResponse.builder()
                .bikeId(row.getBikeId())
                .title(row.getTitle())
                .viewCount(row.getViewCount())
                .bookingCount(row.getBookingCount())
                .revenue(row.getRevenue())
                .build();
    }

    private Map<LocalDate, Long> toCountMap(List<DailyCountProjection> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        for (DailyCountProjection row : rows) {
            map.put(row.getDay(), row.getCount());
        }
        return map;
    }

    private Map<LocalDate, BigDecimal> toAmountMap(List<DailyAmountProjection> rows) {
        Map<LocalDate, BigDecimal> map = new HashMap<>();
        for (DailyAmountProjection row : rows) {
            map.put(row.getDay(), row.getAmount());
        }
        return map;
    }

    /**
     * Builds the business overview's supplementary "at a glance" lists:
     * bookings awaiting accept/reject, the soonest upcoming confirmed
     * bookings, and the most recently received reviews. Delegates to the
     * existing BookingService/ReviewService methods (already used by other
     * endpoints) instead of duplicating their entity-to-DTO mapping here.
     * Erstellt die ergänzenden "auf einen Blick"-Listen der
     * Business-Übersicht: Buchungen, die auf Annahme/Ablehnung warten, die
     * nächsten anstehenden bestätigten Buchungen und die zuletzt erhaltenen
     * Bewertungen. Delegiert an die bestehenden
     * BookingService-/ReviewService-Methoden (bereits von anderen
     * Endpunkten verwendet), statt deren Entity-zu-DTO-Mapping hier zu
     * duplizieren.
     */
    public BusinessOverviewExtrasResponse getOverviewExtras(UUID ownerId) {
        // findByOwnerIdAndStatus already has its own ORDER BY createdAt DESC —
        // unsorted Pageable, same Pageable/Sort pitfall as elsewhere in this codebase.
        List<BookingResponse> pendingActionBookings =
                bookingService.getOwnerBookings(ownerId, BookingStatus.PENDING, PageRequest.of(0, 5)).getContent();

        List<BookingResponse> upcomingBookings =
                bookingService.getOwnerUpcomingBookings(ownerId, 5);

        // findByRevieweeId already has its own ORDER BY createdAt DESC — unsorted Pageable.
        List<ReviewResponse> recentReviews =
                reviewService.getReviewsForUser(ownerId, PageRequest.of(0, 5)).getContent();

        return BusinessOverviewExtrasResponse.builder()
                .pendingActionBookings(pendingActionBookings)
                .upcomingBookings(upcomingBookings)
                .recentReviews(recentReviews)
                .build();
    }
}
