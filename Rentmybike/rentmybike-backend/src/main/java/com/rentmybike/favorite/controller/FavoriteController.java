package com.rentmybike.favorite.controller;

import com.rentmybike.bike.dto.BikeResponse;
import com.rentmybike.common.response.ApiResponse;
import com.rentmybike.common.response.PageResponse;
import com.rentmybike.favorite.dto.FavoriteStatusResponse;
import com.rentmybike.favorite.service.FavoriteService;
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
 * REST endpoints for bike favorites/bookmarks (Stage 2 "Beta launch" trust feature).
 * REST-Endpunkte für Fahrrad-Favoriten/Lesezeichen (Stage-2-"Beta-Start"-Vertrauensfeature).
 *
 * <p>{@code /status} is public (no auth required) so the bike detail page can
 * show the favorite count to anonymous visitors; {@code favorited} is simply
 * {@code false} for them. Every other route requires authentication.
 * <p>{@code /status} ist öffentlich (keine Authentifizierung erforderlich),
 * damit die Fahrrad-Detailseite die Favoritenanzahl auch anonymen Besuchern
 * zeigen kann; {@code favorited} ist für sie einfach {@code false}. Alle
 * anderen Routen erfordern Authentifizierung.
 */
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * Favorites a bike for the current user.
     * Favorisiert ein Fahrrad für den aktuellen Benutzer.
     *
     * <p>POST /api/v1/favorites/{bikeId}
     */
    @PostMapping("/{bikeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> addFavorite(
            @PathVariable UUID bikeId,
            @AuthenticationPrincipal User currentUser) {

        favoriteService.addFavorite(currentUser.getId(), bikeId);
        return ResponseEntity.ok(ApiResponse.success(null, "Added to favorites / Zu Favoriten hinzugefügt"));
    }

    /**
     * Removes a bike from the current user's favorites.
     * Entfernt ein Fahrrad aus den Favoriten des aktuellen Benutzers.
     *
     * <p>DELETE /api/v1/favorites/{bikeId}
     */
    @DeleteMapping("/{bikeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable UUID bikeId,
            @AuthenticationPrincipal User currentUser) {

        favoriteService.removeFavorite(currentUser.getId(), bikeId);
        return ResponseEntity.ok(ApiResponse.success(null, "Removed from favorites / Aus Favoriten entfernt"));
    }

    /**
     * Favorite status + total count for a bike — backs the heart icon.
     * Favoritenstatus + Gesamtanzahl für ein Fahrrad — liefert das Herz-Icon.
     *
     * <p>GET /api/v1/favorites/{bikeId}/status — public, works for anonymous visitors too.
     */
    @GetMapping("/{bikeId}/status")
    public ResponseEntity<ApiResponse<FavoriteStatusResponse>> getStatus(
            @PathVariable UUID bikeId,
            @AuthenticationPrincipal User currentUser) {

        UUID userId = currentUser != null ? currentUser.getId() : null;
        FavoriteStatusResponse status = favoriteService.getStatus(userId, bikeId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Paginated list of the current user's favorited bikes, newest first.
     * Paginierte Liste der favorisierten Fahrräder des aktuellen Benutzers, neueste zuerst.
     *
     * <p>GET /api/v1/favorites?page=0&size=20
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<BikeResponse>>> getMyFavorites(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("createdAt").descending());
        PageResponse<BikeResponse> result = favoriteService.getMyFavorites(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
