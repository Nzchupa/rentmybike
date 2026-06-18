package com.rentmybike.favorite.service;

import com.rentmybike.bike.dto.BikeResponse;
import com.rentmybike.bike.entity.Bike;
import com.rentmybike.bike.repository.BikeRepository;
import com.rentmybike.common.exception.ResourceNotFoundException;
import com.rentmybike.common.response.PageResponse;
import com.rentmybike.favorite.dto.FavoriteStatusResponse;
import com.rentmybike.favorite.entity.Favorite;
import com.rentmybike.favorite.repository.FavoriteRepository;
import com.rentmybike.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for bike favorites/bookmarks (Stage 2 "Beta launch" trust feature).
 * Service für Fahrrad-Favoriten/Lesezeichen (Stage-2-"Beta-Start"-Vertrauensfeature).
 *
 * <p>Favoriting is a simple idempotent toggle: adding an already-favorited
 * bike or removing a not-favorited one is a no-op rather than an error, so
 * the frontend can fire-and-forget without checking state first.
 * <p>Favorisieren ist ein einfacher idempotenter Umschalter: das Favorisieren
 * eines bereits favorisierten Fahrrads oder das Entfernen eines nicht
 * favorisierten ist ein No-op statt eines Fehlers, sodass das Frontend ohne
 * vorherige Statusprüfung "fire-and-forget" senden kann.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final BikeRepository bikeRepository;
    private final UserRepository userRepository;

    // ──────────────────────────────────────────────────────────────────────────
    // Add / remove / Hinzufügen / Entfernen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Favorites a bike for the current user. No-op if already favorited.
     * Favorisiert ein Fahrrad für den aktuellen Benutzer. No-op, falls bereits favorisiert.
     */
    public void addFavorite(UUID userId, UUID bikeId) {
        if (favoriteRepository.existsByUserIdAndBikeId(userId, bikeId)) {
            return;
        }

        Bike bike = bikeRepository.findById(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike", bikeId));

        Favorite favorite = Favorite.builder()
                .user(userRepository.getReferenceById(userId))
                .bike(bike)
                .build();

        favoriteRepository.save(favorite);
        log.info("Bike {} favorited by user {} / Fahrrad {} von Benutzer {} favorisiert",
                bikeId, userId, bikeId, userId);
    }

    /**
     * Removes a bike from the current user's favorites. No-op if not favorited.
     * Entfernt ein Fahrrad aus den Favoriten des aktuellen Benutzers. No-op, falls nicht favorisiert.
     */
    public void removeFavorite(UUID userId, UUID bikeId) {
        favoriteRepository.deleteByUserIdAndBikeId(userId, bikeId);
        log.info("Bike {} unfavorited by user {} / Fahrrad {} von Benutzer {} entfernt",
                bikeId, userId, bikeId, userId);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Read / Lesen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Favorite status + total count for a single bike — backs the heart
     * icon on bike cards/detail pages.
     * Favoritenstatus + Gesamtanzahl für ein einzelnes Fahrrad — liefert das
     * Herz-Icon auf Fahrrad-Karten/Detailseiten.
     */
    @Transactional(readOnly = true)
    public FavoriteStatusResponse getStatus(UUID userId, UUID bikeId) {
        boolean favorited = userId != null && favoriteRepository.existsByUserIdAndBikeId(userId, bikeId);
        long count = favoriteRepository.countByBikeId(bikeId);
        return FavoriteStatusResponse.builder()
                .favorited(favorited)
                .favoriteCount(count)
                .build();
    }

    /**
     * Paginated list of the current user's favorited bikes, newest first.
     * Paginierte Liste der favorisierten Fahrräder des aktuellen Benutzers, neueste zuerst.
     */
    @Transactional(readOnly = true)
    public PageResponse<BikeResponse> getMyFavorites(UUID userId, Pageable pageable) {
        Page<Favorite> page = favoriteRepository.findByUserIdWithBike(userId, pageable);
        return PageResponse.from(page.map(this::toBikeResponse));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Maps a favorited Bike to a public-view BikeResponse (no address /
     * rejection reason — same privacy rule as public search).
     * Mappt ein favorisiertes Fahrrad auf ein öffentliches BikeResponse
     * (keine Adresse / kein Ablehnungsgrund — gleiche Datenschutzregel wie
     * bei der öffentlichen Suche).
     */
    private BikeResponse toBikeResponse(Favorite favorite) {
        Bike bike = favorite.getBike();
        List<com.rentmybike.bike.dto.BikePhotoResponse> photoResponses = bike.getPhotos().stream()
                .map(p -> com.rentmybike.bike.dto.BikePhotoResponse.builder()
                        .id(p.getId())
                        .url(p.getUrl())
                        .displayOrder(p.getDisplayOrder())
                        .primary(p.isPrimary())
                        .build())
                .toList();

        String primaryUrl = bike.getPhotos().stream()
                .filter(com.rentmybike.bike.entity.BikePhoto::isPrimary)
                .map(com.rentmybike.bike.entity.BikePhoto::getUrl)
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
                .latitude(bike.getLatitude())
                .longitude(bike.getLongitude())
                .available(bike.isAvailable())
                .approvalStatus(bike.getApprovalStatus())
                .photos(photoResponses)
                .primaryPhotoUrl(primaryUrl)
                .createdAt(bike.getCreatedAt())
                .updatedAt(bike.getUpdatedAt())
                .build();
    }
}
