package com.rentmybike.bike.repository;

import com.rentmybike.bike.entity.BikePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for bike photos.
 * Repository für Fahrrad-Fotos.
 */
@Repository
public interface BikePhotoRepository extends JpaRepository<BikePhoto, UUID> {

    /**
     * Count of photos for a given bike — used to enforce max 5 limit.
     * Anzahl der Fotos für ein Fahrrad — zum Erzwingen des max. 5-Limits.
     */
    int countByBikeId(UUID bikeId);

    /**
     * All photos for a bike, ordered by displayOrder.
     * Alle Fotos für ein Fahrrad, geordnet nach displayOrder.
     */
    List<BikePhoto> findByBikeIdOrderByDisplayOrderAsc(UUID bikeId);

    /**
     * Find a specific photo that belongs to a specific bike (ownership check).
     * Ein bestimmtes Foto für ein bestimmtes Fahrrad finden (Eigentümerschaftsprüfung).
     */
    Optional<BikePhoto> findByIdAndBikeId(UUID photoId, UUID bikeId);

    /**
     * Returns the highest displayOrder value for a bike's photos.
     * Gibt den höchsten displayOrder-Wert der Fotos eines Fahrrads zurück.
     *
     * <p>Used to assign the next sequential order when uploading a new photo.
     * <p>Wird verwendet, um die nächste sequenzielle Reihenfolge beim Upload zuzuweisen.
     */
    @Query("""
            SELECT COALESCE(MAX(p.displayOrder), -1)
            FROM BikePhoto p
            WHERE p.bike.id = :bikeId
            """)
    int findMaxDisplayOrderByBikeId(@Param("bikeId") UUID bikeId);
}
