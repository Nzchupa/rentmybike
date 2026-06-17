package com.rentmybike.notification.controller;

import com.rentmybike.common.response.ApiResponse;
import com.rentmybike.common.response.PageResponse;
import com.rentmybike.notification.dto.NotificationResponse;
import com.rentmybike.notification.dto.UnreadCountResponse;
import com.rentmybike.notification.service.NotificationService;
import com.rentmybike.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST endpoints for the in-app notification feed and bell-icon badge (Bug 5).
 * REST-Endpunkte für den In-App-Benachrichtigungs-Feed und das Glocken-Icon-Badge (Bug 5).
 *
 * <p>All routes require authentication — notifications are always scoped to
 * the calling user, never to an arbitrary user id, so there is no way to
 * read or modify someone else's notifications.
 * <p>Alle Routen erfordern Authentifizierung — Benachrichtigungen sind immer
 * auf den aufrufenden Benutzer beschränkt, nie auf eine beliebige Benutzer-ID,
 * sodass es keine Möglichkeit gibt, die Benachrichtigungen einer anderen
 * Person zu lesen oder zu ändern.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Paginated notification feed for the current user, newest first.
     * Paginierter Benachrichtigungs-Feed für den aktuellen Benutzer, neueste zuerst.
     *
     * <p>GET /api/v1/notifications?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        PageResponse<NotificationResponse> result = notificationService.getMyNotifications(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Unread notification count for the current user — polled by the bell icon.
     * Ungelesen-Zähler für den aktuellen Benutzer — wird vom Glocken-Icon abgefragt.
     *
     * <p>GET /api/v1/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal User currentUser) {

        long count = notificationService.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(UnreadCountResponse.builder().unreadCount(count).build()));
    }

    /**
     * Marks a single notification as read.
     * Markiert eine einzelne Benachrichtigung als gelesen.
     *
     * <p>POST /api/v1/notifications/{id}/read
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        notificationService.markAsRead(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Notification marked as read / Benachrichtigung als gelesen markiert"));
    }

    /**
     * Marks every unread notification for the current user as read.
     * Markiert alle ungelesenen Benachrichtigungen des aktuellen Benutzers als gelesen.
     *
     * <p>POST /api/v1/notifications/read-all
     */
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal User currentUser) {

        notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read / Alle Benachrichtigungen als gelesen markiert"));
    }
}
