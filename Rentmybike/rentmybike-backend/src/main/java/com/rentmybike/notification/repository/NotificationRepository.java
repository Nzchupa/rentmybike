package com.rentmybike.notification.repository;

import com.rentmybike.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Repository for in-app notification queries.
 * Repository für In-App-Benachrichtigungsabfragen.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // ──────────────────────────────────────────────────────────────────────────
    // List for a user / Liste für einen Benutzer
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Paginated notifications for a user, newest first, excluding soft-deleted.
     * Paginierte Benachrichtigungen für einen Benutzer, neueste zuerst, ohne soft-gelöschte.
     *
     * <p>Explicit {@code countQuery} avoids the classic Hibernate "fetch join +
     * Pageable paginates the whole result set in memory" pitfall — same pattern
     * already used by {@code BookingRepository.findByOwnerIdAndStatus}.
     * <p>Expliziter {@code countQuery} vermeidet die klassische Hibernate-Falle
     * "Fetch-Join + Pageable paginiert das gesamte Ergebnis im Speicher" —
     * gleiches Muster wie bereits in {@code BookingRepository.findByOwnerIdAndStatus}.
     */
    @Query(value = """
            SELECT n FROM Notification n
            JOIN FETCH n.user
            LEFT JOIN FETCH n.booking b
            LEFT JOIN FETCH b.bike
            WHERE n.user.id = :userId
              AND n.deletedAt IS NULL
            ORDER BY n.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(n) FROM Notification n
            WHERE n.user.id = :userId
              AND n.deletedAt IS NULL
            """)
    Page<Notification> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    // ──────────────────────────────────────────────────────────────────────────
    // Unread count (bell badge) / Ungelesen-Zähler (Glocken-Badge)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Count of unread, non-deleted notifications for a user — backs the bell
     * icon's badge number.
     * Anzahl ungelesener, nicht gelöschter Benachrichtigungen für einen
     * Benutzer — liefert die Zahl im Glocken-Icon-Badge.
     */
    long countByUserIdAndReadAtIsNullAndDeletedAtIsNull(UUID userId);

    // ──────────────────────────────────────────────────────────────────────────
    // Mark as read / Als gelesen markieren
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Marks a single notification as read, scoped to its owner so one user
     * cannot mark another user's notification as read by guessing an id.
     * Markiert eine einzelne Benachrichtigung als gelesen, beschränkt auf
     * ihren Eigentümer, damit ein Benutzer nicht die Benachrichtigung eines
     * anderen durch Erraten einer ID als gelesen markieren kann.
     */
    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.readAt = :readAt
            WHERE n.id = :id AND n.user.id = :userId AND n.readAt IS NULL
            """)
    int markAsRead(@Param("id") UUID id, @Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt);

    /**
     * Marks every unread notification for a user as read ("mark all as read").
     * Markiert alle ungelesenen Benachrichtigungen eines Benutzers als gelesen
     * ("alle als gelesen markieren").
     */
    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.readAt = :readAt
            WHERE n.user.id = :userId AND n.readAt IS NULL
            """)
    int markAllAsRead(@Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt);
}
