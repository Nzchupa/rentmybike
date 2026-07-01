package com.rentmybike.support.entity;

import com.rentmybike.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * A user-filed support request — the ticket "header" tracked through a
 * small status workflow, with the back-and-forth conversation living in
 * {@link SupportMessage}.
 * Eine von einem Benutzer eingereichte Support-Anfrage — der Ticket-"Kopf",
 * der durch einen kleinen Status-Workflow verfolgt wird, während der
 * Nachrichtenverlauf in {@link SupportMessage} liegt.
 *
 * <p>userId/userName/userEmail are denormalized snapshots (same rationale as
 * {@code Report.reporterName}): the ticket stays fully readable even if the
 * filing account is later deleted, and userEmail gives an admin a fallback
 * contact channel outside the in-app thread.
 * <p>userId/userName/userEmail sind denormalisierte Momentaufnahmen (gleiche
 * Begründung wie bei {@code Report.reporterName}): das Ticket bleibt
 * vollständig lesbar, auch wenn das einreichende Konto später gelöscht wird,
 * und userEmail gibt einem Admin einen Kontaktkanal außerhalb des
 * In-App-Threads.
 *
 * <p>Maps to PostgreSQL table {@code support_tickets}.
 */
@Entity
@Table(name = "support_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket extends BaseEntity {

    /** The user who filed this ticket / Der Benutzer, der dieses Ticket eingereicht hat */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "user_name", nullable = false, updatable = false, length = 200)
    private String userName;

    @Column(name = "user_email", nullable = false, updatable = false, length = 255)
    private String userEmail;

    @Column(nullable = false, length = 200)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, columnDefinition = "VARCHAR(30)")
    private SupportCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    @Builder.Default
    private SupportTicketStatus status = SupportTicketStatus.OPEN;

    /**
     * True once the ticket has reached the hard-terminal CLOSED state — a
     * closed ticket no longer accepts new messages; the user must file a
     * new ticket instead.
     * True, sobald das Ticket den harten Endzustand CLOSED erreicht hat —
     * ein geschlossenes Ticket akzeptiert keine neuen Nachrichten mehr; der
     * Benutzer muss stattdessen ein neues Ticket eröffnen.
     */
    public boolean isClosed() {
        return status == SupportTicketStatus.CLOSED;
    }
}
