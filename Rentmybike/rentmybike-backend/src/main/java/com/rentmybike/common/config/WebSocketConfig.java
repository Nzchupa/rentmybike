package com.rentmybike.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket configuration for real-time per-booking chat.
 * STOMP-über-WebSocket-Konfiguration für Echtzeit-Chat pro Buchung.
 *
 * <p>Authentication for the {@code /ws} handshake is handled entirely by the
 * existing {@code JwtAuthenticationFilter}, which runs on every HTTP request
 * — including this SockJS/WebSocket upgrade request — and populates
 * {@code SecurityContextHolder}. Spring Security's
 * {@code SecurityContextHolderAwareRequestWrapper} then exposes that as
 * {@code request.getUserPrincipal()}, which Spring's
 * {@code DefaultHandshakeHandler} automatically adopts as the STOMP
 * session's {@code Principal} — no custom {@code HandshakeInterceptor} or
 * separate WS JWT check is required.
 * <p>Die Authentifizierung für den {@code /ws}-Handshake wird vollständig
 * vom bestehenden {@code JwtAuthenticationFilter} übernommen, der bei jeder
 * HTTP-Anfrage läuft — einschließlich dieser SockJS/WebSocket-Upgrade-
 * Anfrage — und {@code SecurityContextHolder} befüllt. Spring Securitys
 * {@code SecurityContextHolderAwareRequestWrapper} stellt dies dann als
 * {@code request.getUserPrincipal()} bereit, was Springs
 * {@code DefaultHandshakeHandler} automatisch als {@code Principal} der
 * STOMP-Sitzung übernimmt — kein benutzerdefinierter
 * {@code HandshakeInterceptor} oder separate WS-JWT-Prüfung erforderlich.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CorsConfig corsConfig;
    private final ChatChannelInterceptor chatChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // Reuse the exact same allowlist as the REST CORS config so the
                // two never drift out of sync.
                // Verwendet exakt dieselbe Allowlist wie die REST-CORS-Konfiguration,
                // damit beide nie auseinanderlaufen.
                .setAllowedOriginPatterns(corsConfig.allowedOriginPatterns().toArray(new String[0]))
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple in-memory broker — sufficient for a single Railway instance;
        // would need an external broker relay (e.g. RabbitMQ STOMP plugin) to
        // scale to multiple backend instances.
        // Einfacher In-Memory-Broker — ausreichend für eine einzelne Railway-
        // Instanz; würde einen externen Broker-Relay (z. B. RabbitMQ-STOMP-
        // Plugin) benötigen, um auf mehrere Backend-Instanzen zu skalieren.
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(chatChannelInterceptor);
    }
}
