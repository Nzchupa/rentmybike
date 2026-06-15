package com.rentmybike.review.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Summary of ratings received by a user or for a bike.
 * Zusammenfassung der Bewertungen, die ein Benutzer oder ein Fahrrad erhalten hat.
 *
 * <p>Used as a header above the review list on profile and bike detail pages.
 * <p>Wird als Kopfzeile über der Bewertungsliste auf Profil- und Fahrrad-Detailseiten verwendet.
 */
@Data
@Builder
public class UserRatingResponse {

    /** Average star rating (1.0–5.0), 0.0 if no reviews yet / Durchschnittliche Sternebewertung, 0.0 wenn noch keine Bewertungen */
    private double averageRating;

    /** Total number of reviews received / Gesamtanzahl der erhaltenen Bewertungen */
    private long reviewCount;
}
