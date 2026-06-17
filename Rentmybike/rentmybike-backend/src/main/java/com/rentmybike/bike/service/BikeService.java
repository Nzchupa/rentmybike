package com.rentmybike.bike.service;

import com.rentmybike.bike.dto.*;
import com.rentmybike.bike.entity.ApprovalStatus;
import com.rentmybike.bike.entity.Bike;
import com.rentmybike.bike.entity.BikeCategory;
import com.rentmybike.bike.entity.BikePhoto;
import com.rentmybike.bike.repository.BikePhotoRepository;
import com.rentmybike.bike.repository.BikeRepository;
import com.rentmybike.booking.repository.BookingRepository;
import com.rentmybike.common.exception.AccessDeniedException;
import com.rentmybike.common.exception.BusinessException;
import com.rentmybike.common.exception.ResourceNotFoundException;
import com.rentmybike.common.response.PageResponse;
import com.rentmybike.common.service.CloudinaryService;
import com.rentmybike.user.entity.User;
import com.rentmybike.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service for bike listing management.
 * Service für die Fahrrad-Inseratverwaltung.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>CRUD for bike listings</li>
 *   <li>Photo upload / delete (max 5 per bike)</li>
 *   <li>Public search with filters + pagination</li>
 *   <li>Admin approval workflow (approve / reject)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BikeService {

    private static final int MAX_PHOTOS_PER_BIKE = 5;
    private static final String BIKE_PHOTOS_FOLDER = "rentmybike/bikes";

    private final BikeRepository bikeRepository;
    private final BikePhotoRepository bikePhotoRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final BookingRepository bookingRepository;

    // ──────────────────────────────────────────────────────────────────────────
    // CREATE / ERSTELLEN
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new bike listing for the authenticated owner.
     * Erstellt ein neues Fahrrad-Inserat für den authentifizierten Eigentümer.
     *
     * <p>New bikes start in PENDING approval status — invisible to public search
     * until an admin approves them.
     * <p>Neue Fahrräder starten im PENDING-Status — unsichtbar in öffentlicher Suche
     * bis ein Admin sie genehmigt.
     *
     * @param ownerId owner's UUID / UUID des Eigentümers
     * @param request listing data / Inserat-Daten
     * @return created bike response / erstellte Fahrrad-Antwort
     */
    public BikeResponse createBike(UUID ownerId, CreateBikeRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));

        Bike bike = Bike.builder()
                .owner(owner)
                .title(request.getTitle().strip())
                .description(request.getDescription().strip())
                .category(request.getCategory())
                .pricePerDay(request.getPricePerDay())
                .city(request.getCity().strip())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .available(true)
                .approvalStatus(ApprovalStatus.PENDING)
                .build();

        bikeRepository.save(bike);
        log.info("Bike created: {} by owner: {} / Fahrrad erstellt: {} von Eigentümer: {}",
                bike.getId(), ownerId, bike.getId(), ownerId);

        return toBikeResponse(bike, true);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // READ / LESEN
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Public search — only APPROVED + available + non-deleted bikes.
     * Öffentliche Suche — nur APPROVED + verfügbare + nicht gelöschte Fahrräder.
     *
     * @param city      optional city filter / optionaler Stadtfilter
     * @param category  optional category filter / optionaler Kategoriefilter
     * @param minPrice  optional min price filter / optionaler Mindestpreisfilter
     * @param maxPrice  optional max price filter / optionaler Höchstpreisfilter
     * @param pageable  pagination / Seitennavigation
     */
    @Transactional(readOnly = true)
    public PageResponse<BikeResponse> searchBikes(
            String city, BikeCategory category,
            BigDecimal minPrice, BigDecimal maxPrice,
            Pageable pageable) {

        Page<Bike> page = bikeRepository.searchPublic(city, category, minPrice, maxPrice, pageable);
        // Public view: no address, no rejection reason / Öffentliche Ansicht: keine Adresse, kein Ablehnungsgrund
        return PageResponse.from(page.map(bike -> toBikeResponse(bike, false)));
    }

    /**
     * Get a single bike by ID — public endpoint, only returns APPROVED bikes.
     * Einzelnes Fahrrad nach ID abrufen — öffentlicher Endpunkt, nur APPROVED-Fahrräder.
     */
    @Transactional(readOnly = true)
    public BikeResponse getPublicBike(UUID bikeId) {
        Bike bike = bikeRepository.findByIdWithDetails(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        if (!bike.isPubliclyVisible()) {
            throw new ResourceNotFoundException("Bike", bikeId);
        }

        return toBikeResponse(bike, false);
    }

    /**
     * Get a bike by ID for its owner (includes all fields, any approval status).
     * Fahrrad nach ID für Eigentümer abrufen (alle Felder, jeder Genehmigungsstatus).
     */
    @Transactional(readOnly = true)
    public BikeResponse getOwnerBike(UUID bikeId, UUID ownerId) {
        Bike bike = bikeRepository.findByIdWithDetails(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        requireOwner(bike, ownerId);
        return toBikeResponse(bike, true); // owner sees address + rejection reason
    }

    /**
     * List all bikes owned by a user (for "My Bikes" page).
     * Alle Fahrräder eines Benutzers auflisten (für "Meine Fahrräder"-Seite).
     */
    @Transactional(readOnly = true)
    public PageResponse<BikeResponse> getMyBikes(UUID ownerId, Pageable pageable) {
        Page<Bike> page = bikeRepository.findByOwnerIdAndDeletedAtIsNull(ownerId, pageable);
        return PageResponse.from(page.map(bike -> toBikeResponse(bike, true)));
    }

    /**
     * Admin: list all bikes, optionally filtered by approval status.
     * Admin: alle Fahrräder auflisten, optional nach Genehmigungsstatus gefiltert.
     */
    @Transactional(readOnly = true)
    public PageResponse<BikeResponse> adminListBikes(ApprovalStatus status, Pageable pageable) {
        Page<Bike> page = bikeRepository.findAllForAdmin(status, pageable);
        return PageResponse.from(page.map(bike -> toBikeResponse(bike, true)));
    }

    /**
     * Admin: get a single bike (any status).
     * Admin: einzelnes Fahrrad abrufen (jeder Status).
     */
    @Transactional(readOnly = true)
    public BikeResponse adminGetBike(UUID bikeId) {
        Bike bike = bikeRepository.findByIdWithDetails(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));
        return toBikeResponse(bike, true);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // UPDATE / AKTUALISIEREN
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Owner updates their bike listing.
     * Eigentümer aktualisiert sein Fahrrad-Inserat.
     *
     * <p>If the bike was APPROVED, updating it resets it to PENDING for re-review,
     * because the content has changed. Toggling availability alone does NOT reset status.
     * <p>Wenn das Fahrrad APPROVED war, setzt ein Update es auf PENDING zurück,
     * da sich der Inhalt geändert hat. Das Umschalten der Verfügbarkeit setzt den Status NICHT zurück.
     */
    public BikeResponse updateBike(UUID bikeId, UUID ownerId, UpdateBikeRequest request) {
        Bike bike = bikeRepository.findByIdWithDetails(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        requireOwner(bike, ownerId);

        boolean contentChanged =
                !bike.getTitle().equals(request.getTitle().strip())
                || !bike.getDescription().equals(request.getDescription().strip())
                || bike.getCategory() != request.getCategory()
                || bike.getPricePerDay().compareTo(request.getPricePerDay()) != 0
                || !bike.getCity().equals(request.getCity().strip());

        bike.setTitle(request.getTitle().strip());
        bike.setDescription(request.getDescription().strip());
        bike.setCategory(request.getCategory());
        bike.setPricePerDay(request.getPricePerDay());
        bike.setCity(request.getCity().strip());
        bike.setAddress(request.getAddress());
        bike.setLatitude(request.getLatitude());
        bike.setLongitude(request.getLongitude());
        bike.setAvailable(request.getAvailable());

        // Reset to PENDING if meaningful content changed on an already-approved bike
        // Auf PENDING zurücksetzen, wenn sich wichtiger Inhalt bei einem genehmigten Fahrrad geändert hat
        if (contentChanged && bike.getApprovalStatus() == ApprovalStatus.APPROVED) {
            bike.setApprovalStatus(ApprovalStatus.PENDING);
            bike.setRejectionReason(null);
            log.info("Bike {} reset to PENDING after owner update / Fahrrad {} nach Eigentümer-Update auf PENDING zurückgesetzt",
                    bikeId, bikeId);
        }

        bikeRepository.save(bike);
        return toBikeResponse(bike, true);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DELETE / LÖSCHEN
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Soft-deletes a bike (sets deleted_at timestamp).
     * Löscht ein Fahrrad soft (setzt deleted_at-Zeitstempel).
     *
     * <p>Refuses to delete while the bike has any PENDING/ACCEPTED booking —
     * otherwise a renter could be left with a confirmed rental for a bike that
     * silently disappeared. The owner must wait for those bookings to resolve
     * (complete/cancel/reject) before deleting the listing.
     * <p>Verweigert die Löschung, solange das Fahrrad eine PENDING/ACCEPTED-Buchung
     * hat — sonst könnte ein Mieter mit einer bestätigten Mietbuchung für ein
     * Fahrrad zurückbleiben, das stillschweigend verschwunden ist. Der Eigentümer
     * muss warten, bis diese Buchungen abgeschlossen sind (completed/cancelled/
     * rejected), bevor das Inserat gelöscht werden kann.
     *
     * <p>Photos are NOT removed from Cloudinary immediately — a background cleanup
     * job would handle that in production. For now, only the DB row is soft-deleted.
     * <p>Fotos werden nicht sofort von Cloudinary entfernt — ein Hintergrundbereinigungsjob
     * würde das in der Produktion übernehmen.
     */
    public void deleteBike(UUID bikeId, UUID ownerId) {
        Bike bike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        requireOwner(bike, ownerId);

        if (bookingRepository.existsActiveBookingsForBike(bikeId)) {
            throw new BusinessException(
                    "Cannot delete a bike with pending or accepted bookings — resolve them first / "
                    + "Ein Fahrrad mit ausstehenden oder akzeptierten Buchungen kann nicht gelöscht werden — "
                    + "bitte diese zuerst abschließen");
        }

        bike.softDelete(); // from BaseEntity
        bikeRepository.save(bike);

        log.info("Bike soft-deleted: {} by owner: {} / Fahrrad soft-gelöscht: {} von Eigentümer: {}",
                bikeId, ownerId, bikeId, ownerId);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PHOTOS / FOTOS
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Uploads a photo to a bike (max 5 per bike).
     * Lädt ein Foto zu einem Fahrrad hoch (max. 5 pro Fahrrad).
     *
     * <p>First photo uploaded automatically becomes the primary (cover) photo.
     * <p>Das erste hochgeladene Foto wird automatisch zum Primär- (Titel-) Foto.
     *
     * @return updated bike response with new photo list
     */
    public BikeResponse uploadPhoto(UUID bikeId, UUID ownerId, MultipartFile file) {
        Bike bike = bikeRepository.findByIdWithDetails(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        requireOwner(bike, ownerId);

        int currentCount = bikePhotoRepository.countByBikeId(bikeId);
        if (currentCount >= MAX_PHOTOS_PER_BIKE) {
            throw new BusinessException(
                    "Maximum " + MAX_PHOTOS_PER_BIKE + " photos allowed per bike / "
                    + "Maximal " + MAX_PHOTOS_PER_BIKE + " Fotos pro Fahrrad erlaubt");
        }

        String url = cloudinaryService.uploadImage(file, BIKE_PHOTOS_FOLDER);

        int nextOrder = bikePhotoRepository.findMaxDisplayOrderByBikeId(bikeId) + 1;
        boolean isPrimary = (currentCount == 0); // first photo = primary

        BikePhoto photo = BikePhoto.builder()
                .bike(bike)
                .url(url)
                .displayOrder(nextOrder)
                .primary(isPrimary)
                .build();

        bikePhotoRepository.save(photo);
        log.info("Photo uploaded to bike: {} / Foto zu Fahrrad hochgeladen: {}", bikeId, bikeId);

        // Reload to get fresh photo list / Neu laden für aktuelle Fotoliste
        Bike refreshed = bikeRepository.findByIdWithDetails(bikeId).orElseThrow();
        return toBikeResponse(refreshed, true);
    }

    /**
     * Deletes a single photo from a bike.
     * Löscht ein einzelnes Foto von einem Fahrrad.
     *
     * <p>If the deleted photo was primary, the next photo (lowest displayOrder) becomes primary.
     * <p>Wenn das gelöschte Foto das primäre war, wird das nächste Foto (niedrigster displayOrder) primär.
     */
    public BikeResponse deletePhoto(UUID bikeId, UUID photoId, UUID ownerId) {
        Bike bike = bikeRepository.findByIdWithDetails(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        requireOwner(bike, ownerId);

        BikePhoto photo = bikePhotoRepository.findByIdAndBikeId(photoId, bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", photoId));

        boolean wasPrimary = photo.isPrimary();

        // Delete from Cloudinary / Von Cloudinary löschen
        cloudinaryService.deleteImage(photo.getUrl());
        bikePhotoRepository.delete(photo);

        // If deleted photo was primary, promote the next one / Wenn gelöschtes Foto primär war, nächstes befördern
        if (wasPrimary) {
            List<BikePhoto> remaining = bikePhotoRepository.findByBikeIdOrderByDisplayOrderAsc(bikeId);
            if (!remaining.isEmpty()) {
                BikePhoto newPrimary = remaining.get(0);
                newPrimary.setPrimary(true);
                bikePhotoRepository.save(newPrimary);
            }
        }

        log.info("Photo {} deleted from bike {} / Foto {} von Fahrrad {} gelöscht",
                photoId, bikeId, photoId, bikeId);

        Bike refreshed = bikeRepository.findByIdWithDetails(bikeId).orElseThrow();
        return toBikeResponse(refreshed, true);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // ADMIN APPROVAL / ADMIN-GENEHMIGUNG
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Admin approves a bike listing — makes it visible in public search.
     * Admin genehmigt ein Fahrrad-Inserat — macht es in öffentlicher Suche sichtbar.
     */
    public BikeResponse approveBike(UUID bikeId) {
        Bike bike = bikeRepository.findByIdWithDetails(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        if (bike.getApprovalStatus() == ApprovalStatus.APPROVED) {
            throw new BusinessException("Bike is already approved / Fahrrad ist bereits genehmigt");
        }

        bike.setApprovalStatus(ApprovalStatus.APPROVED);
        bike.setRejectionReason(null);
        bikeRepository.save(bike);

        log.info("Bike APPROVED: {} / Fahrrad GENEHMIGT: {}", bikeId, bikeId);
        return toBikeResponse(bike, true);
    }

    /**
     * Admin rejects a bike listing with a mandatory reason.
     * Admin lehnt ein Fahrrad-Inserat mit einem obligatorischen Grund ab.
     *
     * <p>The owner will see the rejection reason so they can fix and resubmit.
     * <p>Der Eigentümer sieht den Ablehnungsgrund, damit er Probleme beheben und erneut einreichen kann.
     */
    public BikeResponse rejectBike(UUID bikeId, RejectBikeRequest request) {
        Bike bike = bikeRepository.findByIdWithDetails(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        // Guard against re-rejecting an already-rejected bike — symmetric
        // with the already-approved guard in approveBike() above.
        // Schutz vor erneuter Ablehnung eines bereits abgelehnten Fahrrads —
        // symmetrisch zum "bereits genehmigt"-Schutz in approveBike() oben.
        if (bike.getApprovalStatus() == ApprovalStatus.REJECTED) {
            throw new BusinessException("Bike is already rejected / Fahrrad ist bereits abgelehnt");
        }

        bike.setApprovalStatus(ApprovalStatus.REJECTED);
        bike.setRejectionReason(request.getReason().strip());
        bikeRepository.save(bike);

        log.info("Bike REJECTED: {} / Fahrrad ABGELEHNT: {}", bikeId, bikeId);
        return toBikeResponse(bike, true);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Verifies that the given user is the owner of the bike.
     * Überprüft, dass der gegebene Benutzer der Eigentümer des Fahrrads ist.
     *
     * @throws AccessDeniedException if not the owner / wenn nicht der Eigentümer
     */
    private void requireOwner(Bike bike, UUID userId) {
        if (!bike.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException(
                    "You are not the owner of this bike / Sie sind nicht der Eigentümer dieses Fahrrads");
        }
    }

    /**
     * Maps a Bike entity to a BikeResponse DTO.
     * Mappt eine Bike-Entity auf ein BikeResponse-DTO.
     *
     * @param includePrivate if true, includes address and rejectionReason
     *                       wenn true, werden Adresse und Ablehnungsgrund einbezogen
     */
    private BikeResponse toBikeResponse(Bike bike, boolean includePrivate) {
        List<BikePhotoResponse> photoResponses = bike.getPhotos().stream()
                .map(p -> BikePhotoResponse.builder()
                        .id(p.getId())
                        .url(p.getUrl())
                        .displayOrder(p.getDisplayOrder())
                        .primary(p.isPrimary())
                        .build())
                .toList();

        String primaryUrl = bike.getPhotos().stream()
                .filter(BikePhoto::isPrimary)
                .map(BikePhoto::getUrl)
                .findFirst()
                .orElse(null);

        return BikeResponse.builder()
                .id(bike.getId())
                .ownerId(bike.getOwner().getId())
                .ownerName(bike.getOwner().getFullName())
                .ownerAvatarUrl(bike.getOwner().getAvatarUrl())
                .title(bike.getTitle())
                .description(bike.getDescription())
                .category(bike.getCategory())
                .pricePerDay(bike.getPricePerDay())
                .city(bike.getCity())
                .address(includePrivate ? bike.getAddress() : null)
                .latitude(bike.getLatitude())
                .longitude(bike.getLongitude())
                .available(bike.isAvailable())
                .approvalStatus(bike.getApprovalStatus())
                .rejectionReason(includePrivate ? bike.getRejectionReason() : null)
                .photos(photoResponses)
                .primaryPhotoUrl(primaryUrl)
                .createdAt(bike.getCreatedAt())
                .updatedAt(bike.getUpdatedAt())
                .build();
    }
}
