package com.rentmybike.chat.entity;

import com.rentmybike.booking.entity.Booking;
import com.rentmybike.common.entity.BaseEntity;
import com.rentmybike.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * A single chat message exchanged between the renter and owner of a booking.
 * Eine einzelne Chatnachricht zwischen Mieter und Eigentümer einer Buchung.
 *
 * <p>Chat is scoped to a booking — there is no general inbox between two
 * users, only a thread tied to the specific rental they're coordinating.
 * Persisted (not just broadcast) so the history survives reconnects and
 * page reloads.
 * <p>Der Chat ist an eine Buchung gebunden — es gibt keinen allgemeinen
 * Posteingang zwischen zwei Benutzern, nur einen Thread, der an die
 * konkrete Mietkoordination gebunden ist. Wird persistiert (nicht nur
 * gesendet), damit die Historie Wiederverbindungen und Seiten-Reloads
 * überlebt.
 *
 * <p>Maps to PostgreSQL table {@code chat_messages}.
 * <p>Entspricht der PostgreSQL-Tabelle {@code chat_messages}.
 */
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage extends BaseEntity {

    /**
     * The booking this message belongs to.
     * Die Buchung, zu der diese Nachricht gehört.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, updatable = false)
    private Booking booking;

    /**
     * The user who sent the message — either the renter or the owner.
     * Der Benutzer, der die Nachricht gesendet hat — Mieter oder Eigentümer.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false, updatable = false)
    private User sender;

    /**
     * The message text. Capped at 2000 chars — plenty for rental
     * coordination chat, short enough to discourage abuse.
     * Der Nachrichtentext. Begrenzt auf 2000 Zeichen — ausreichend für
     * Mietkoordinations-Chat, kurz genug, um Missbrauch zu erschweren.
     */
    @Column(name = "content", length = 2000, nullable = false, updatable = false)
    private String content;
}
