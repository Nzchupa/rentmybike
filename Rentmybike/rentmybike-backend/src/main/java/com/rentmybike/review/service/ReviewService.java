package com.rentmybike.review.service;

import com.rentmybike.booking.entity.Booking;
import com.rentmybike.booking.entity.BookingStatus;
import com.rentmybike.booking.repository.BookingRepository;
import com.rentmybike.common.exception.AccessDeniedException;
import com.rentmybike.common.exception.BusinessException;
import com.rentmybike.common.exception.ResourceNotFoundException;
import com.rentmybike.common.response.PageResponse;
import com.rentmybike.review.dto.CreateReviewRequest;
import com.rentmybike.review.dto.ReviewResponse;
import com.rentmybike.review.dto.UserRatingResponse;
import com.rentmybike.review.entity.Review;
import com.rentmybike.review.entity.ReviewType;
import com.rentmybike.review.repository.ReviewRepository;
import com.rentmybike.user.entity.User;
import com.rentmybike.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for bidirectional review management.
 * Service für bidirektionales Bewertungsmanagement.
 *
 * <p>Business rules:
 * <ul>
 *   <li>Only COMPLETED bookings can be reviewed.</li>
 *   <li>RENTER_TO_OWNER: caller must be the renter of that booking.</li>
 *   <li>OWNER_TO_RENTER: caller must be the owner of that booking's bike.</li>
 *   <li>One review per booking per direction (DB UNIQUE enforces this).</li>
 *   <li>Reviews are immutable once submitted.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    // ──────────────────────────────────────────────────────────────────────────
    // CREATE / ERSTELLEN
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Submits a review for a completed booking.
     * Reicht eine Bewertung für eine abgeschlossene Buchung ein.
     *
     * @param reviewerId the authenticated user submitting the review
     *                   der authentifizierte Benutzer, der die Bewertung einreicht
     * @param request    review data including bookingId, type, rating, comment
     */
    public ReviewResponse createReview(UUID reviewerId, CreateReviewRequest request) {
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reviewerId));

        Booking booking = bookingRepository.findByIdWithDetails(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", request.getBookingId()));

        // Booking must be COMPLETED / Buchung muss COMPLETED sein
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessException(
                    "Reviews can only be left for completed bookings / Bewertungen können nur für abgeschlossene Buchungen hinterlassen werden");
        }

        // Verify reviewer role matches review type / Bewerterrolle mit Bewertungstyp abgleichen
        User reviewee = resolveRevieweeAndValidateCaller(booking, reviewer, request.getType());

        // Prevent duplicate reviews / Doppelte Bewertungen verhindern
        if (reviewRepository.existsByBookingIdAndType(booking.getId(), request.getType())) {
            throw new BusinessException(
                    "You have already reviewed this booking / Sie haben diese Buchung bereits bewertet");
        }

        Review review = Review.builder()
                .booking(booking)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .rating(request.getRating())
                .comment(request.getComment())
                .type(request.getType())
                .build();

        reviewRepository.save(review);
        log.info("Review created: type={} for booking: {} by reviewer: {} / Bewertung erstellt: Typ={} für Buchung: {} von Bewerter: {}",
                request.getType(), booking.getId(), reviewerId,
                request.getType(), booking.getId(), reviewerId);

        return toReviewResponse(review);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // READ / LESEN
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * All reviews received by a user (for their public profile).
     * Alle Bewertungen, die ein Benutzer erhalten hat (für sein öffentliches Profil).
     */
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsForUser(UUID userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        Page<Review> page = reviewRepository.findByRevieweeId(userId, pageable);
        return PageResponse.from(page.map(this::toReviewResponse));
    }

    /**
     * Average rating + count for a user.
     * Durchschnittsbewertung + Anzahl für einen Benutzer.
     */
    @Transactional(readOnly = true)
    public UserRatingResponse getUserRating(UUID userId) {
        double avg = reviewRepository.findAverageRatingByRevieweeId(userId);
        long count = reviewRepository.countByRevieweeId(userId);
        return UserRatingResponse.builder()
                .averageRating(Math.round(avg * 10.0) / 10.0) // round to 1 decimal
                .reviewCount(count)
                .build();
    }

    /**
     * All RENTER_TO_OWNER reviews for a specific bike (public bike detail page).
     * Alle RENTER_TO_OWNER-Bewertungen für ein bestimmtes Fahrrad (öffentliche Fahrrad-Detailseite).
     */
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsForBike(UUID bikeId, Pageable pageable) {
        Page<Review> page = reviewRepository.findByBikeId(bikeId, pageable);
        return PageResponse.from(page.map(this::toReviewResponse));
    }

    /**
     * Average rating + count for a bike.
     * Durchschnittsbewertung + Anzahl für ein Fahrrad.
     */
    @Transactional(readOnly = true)
    public UserRatingResponse getBikeRating(UUID bikeId) {
        double avg = reviewRepository.findAverageRatingByBikeId(bikeId);
        long count = reviewRepository.findByBikeId(bikeId, Pageable.unpaged()).getTotalElements();
        return UserRatingResponse.builder()
                .averageRating(Math.round(avg * 10.0) / 10.0)
                .reviewCount(count)
                .build();
    }

    /**
     * Both reviews for a specific booking (shown on booking detail page).
     * Beide Bewertungen für eine bestimmte Buchung (auf Buchungsdetailseite angezeigt).
     */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForBooking(UUID bookingId, UUID requesterId) {
        // Verify requester is a participant / Anfragenden als Teilnehmer verifizieren
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        boolean isParticipant = booking.getRenter().getId().equals(requesterId)
                || booking.getOwner().getId().equals(requesterId);
        if (!isParticipant) {
            throw new AccessDeniedException("Access denied / Zugriff verweigert");
        }

        return reviewRepository.findByBookingIdWithDetails(bookingId)
                .stream()
                .map(this::toReviewResponse)
                .toList();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Validates the caller's role matches the review type and returns the reviewee.
     * Validiert, dass die Aufruferrolle dem Bewertungstyp entspricht und gibt den Bewerteten zurück.
     *
     * <p>RENTER_TO_OWNER → caller must be the renter, reviewee = owner.
     * <p>OWNER_TO_RENTER → caller must be the owner, reviewee = renter.
     */
    private User resolveRevieweeAndValidateCaller(Booking booking, User caller, ReviewType type) {
        return switch (type) {
            case RENTER_TO_OWNER -> {
                if (!booking.getRenter().getId().equals(caller.getId())) {
                    throw new AccessDeniedException(
                            "Only the renter can submit a RENTER_TO_OWNER review / Nur der Mieter kann eine RENTER_TO_OWNER-Bewertung einreichen");
                }
                yield booking.getOwner();
            }
            case OWNER_TO_RENTER -> {
                if (!booking.getOwner().getId().equals(caller.getId())) {
                    throw new AccessDeniedException(
                            "Only the owner can submit an OWNER_TO_RENTER review / Nur der Eigentümer kann eine OWNER_TO_RENTER-Bewertung einreichen");
                }
                yield booking.getRenter();
            }
        };
    }

    /**
     * Maps a Review entity to a ReviewResponse DTO.
     * Mappt eine Review-Entity auf ein ReviewResponse-DTO.
     */
    private ReviewResponse toReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .bookingId(review.getBooking().getId())
                .type(review.getType())
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getFullName())
                .reviewerAvatarUrl(review.getReviewer().getAvatarUrl())
                .revieweeId(review.getReviewee().getId())
                .revieweeName(review.getReviewee().getFullName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
