package com.rentmybike.chat.controller;

import com.rentmybike.chat.dto.ChatMessageResponse;
import com.rentmybike.chat.service.ChatService;
import com.rentmybike.common.response.ApiResponse;
import com.rentmybike.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoint for fetching chat history for a booking.
 * REST-Endpunkt zum Abrufen des Chatverlaufs einer Buchung.
 *
 * <p>Sending happens over the STOMP {@code /app/chat/{bookingId}/send}
 * destination (see {@code ChatStompController}) — this controller only
 * serves the initial history load when a chat panel opens.
 * <p>Das Senden erfolgt über das STOMP-Ziel
 * {@code /app/chat/{bookingId}/send} (siehe {@code ChatStompController}) —
 * dieser Controller bedient nur das initiale Laden des Verlaufs beim Öffnen
 * eines Chat-Panels.
 */
@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/chat")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ChatRestController {

    private final ChatService chatService;

    /**
     * GET /api/v1/bookings/{bookingId}/chat — full message history, oldest first.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getHistory(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal User currentUser) {

        List<ChatMessageResponse> history = chatService.getHistory(bookingId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
