package com.rentmybike.favorite.repository;

import com.rentmybike.favorite.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for bike favorites/bookmarks.
 * Repository für Fahrrad-Favoriten/Lesezeichen.
 */
@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    /**
     * Paginated list of a user's favorited bikes, newest first, with the
     * bike + its owner fetched in one query so the response can be mapped
     * straight to {@code BikeResponse} without N+1 lazy-load hits.
     * Paginierte Liste der favorisierten Fahrräder eines Benutzers, neueste
     * zuerst, mit dem Fahrrad + seinem Eigentümer in einer Abfrage geladen,
     * damit die Antwort ohne N+1-Lazy-Load-Zugriffe direkt auf
     * {@code BikeResponse} gemappt werden kann.
     */
    @Query("""
            SELECT f FROM Favorite f
            JOIN FETCH f.bike b
            JOIN FETCH b.owner o
            WHERE f.user.id = :userId
              AND b.deletedAt IS NULL
            ORDER BY f.createdAt DESC
            """)
    Page<Favorite> findByUserIdWithBike(@Param("userId") UUID userId, Pageable pageable);

    Optional<Favorite> findByUserIdAndBikeId(UUID userId, UUID bikeId);

    boolean existsByUserIdAndBikeId(UUID userId, UUID bikeId);

    long countByBikeId(UUID bikeId);

    void deleteByUserIdAndBikeId(UUID userId, UUID bikeId);
}
