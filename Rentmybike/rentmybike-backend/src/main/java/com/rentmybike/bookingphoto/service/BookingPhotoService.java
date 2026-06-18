package com.rentmybike.bookingphoto.service;

import com.rentmybike.booking.entity.Booking;
import com.rentmybike.booking.repository.BookingRepository;
import com.rentmybike.bookingphoto.dto.BookingPhotoResponse;
import com.rentmybike.bookingphoto.entity.BookingPhoto;
import com.rentmybike.bookingphoto.entity.BookingPhotoPhase;
import com.rentmybike.bookingphoto.repository.BookingPhotoRepository;
import com.rentmybike.common.exception.AccessDeniedException;
import com.rentmybike.common.exception.ResourceNotFoundException;
import com.rentmybike.common.service.CloudinaryService;
import com.rentmybike.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Service for before/after rental condition photos on a booking.
 * Service für Vorher/Nachher-Mietzustandsfotos einer Buchung.
 *
 * <p>Both the renter and the owner of a booking may upload and view photos —
 * this is the "full cycle" scope the user confirmed (both parties, tied to
 * one specific booking). Anyone else (other users, anonymous visitors) is
 * denied with {@link AccessDeniedException}.
 * <p>Sowohl der Mieter als auch der Eigentümer einer Buchung können Fotos
 * hochladen und einsehen — dies ist der vom Benutzer bestätigte
 * "Vollzyklus"-Umfang (beide Parteien, gebunden an eine bestimmte Buchung).
 * Jeder andere (andere Benutzer, anonyme Besucher) wird mit
 * {@link AccessDeniedException} abgewiesen.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingPhotoService {

    private static final String CLOUDINARY_FOLDER = "rentmybike/booking-photos";

    private final BookingPhotoRepository bookingPhotoRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * Uploads a before/after condition photo for a booking.
     * Lädt ein Vorher/Nachher-Zustandsfoto für eine Buchung hoch.
     *
     * <p>Only the renter or the owner of this specific booking may upload.
     * <p>Nur der Mieter oder der Eigentümer dieser bestimmten Buchung darf hochladen.
     */
    public BookingPhotoResponse uploadPhoto(UUID bookingId, UUID uploaderId,
                                             BookingPhotoPhase phase, MultipartFile file) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        assertParticipant(booking, uploaderId);

        String url = cloudinaryService.uploadImage(file, CLOUDINARY_FOLDER);

        BookingPhoto photo = BookingPhoto.builder()
                .booking(booking)
                .uploader(userRepository.getReferenceById(uploaderId))
                .phase(phase)
                .photoUrl(url)
                .build();

        photo = bookingPhotoRepository.save(photo);
        log.info("Booking photo uploaded for booking {} by user {} (phase={}) / " +
                 "Buchungsfoto für Buchung {} von Benutzer {} hochgeladen (Phase={})",
                bookingId, uploaderId, phase, bookingId, uploaderId, phase);

        return toResponse(photo);
    }

    /**
     * Lists all condition photos for a booking, oldest first.
     * Listet alle Zustandsfotos einer Buchung auf, älteste zuerst.
     *
     * <p>Only the renter or the owner of this booking may view its photos.
     * <p>Nur der Mieter oder der Eigentümer dieser Buchung darf ihre Fotos einsehen.
     */
    @Transactional(readOnly = true)
    public List<BookingPhotoResponse> listPhotos(UUID bookingId, UUID requesterId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        assertParticipant(booking, requesterId);

        return bookingPhotoRepository.findByBookingIdWithUploader(bookingId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Deletes a condition photo — only the user who uploaded it may delete it.
     * Löscht ein Zustandsfoto — nur der Benutzer, der es hochgeladen hat, darf es löschen.
     */
    public void deletePhoto(UUID photoId, UUID requesterId) {
        BookingPhoto photo = bookingPhotoRepository.findById(photoId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("BookingPhoto", photoId));

        if (!photo.getUploader().getId().equals(requesterId)) {
            throw new AccessDeniedException(
                    "You can only delete photos you uploaded / Sie können nur von Ihnen hochgeladene Fotos löschen");
        }

        cloudinaryService.deleteImage(photo.getPhotoUrl());
        photo.softDelete();
        bookingPhotoRepository.save(photo);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    private void assertParticipant(Booking booking, UUID userId) {
        boolean isRenter = booking.getRenter().getId().equals(userId);
        boolean isOwner = booking.getOwner().getId().equals(userId);
        if (!isRenter && !isOwner) {
            throw new AccessDeniedException(
                    "You are not a participant in this booking / Sie sind kein Teilnehmer dieser Buchung");
        }
    }

    private BookingPhotoResponse toResponse(BookingPhoto photo) {
        return BookingPhotoResponse.builder()
                .id(photo.getId())
                .phase(photo.getPhase())
                .photoUrl(photo.getPhotoUrl())
                .uploaderId(photo.getUploader().getId())
                .uploaderName(photo.getUploader().getFullName())
                .createdAt(photo.getCreatedAt())
                .build();
    }
}
