package com.rentmybike.notification.entity;

import com.rentmybike.booking.entity.Booking;
import com.rentmybike.common.entity.BaseEntity;
import com.rentmybike.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * An in-app notification delivered to a single user.
 * Eine In-App-Benachrichtigung, die an einen einzelnen Benutzer zugestellt wird.
 *
 * <p>Created alongside (not instead of) the transactional email sent via
 * {@code EmailService} — the email reaches the user even if they never open
 * the app, while this row drives the bell-icon badge and notification feed
 * for as long as they're logged in.
 * <p>Wird zusätzlich zur (nicht statt der) Transaktions-E-Mail erstellt, die
 * über {@code EmailService} versendet wird — die E-Mail erreicht den
 * Benutzer auch dann, wenn er die App nie öffnet, während diese Zeile das
 * Glocken-Icon-Badge und den Benachrichtigungs-Feed antreibt, solange er
 * angemeldet ist.
 *
 * <p>Extends {@link BaseEntity} (soft-delete + audit timestamps) — unlike
 * {@code Review}, notifications are mutable (read/unread) and a user may
 * eventually want to "dismiss" (soft-delete) old ones.
 * <p>Erweitert {@link BaseEntity} (Soft-Delete + Audit-Zeitstempel) — anders
 * als {@code Review} sind Benachrichtigungen veränderlich (gelesen/ungelesen),
 * und ein Benutzer möchte alte irgendwann eventuell "verwerfen" (soft-löschen).
 *
 * <p>Maps to PostgreSQL table {@code notifications}.
 * <p>Entspricht der PostgreSQL-Tabelle {@code notifications}.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    /**
     * The user this notification is for (the bike owner, today).
     * Der Benutzer, für den diese Benachrichtigung bestimmt ist (heute: der Fahrrad-Eigentümer).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    /**
     * The booking that triggered this notification, if any — lets the
     * frontend deep-link "View request" straight to it. Nullable because
     * future notification types may not be booking-related.
     * Die Buchung, die diese Benachrichtigung ausgelöst hat, falls vorhanden —
     * ermöglicht dem Frontend, "Anfrage ansehen" direkt darauf zu verlinken.
     * Nullable, da künftige Benachrichtigungstypen evtl. nicht buchungsbezogen sind.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", updatable = false)
    private Booking booking;

    /**
     * What kind of event produced this notification.
     * Welche Art von Ereignis diese Benachrichtigung erzeugt hat.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, columnDefinition = "VARCHAR(40)")
    private NotificationType type;

    /**
     * Short headline shown in the notification list/dropdown.
     * Kurze Überschrift, die in der Benachrichtigungsliste/-dropdown angezeigt wird.
     */
    @Column(nullable = false, updatable = false, length = 200)
    private String title;

    /**
     * Full notification body text.
     * Vollständiger Benachrichtigungstext.
     */
    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Null while unread; set to the read timestamp once the user opens/views it.
     * Null solange ungelesen; wird auf den Lesezeitpunkt gesetzt, sobald der
     * Benutzer sie öffnet/ansieht.
     */
    @Column
    private LocalDateTime readAt;

    /**
     * Convenience method mirroring {@code Booking}/{@code Bike} style helpers.
     * Hilfsmethode im Stil von {@code Booking}/{@code Bike}.
     */
    public boolean isRead() {
        return readAt != null;
    }

    public void markAsRead() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }
}
