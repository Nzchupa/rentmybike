package com.rentmybike.common.config;

import com.rentmybike.chat.service.ChatService;
import com.rentmybike.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enforces booking-participant access control on STOMP SUBSCRIBE frames.
 * Erzwingt Buchungs-Teilnehmer-Zugriffskontrolle auf STOMP-SUBSCRIBE-Frames.
 *
 * <p>Sending is already gated inside {@code ChatService.sendMessage} (via
 * {@code assertParticipant}) because that's a normal authenticated service
 * call. Subscribing, however, is a pure messaging-broker operation — Spring
 * happily lets any connected client subscribe to any {@code /topic/**}
 * destination unless something stops it. Without this interceptor, any
 * authenticated user could subscribe to {@code /topic/booking/{anyId}} and
 * silently read another pair's rental chat. This interceptor closes that
 * gap by checking booking participation on every SUBSCRIBE to
 * {@code /topic/booking/{bookingId}}.
 * <p>Das Senden wird bereits in {@code ChatService.sendMessage} (via
 * {@code assertParticipant}) abgesichert, da es sich um einen normalen
 * authentifizierten Service-Aufruf handelt. Das Abonnieren hingegen ist eine
 * reine Messaging-Broker-Operation — Spring lässt jeden verbundenen Client
 * ohne Gegenmaßnahme jedes {@code /topic/**}-Ziel abonnieren. Ohne diesen
 * Interceptor könnte jeder authentifizierte Benutzer
 * {@code /topic/booking/{beliebigeId}} abonnieren und den Mietchat eines
 * anderen Paares unbemerkt mitlesen. Dieser Interceptor schließt diese
 * Lücke, indem er bei jedem SUBSCRIBE auf
 * {@code /topic/booking/{bookingId}} die Buchungsteilnahme prüft.
 */
@Component
@Slf4j
public class ChatChannelInterceptor implements ChannelInterceptor {

    private static final Pattern BOOKING_TOPIC = Pattern.compile("^/topic/booking/([0-9a-fA-F-]{36})$");

    private final ChatService chatService;

    /**
     * {@code @Lazy} breaks a circular bean dependency: {@code ChatService}
     * depends on {@code SimpMessagingTemplate}, whose creation is driven by
     * {@code DelegatingWebSocketMessageBrokerConfiguration}, which must first
     * construct {@code WebSocketConfig} (a {@code WebSocketMessageBrokerConfigurer}),
     * which depends on this interceptor. Without {@code @Lazy} here, Spring
     * needs a fully-built {@code ChatService} to build this interceptor,
     * which is needed to build {@code WebSocketConfig}, which is needed to
     * finish building {@code ChatService} — an unresolvable cycle that fails
     * application startup. The lazy proxy defers the real {@code ChatService}
     * lookup until {@code isParticipant(...)} is actually invoked at runtime,
     * well after the context has finished refreshing.
     * <p>{@code @Lazy} durchbricht eine zirkuläre Bean-Abhängigkeit:
     * {@code ChatService} benötigt {@code SimpMessagingTemplate}, deren
     * Erstellung von {@code DelegatingWebSocketMessageBrokerConfiguration}
     * gesteuert wird, die zunächst {@code WebSocketConfig} (einen
     * {@code WebSocketMessageBrokerConfigurer}) erstellen muss, der von
     * diesem Interceptor abhängt. Ohne {@code @Lazy} bräuchte Spring einen
     * vollständig erstellten {@code ChatService}, um diesen Interceptor zu
     * bauen, der für {@code WebSocketConfig} benötigt wird, der wiederum für
     * die Fertigstellung von {@code ChatService} nötig ist — ein
     * unauflösbarer Zyklus, der den Start der Anwendung scheitern lässt. Der
     * Lazy-Proxy verzögert die tatsächliche {@code ChatService}-Auflösung,
     * bis {@code isParticipant(...)} zur Laufzeit aufgerufen wird, lange
     * nachdem der Kontext fertig aktualisiert wurde.
     */
    public ChatChannelInterceptor(@Lazy ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        SimpMessageHeaderAccessor accessor =
                SimpMessageHeaderAccessor.wrap(message);

        if (accessor.getMessageType() == SimpMessageType.SUBSCRIBE) {
            String destination = accessor.getDestination();
            Matcher matcher = destination != null ? BOOKING_TOPIC.matcher(destination) : null;

            if (matcher != null && matcher.matches()) {
                UUID bookingId = UUID.fromString(matcher.group(1));
                UUID userId = resolveUserId(accessor.getUser());

                if (userId == null || !chatService.isParticipant(bookingId, userId)) {
                    log.warn("Rejected SUBSCRIBE to {} — user {} is not a participant / " +
                             "SUBSCRIBE auf {} abgelehnt — Benutzer {} ist kein Teilnehmer",
                            destination, userId, destination, userId);
                    throw new AccessDeniedException(
                            "Not a participant in this booking / Kein Teilnehmer dieser Buchung");
                }
            }
        }

        return message;
    }

    private UUID resolveUserId(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }
}
