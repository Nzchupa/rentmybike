package com.rentmybike.review.entity;

import com.rentmybike.booking.entity.Booking;
import com.rentmybike.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A review left by one party about the other after a completed rental.
 * Eine Bewertung, die eine Partei über die andere nach einem abgeschlossenen Mietverhältnis hinterlässt.
 *
 * <p>Business rules:
 * <ul>
 *   <li>Only COMPLETED bookings can be reviewed.</li>
 *   <li>One review per booking per direction (enforced by DB UNIQUE constraint on booking_id + type).</li>
 *   <li>Renter reviews owner (RENTER_TO_OWNER) and owner reviews renter (OWNER_TO_RENTER).</li>
 *   <li>Comment is optional; rating 1-5 is required.</li>
 * </ul>
 *
 * <p>Does NOT extend BaseEntity — no soft delete needed for reviews
 * (they should be immutable once submitted).
 * <p>Erweitert BaseEntity NICHT — kein Soft-Delete für Bewertungen nötig
 * (sie sollten unveränderlich sein nach dem Einreichen).
 *
 * <p>Maps to PostgreSQL table {@code reviews}.
 * <p>Entspricht der PostgreSQL-Tabelle {@code reviews}.
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ──────────────────────────────────────────────────────────────────────────
    // Context / Kontext
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * The booking this review is associated with.
     * Die Buchung, mit der diese Bewertung verknüpft ist.
     *
     * <p>Together with {@code type}, uniquely identifies a review
     * (enforced by DB UNIQUE constraint {@code unique_review_per_booking_type}).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, updatable = false)
    private Booking booking;

    /**
     * The user who wrote this review.
     * Der Benutzer, der diese Bewertung geschrieben hat.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false, updatable = false)
    private User reviewer;

    /**
     * The user being reviewed.
     * Der Benutzer, der bewertet wird.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewee_id", nullable = false, updatable = false)
    private User reviewee;

    // ──────────────────────────────────────────────────────────────────────────
    // Content / Inhalt
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Star rating 1–5. Enforced both at DB level (CHECK) and in service.
     * Sternebewertung 1–5. Auf DB-Ebene (CHECK) und im Service erzwungen.
     */
    @Column(nullable = false)
    private int rating;

    /**
     * Optional free-text comment.
     * Optionaler Freitext-Kommentar.
     */
    @Column(columnDefinition = "TEXT")
    private String comment;

    /**
     * Direction of the review — determines who is reviewer and who is reviewee.
     * Richtung der Bewertung — bestimmt, wer Bewerter und wer Bewerteter ist.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, columnDefinition = "review_type")
    private ReviewType type;

    // ──────────────────────────────────────────────────────────────────────────
    // Timestamps (set by DB default in V1 schema)
    // Zeitstempel (durch DB-Standard in V1-Schema gesetzt)
    // ──────────────────────────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private LocalDateTime updatedAt;
}
