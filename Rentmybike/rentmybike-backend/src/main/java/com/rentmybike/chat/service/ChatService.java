package com.rentmybike.chat.service;

import com.rentmybike.booking.entity.Booking;
import com.rentmybike.booking.repository.BookingRepository;
import com.rentmybike.chat.dto.ChatMessageResponse;
import com.rentmybike.chat.entity.ChatMessage;
import com.rentmybike.chat.repository.ChatMessageRepository;
import com.rentmybike.common.exception.AccessDeniedException;
import com.rentmybike.common.exception.ResourceNotFoundException;
import com.rentmybike.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for real-time per-booking chat between renter and owner.
 * Service für Echtzeit-Chat pro Buchung zwischen Mieter und Eigentümer.
 *
 * <p>Messages are persisted (so history survives reconnects/reloads) and then
 * broadcast over STOMP to {@code /topic/booking/{bookingId}}. Both upload and
 * subscribe access mirror the {@code assertParticipant} permission pattern
 * already used in {@code BookingPhotoService}: only the renter or owner of a
 * given booking may read or write its chat thread.
 * <p>Nachrichten werden persistiert (damit die Historie Wiederverbindungen/
 * Reloads überlebt) und dann per STOMP an {@code /topic/booking/{bookingId}}
 * gesendet. Sowohl Schreib- als auch Lesezugriff spiegeln das bereits in
 * {@code BookingPhotoService} verwendete {@code assertParticipant}-
 * Berechtigungsmuster: Nur der Mieter oder Eigentümer einer bestimmten
 * Buchung darf ihren Chat-Thread lesen oder schreiben.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Persists a chat message and broadcasts it to everyone subscribed to
     * this booking's chat topic.
     * Persistiert eine Chatnachricht und sendet sie an alle Abonnenten des
     * Chat-Themas dieser Buchung.
     */
    public ChatMessageResponse sendMessage(UUID bookingId, UUID senderId, String content) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        assertParticipant(booking, senderId);

        ChatMessage message = ChatMessage.builder()
                .booking(booking)
                .sender(userRepository.getReferenceById(senderId))
                .content(content)
                .build();

        message = chatMessageRepository.save(message);
        log.info("Chat message sent for booking {} by user {} / " +
                 "Chatnachricht für Buchung {} von Benutzer {} gesendet",
                bookingId, senderId, bookingId, senderId);

        ChatMessageResponse response = toResponse(message);
        messagingTemplate.convertAndSend("/topic/booking/" + bookingId, response);
        return response;
    }

    /**
     * Full chat history for a booking, oldest first.
     * Vollständiger Chatverlauf einer Buchung, älteste zuerst.
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getHistory(UUID bookingId, UUID requesterId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        assertParticipant(booking, requesterId);

        return chatMessageRepository.findByBookingIdWithSender(bookingId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Verifies that {@code userId} is the renter or owner of {@code booking};
     * used both by the REST history endpoint and by the inbound STOMP
     * channel interceptor (subscribe/send) so unauthorized users can neither
     * read nor write a booking's chat thread.
     * Überprüft, dass {@code userId} Mieter oder Eigentümer der
     * {@code booking} ist; wird sowohl vom REST-Verlaufs-Endpunkt als auch
     * vom eingehenden STOMP-Channel-Interceptor (Subscribe/Send) verwendet,
     * damit nicht autorisierte Benutzer den Chat-Thread einer Buchung weder
     * lesen noch schreiben können.
     */
    public void assertParticipant(Booking booking, UUID userId) {
        boolean isRenter = booking.getRenter().getId().equals(userId);
        boolean isOwner = booking.getOwner().getId().equals(userId);
        if (!isRenter && !isOwner) {
            throw new AccessDeniedException(
                    "You are not a participant in this booking / Sie sind kein Teilnehmer dieser Buchung");
        }
    }

    /**
     * Same check as {@link #assertParticipant(Booking, UUID)} but loading the
     * booking by ID first — used by the channel interceptor, which only has
     * the booking ID parsed out of the STOMP destination, not a loaded entity.
     * Gleiche Prüfung wie {@link #assertParticipant(Booking, UUID)}, lädt die
     * Buchung jedoch zuerst per ID — wird vom Channel-Interceptor verwendet,
     * der nur die aus dem STOMP-Ziel extrahierte Buchungs-ID hat, keine
     * geladene Entität.
     */
    @Transactional(readOnly = true)
    public boolean isParticipant(UUID bookingId, UUID userId) {
        return bookingRepository.findById(bookingId)
                .map(booking -> booking.getRenter().getId().equals(userId)
                        || booking.getOwner().getId().equals(userId))
                .orElse(false);
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .bookingId(message.getBooking().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFullName())
                .senderAvatarUrl(message.getSender().getAvatarUrl())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
