package com.rentmybike.favorite.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Whether the current user has favorited a given bike, plus the bike's
 * total favorite count — backs the heart icon on bike cards/detail pages.
 * Ob der aktuelle Benutzer ein bestimmtes Fahrrad favorisiert hat, plus die
 * Gesamtanzahl der Favoriten des Fahrrads — liefert das Herz-Icon auf
 * Fahrrad-Karten/Detailseiten.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteStatusResponse {
    private boolean favorited;
    private long favoriteCount;
}
