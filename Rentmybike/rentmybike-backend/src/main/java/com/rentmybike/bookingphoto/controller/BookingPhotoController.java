package com.rentmybike.bookingphoto.controller;

import com.rentmybike.bookingphoto.dto.BookingPhotoResponse;
import com.rentmybike.bookingphoto.entity.BookingPhotoPhase;
import com.rentmybike.bookingphoto.service.BookingPhotoService;
import com.rentmybike.common.response.ApiResponse;
import com.rentmybike.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for before/after rental condition photos on a booking.
 * REST-Controller für Vorher/Nachher-Mietzustandsfotos einer Buchung.
 *
 * <p>All endpoints require authentication; the service layer additionally
 * restricts access to the renter/owner of the specific booking.
 * <p>Alle Endpunkte erfordern Authentifizierung; die Service-Schicht
 * beschränkt den Zugriff zusätzlich auf den Mieter/Eigentümer der
 * jeweiligen Buchung.
 */
@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/photos")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class BookingPhotoController {

    private final BookingPhotoService bookingPhotoService;

    /**
     * Upload a before/after condition photo for a booking.
     * Vorher/Nachher-Zustandsfoto für eine Buchung hochladen.
     *
     * <p>POST /api/v1/bookings/{bookingId}/photos (multipart/form-data, fields: "file", "phase")
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BookingPhotoResponse>> uploadPhoto(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal User currentUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam("phase") BookingPhotoPhase phase) {

        BookingPhotoResponse uploaded = bookingPhotoService.uploadPhoto(bookingId, currentUser.getId(), phase, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(uploaded, "Photo uploaded / Foto hochgeladen"));
    }

    /**
     * List all condition photos for a booking.
     * Alle Zustandsfotos einer Buchung auflisten.
     *
     * <p>GET /api/v1/bookings/{bookingId}/photos
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingPhotoResponse>>> listPhotos(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal User currentUser) {

        List<BookingPhotoResponse> photos = bookingPhotoService.listPhotos(bookingId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(photos));
    }

    /**
     * Delete a condition photo — only the uploader may delete it.
     * Ein Zustandsfoto löschen — nur der Uploader darf es löschen.
     *
     * <p>DELETE /api/v1/bookings/{bookingId}/photos/{photoId}
     */
    @DeleteMapping("/{photoId}")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(
            @PathVariable UUID bookingId,
            @PathVariable UUID photoId,
            @AuthenticationPrincipal User currentUser) {

        bookingPhotoService.deletePhoto(photoId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Photo deleted / Foto gelöscht"));
    }
}
