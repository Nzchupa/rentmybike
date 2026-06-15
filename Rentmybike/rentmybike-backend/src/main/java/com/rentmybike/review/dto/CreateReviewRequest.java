package com.rentmybike.review.dto;

import com.rentmybike.review.entity.ReviewType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

/**
 * DTO for submitting a review — POST /api/v1/reviews.
 * DTO zum Einreichen einer Bewertung — POST /api/v1/reviews.
 *
 * <p>Preconditions (validated in service):
 * <ul>
 *   <li>Booking must be COMPLETED.</li>
 *   <li>Caller must be the appropriate participant (renter or owner).</li>
 *   <li>No review of this type already exists for the booking.</li>
 * </ul>
 */
@Data
public class CreateReviewRequest {

    @NotNull(message = "Booking ID is required / Buchungs-ID ist erforderlich")
    private UUID bookingId;

    /**
     * Direction of the review:
     * RENTER_TO_OWNER — renter reviews the owner/bike.
     * OWNER_TO_RENTER — owner reviews the renter.
     */
    @NotNull(message = "Review type is required / Bewertungstyp ist erforderlich")
    private ReviewType type;

    /**
     * Star rating 1–5.
     * Sternebewertung 1–5.
     */
    @NotNull(message = "Rating is required / Bewertung ist erforderlich")
    @Min(value = 1, message = "Minimum rating is 1 / Mindestbewertung ist 1")
    @Max(value = 5, message = "Maximum rating is 5 / Höchstbewertung ist 5")
    private Integer rating;

    /**
     * Optional free-text comment (max 2000 characters).
     * Optionaler Freitext-Kommentar (max. 2000 Zeichen).
     */
    @Size(max = 2000, message = "Comment too long (max 2000 chars) / Kommentar zu lang (max. 2000 Zeichen)")
    private String comment;
}
