package com.rentmybike.review.controller;

import com.rentmybike.common.response.ApiResponse;
import com.rentmybike.common.response.PageResponse;
import com.rentmybike.review.dto.CreateReviewRequest;
import com.rentmybike.review.dto.ReviewResponse;
import com.rentmybike.review.dto.UserRatingResponse;
import com.rentmybike.review.service.ReviewService;
import com.rentmybike.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for the review system.
 * REST-Endpunkte für das Bewertungssystem.
 *
 * <p>Public routes (no auth needed):
 * <ul>
 *   <li>GET /api/v1/reviews/bike/{bikeId}      — bike reviews + average</li>
 *   <li>GET /api/v1/reviews/user/{userId}       — user reviews + average</li>
 * </ul>
 *
 * <p>Auth-required routes:
 * <ul>
 *   <li>POST /api/v1/reviews                   — submit a review</li>
 *   <li>GET  /api/v1/reviews/booking/{bookingId} — both reviews for a booking</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // ──────────────────────────────────────────────────────────────────────────
    // Submit review / Bewertung einreichen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Submit a review for a completed booking.
     * Eine Bewertung für eine abgeschlossene Buchung einreichen.
     *
     * <p>POST /api/v1/reviews
     * <p>Auth: any authenticated user; service validates they are a booking participant.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateReviewRequest request) {

        ReviewResponse response = reviewService.createReview(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Review submitted / Bewertung eingereicht"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Bike reviews (public) / Fahrrad-Bewertungen (öffentlich)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Paginated RENTER_TO_OWNER reviews for a bike.
     * Paginierte RENTER_TO_OWNER-Bewertungen für ein Fahrrad.
     *
     * <p>GET /api/v1/reviews/bike/{bikeId}?page=0&size=10
     */
    @GetMapping("/bike/{bikeId}")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getBikeReviews(
            @PathVariable("bikeId") UUID bikeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<ReviewResponse> reviews = reviewService.getReviewsForBike(bikeId, pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    /**
     * Average rating + total count for a bike.
     * Durchschnittsbewertung + Gesamtanzahl für ein Fahrrad.
     *
     * <p>GET /api/v1/reviews/bike/{bikeId}/rating
     */
    @GetMapping("/bike/{bikeId}/rating")
    public ResponseEntity<ApiResponse<UserRatingResponse>> getBikeRating(
            @PathVariable("bikeId") UUID bikeId) {

        return ResponseEntity.ok(ApiResponse.success(reviewService.getBikeRating(bikeId)));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // User reviews (public) / Benutzer-Bewertungen (öffentlich)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Paginated reviews received by a user.
     * Paginierte Bewertungen, die ein Benutzer erhalten hat.
     *
     * <p>GET /api/v1/reviews/user/{userId}?page=0&size=10
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getUserReviews(
            @PathVariable("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<ReviewResponse> reviews = reviewService.getReviewsForUser(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    /**
     * Average rating + total count for a user.
     * Durchschnittsbewertung + Gesamtanzahl für einen Benutzer.
     *
     * <p>GET /api/v1/reviews/user/{userId}/rating
     */
    @GetMapping("/user/{userId}/rating")
    public ResponseEntity<ApiResponse<UserRatingResponse>> getUserRating(
            @PathVariable("userId") UUID userId) {

        return ResponseEntity.ok(ApiResponse.success(reviewService.getUserRating(userId)));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Booking reviews (participants only) / Buchungsbewertungen (nur Teilnehmer)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Both reviews (if any) for a specific booking.
     * Beide Bewertungen (falls vorhanden) für eine bestimmte Buchung.
     *
     * <p>GET /api/v1/reviews/booking/{bookingId}
     * <p>Auth: only the renter or owner of the booking may call this.
     */
    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getBookingReviews(
            @PathVariable("bookingId") UUID bookingId,
            @AuthenticationPrincipal User currentUser) {

        List<ReviewResponse> reviews = reviewService.getReviewsForBooking(bookingId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }
}
