package com.rentmybike.chat.controller;

import com.rentmybike.chat.dto.SendChatMessageRequest;
import com.rentmybike.chat.service.ChatService;
import com.rentmybike.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * STOMP message handler for sending chat messages over the WebSocket
 * connection established at {@code /ws}.
 * STOMP-Nachrichten-Handler zum Senden von Chatnachrichten über die unter
 * {@code /ws} aufgebaute WebSocket-Verbindung.
 *
 * <p>Clients send to the app destination {@code /app/chat/{bookingId}/send};
 * this handler persists the message via {@link ChatService} (which also
 * broadcasts it to {@code /topic/booking/{bookingId}}), so there's no
 * separate {@code @SendTo} — the destination needs the runtime booking ID,
 * which {@code @SendTo}'s static value can't express.
 * <p>Clients senden an das App-Ziel {@code /app/chat/{bookingId}/send};
 * dieser Handler persistiert die Nachricht über {@link ChatService}
 * (welcher sie auch an {@code /topic/booking/{bookingId}} sendet), daher
 * gibt es kein separates {@code @SendTo} — das Ziel benötigt die
 * Laufzeit-Buchungs-ID, die der statische Wert von {@code @SendTo} nicht
 * ausdrücken kann.
 *
 * <p>{@code principal} here is the {@code Authentication} object itself
 * (Spring Security's {@code SecurityContextHolderAwareRequestWrapper}
 * exposes {@code getUserPrincipal()} as the Authentication, not the bare
 * {@code User}), populated during the STOMP/SockJS HTTP handshake by the
 * existing {@code JwtAuthenticationFilter} — no separate WS-specific JWT
 * check is needed.
 * <p>{@code principal} ist hier das {@code Authentication}-Objekt selbst
 * (Spring Securitys {@code SecurityContextHolderAwareRequestWrapper} stellt
 * {@code getUserPrincipal()} als die Authentication bereit, nicht den
 * bloßen {@code User}), das während des STOMP-/SockJS-HTTP-Handshakes vom
 * bestehenden {@code JwtAuthenticationFilter} gesetzt wird — keine separate
 * WS-spezifische JWT-Prüfung nötig.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatStompController {

    private final ChatService chatService;

    @MessageMapping("/chat/{bookingId}/send")
    public void send(@DestinationVariable UUID bookingId,
                      @Valid SendChatMessageRequest request,
                      Principal principal) {

        UUID senderId = resolveUserId(principal);
        chatService.sendMessage(bookingId, senderId, request.getContent());
    }

    /**
     * Extracts the authenticated user's ID from the STOMP session principal.
     * Extrahiert die Benutzer-ID des authentifizierten Benutzers aus dem
     * STOMP-Sitzungs-Principal.
     */
    static UUID resolveUserId(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        log.warn("STOMP message received without a resolvable User principal: {}", principal);
        throw new org.springframework.security.access.AccessDeniedException(
                "Not authenticated / Nicht authentifiziert");
    }
}
