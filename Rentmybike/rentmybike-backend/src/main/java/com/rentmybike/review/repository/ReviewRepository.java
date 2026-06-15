package com.rentmybike.review.repository;

import com.rentmybike.review.entity.Review;
import com.rentmybike.review.entity.ReviewType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for review queries.
 * Repository für Bewertungsabfragen.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    // ──────────────────────────────────────────────────────────────────────────
    // Duplicate check / Duplikatprüfung
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Checks if a review of a given type already exists for a booking.
     * Prüft, ob eine Bewertung eines bestimmten Typs für eine Buchung bereits existiert.
     *
     * <p>Mirrors the DB UNIQUE constraint {@code unique_review_per_booking_type}.
     * <p>Spiegelt die DB-UNIQUE-Einschränkung {@code unique_review_per_booking_type}.
     */
    boolean existsByBookingIdAndType(UUID bookingId, ReviewType type);

    // ──────────────────────────────────────────────────────────────────────────
    // Reviews for a booking / Bewertungen für eine Buchung
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Both reviews for a single booking (at most 2: RENTER_TO_OWNER + OWNER_TO_RENTER).
     * Beide Bewertungen für eine einzelne Buchung (höchstens 2).
     */
    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.reviewer
            JOIN FETCH r.reviewee
            WHERE r.booking.id = :bookingId
            ORDER BY r.createdAt ASC
            """)
    List<Review> findByBookingIdWithDetails(@Param("bookingId") UUID bookingId);

    /**
     * Find a specific review by booking + type (used to check if already reviewed).
     * Bestimmte Bewertung nach Buchung + Typ finden (prüft ob bereits bewertet).
     */
    Optional<Review> findByBookingIdAndType(UUID bookingId, ReviewType type);

    // ──────────────────────────────────────────────────────────────────────────
    // Reviews about a user (their profile rating) / Bewertungen über einen Benutzer
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * All reviews received by a user — used for their public profile.
     * Alle Bewertungen, die ein Benutzer erhalten hat — für sein öffentliches Profil.
     *
     * <p>Uses index {@code idx_reviews_reviewee_id}.
     */
    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.reviewer
            JOIN FETCH r.booking b
            JOIN FETCH b.bike
            WHERE r.reviewee.id = :userId
            ORDER BY r.createdAt DESC
            """)
    Page<Review> findByRevieweeId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Average rating received by a user (for profile display).
     * Durchschnittsbewertung eines Benutzers (für Profilanzeige).
     */
    @Query("""
            SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r
            WHERE r.reviewee.id = :userId
            """)
    Double findAverageRatingByRevieweeId(@Param("userId") UUID userId);

    /**
     * Count of reviews received by a user.
     * Anzahl der Bewertungen, die ein Benutzer erhalten hat.
     */
    long countByRevieweeId(UUID userId);

    // ──────────────────────────────────────────────────────────────────────────
    // Reviews for a bike (via owner) / Bewertungen für ein Fahrrad (via Eigentümer)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * All RENTER_TO_OWNER reviews for a specific bike.
     * Alle RENTER_TO_OWNER-Bewertungen für ein bestimmtes Fahrrad.
     *
     * <p>Joins through booking → bike to reach the target bike.
     * <p>Joinen über Buchung → Fahrrad zum Zielen des Fahrrads.
     */
    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.reviewer
            JOIN FETCH r.booking b
            WHERE b.bike.id = :bikeId
              AND r.type = com.rentmybike.review.entity.ReviewType.RENTER_TO_OWNER
            ORDER BY r.createdAt DESC
            """)
    Page<Review> findByBikeId(@Param("bikeId") UUID bikeId, Pageable pageable);

    /**
     * Average rating for a specific bike (from RENTER_TO_OWNER reviews).
     * Durchschnittsbewertung für ein bestimmtes Fahrrad (aus RENTER_TO_OWNER-Bewertungen).
     */
    @Query("""
            SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r
            JOIN r.booking b
            WHERE b.bike.id = :bikeId
              AND r.type = com.rentmybike.review.entity.ReviewType.RENTER_TO_OWNER
            """)
    Double findAverageRatingByBikeId(@Param("bikeId") UUID bikeId);
}
