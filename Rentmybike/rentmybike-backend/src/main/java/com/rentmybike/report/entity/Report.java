package com.rentmybike.report.entity;

import com.rentmybike.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A user-filed complaint about a bike listing, another user, or a review —
 * triaged and resolved by admins in the moderation center.
 * Eine von einem Benutzer eingereichte Beschwerde über ein Fahrrad-Inserat,
 * einen anderen Benutzer oder eine Bewertung — wird von Admins im
 * Moderationszentrum bearbeitet und gelöst.
 *
 * <p>Extends {@link BaseEntity} (unlike the immutable {@code AuditLog}):
 * reports are mutable — status, resolution note, and resolver fields are
 * all filled in / changed after creation as an admin works the report.
 * <p>Erweitert {@link BaseEntity} (im Gegensatz zum unveränderlichen
 * {@code AuditLog}): Meldungen sind veränderlich — Status, Auflösungsnotiz
 * und Bearbeiterfelder werden nach der Erstellung ausgefüllt/geändert,
 * während ein Admin die Meldung bearbeitet.
 *
 * <p>Maps to PostgreSQL table {@code reports}.
 */
@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report extends BaseEntity {

    /** The user who filed this report / Der Benutzer, der diese Meldung eingereicht hat */
    @Column(name = "reporter_id", nullable = false, updatable = false)
    private UUID reporterId;

    /**
     * Denormalized snapshot of the reporter's name at filing time — survives
     * the reporter account later being deleted.
     * Denormalisierte Momentaufnahme des Namens des Meldenden zum
     * Einreichungszeitpunkt — bleibt erhalten, falls das Konto später
     * gelöscht wird.
     */
    @Column(name = "reporter_name", nullable = false, updatable = false, length = 200)
    private String reporterName;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, updatable = false, columnDefinition = "VARCHAR(20)")
    private ReportTargetType targetType;

    /** The id of the bike/user/review being reported, per {@link #targetType} */
    @Column(name = "target_id", nullable = false, updatable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, columnDefinition = "VARCHAR(40)")
    private ReportReason reason;

    /** Optional free-text elaboration from the reporter / Optionale Freitext-Erläuterung des Meldenden */
    @Column(columnDefinition = "TEXT")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    /** Admin's note explaining how the report was resolved/dismissed / Notiz des Admins zur Lösung/Ablehnung */
    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    /** The admin who resolved/dismissed this report — null while PENDING/UNDER_REVIEW */
    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_by_name", length = 200)
    private String resolvedByName;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Convenience method: true once the report has reached a terminal state.
     * Hilfsmethode: true, sobald die Meldung einen Endzustand erreicht hat.
     */
    public boolean isClosed() {
        return status == ReportStatus.RESOLVED || status == ReportStatus.DISMISSED;
    }
}
