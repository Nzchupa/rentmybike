package com.rentmybike.support.entity;

import com.rentmybike.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * A single message in a {@link SupportTicket}'s thread — from either the
 * filing user or an admin.
 * Eine einzelne Nachricht im Thread eines {@link SupportTicket} — entweder
 * vom einreichenden Benutzer oder von einem Admin.
 *
 * <p>senderId/senderName are denormalized the same way as
 * {@code SupportTicket.userName} — a message stays attributable even if the
 * sender's account is later deleted. {@code fromAdmin} drives which side of
 * the thread bubble a message renders on in the frontend.
 * <p>senderId/senderName sind analog zu {@code SupportTicket.userName}
 * denormalisiert — eine Nachricht bleibt zuordenbar, auch wenn das
 * Sender-Konto später gelöscht wird. {@code fromAdmin} bestimmt im Frontend,
 * auf welcher Seite der Nachricht-Sprechblase eine Nachricht dargestellt wird.
 *
 * <p>Maps to PostgreSQL table {@code support_messages}.
 */
@Entity
@Table(name = "support_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private SupportTicket ticket;

    @Column(name = "sender_id", nullable = false, updatable = false)
    private UUID senderId;

    @Column(name = "sender_name", nullable = false, updatable = false, length = 200)
    private String senderName;

    @Column(name = "from_admin", nullable = false, updatable = false)
    @Builder.Default
    private boolean fromAdmin = false;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String body;
}
