package com.rentmybike.accessory.repository;

import com.rentmybike.accessory.entity.Accessory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for business accessories (helmets, child seats, locks).
 * Repository für Business-Zubehör (Helme, Kindersitze, Schlösser).
 */
@Repository
public interface AccessoryRepository extends JpaRepository<Accessory, UUID> {

    /**
     * Active accessories owned by a given business, newest first — backs
     * both the "my accessories" management list and the public
     * "accessories offered by this owner" lookup used at booking time.
     * Aktives Zubehör eines bestimmten Unternehmens, neueste zuerst —
     * liefert sowohl die "Mein Zubehör"-Verwaltungsliste als auch die
     * öffentliche "von diesem Eigentümer angebotenes Zubehör"-Abfrage zur
     * Buchungszeit.
     */
    @Query("""
            SELECT a FROM Accessory a
            WHERE a.owner.id = :ownerId
              AND a.deletedAt IS NULL
            ORDER BY a.createdAt DESC
            """)
    List<Accessory> findActiveByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Single active accessory lookup, used before update/delete to ensure
     * the record exists and hasn't been soft-deleted already.
     * Einzelne Zubehör-Abfrage für aktive Datensätze, verwendet vor
     * Aktualisierung/Löschung, um sicherzustellen, dass der Datensatz
     * existiert und noch nicht soft-gelöscht wurde.
     */
    @Query("""
            SELECT a FROM Accessory a
            WHERE a.id = :id
              AND a.deletedAt IS NULL
            """)
    Optional<Accessory> findActiveById(@Param("id") UUID id);
}
