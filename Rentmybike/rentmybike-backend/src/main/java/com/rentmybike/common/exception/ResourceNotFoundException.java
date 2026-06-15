package com.rentmybike.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource does not exist or has been soft-deleted.
 * Wird ausgelöst, wenn eine angeforderte Ressource nicht existiert oder soft-gelöscht wurde.
 *
 * <p>Maps to HTTP 404 Not Found / Wird auf HTTP 404 Not Found gemappt.
 *
 * <p>Examples / Beispiele:
 * <ul>
 *   <li>Bike with given UUID not found</li>
 *   <li>User profile not found</li>
 *   <li>Booking ID doesn't exist</li>
 * </ul>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /**
     * @param message human-readable description of what was not found
     *                Menschenlesbare Beschreibung dessen, was nicht gefunden wurde
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Convenience constructor for the common pattern "Entity type not found by id".
     * Komfort-Konstruktor für das häufige Muster "Entitätstyp nicht gefunden nach ID".
     *
     * @param entityName name of the entity (e.g. "Bike") / Name der Entität (z.B. "Bike")
     * @param id         the ID that was looked up / die gesuchte ID
     */
    public ResourceNotFoundException(String entityName, Object id) {
        super(entityName + " not found with id: " + id +
              " / " + entityName + " nicht gefunden mit ID: " + id);
    }
}
