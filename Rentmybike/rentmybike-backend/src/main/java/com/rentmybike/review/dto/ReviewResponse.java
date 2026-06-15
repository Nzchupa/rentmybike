package com.rentmybike.review.dto;

import com.rentmybike.review.entity.ReviewType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for a review in API responses.
 * DTO für eine Bewertung in API-Antworten.
 */
@Data
@Builder
public class ReviewResponse {

    private UUID id;

    // Context / Kontext
    private UUID bookingId;
    private ReviewType type;

    // Reviewer info / Bewerter-Info
    private UUID reviewerId;
    private String reviewerName;
    private String reviewerAvatarUrl;

    // Reviewee info / Bewerteter-Info
    private UUID revieweeId;
    private String revieweeName;

    // Content / Inhalt
    private int rating;
    private String comment;

    private LocalDateTime createdAt;
}
