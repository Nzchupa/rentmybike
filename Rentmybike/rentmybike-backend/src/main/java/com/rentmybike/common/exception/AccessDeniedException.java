package com.rentmybike.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an authenticated user attempts an action they're not authorized to perform.
 * Wird ausgelöst, wenn ein authentifizierter Benutzer eine Aktion versucht, zu der er nicht berechtigt ist.
 *
 * <p>Maps to HTTP 403 Forbidden / Wird auf HTTP 403 Forbidden gemappt.
 *
 * <p>Note: named with "Custom" prefix to avoid clash with
 * {@code org.springframework.security.access.AccessDeniedException}.
 * Hinweis: mit "Custom" Präfix benannt, um Kollision mit
 * {@code org.springframework.security.access.AccessDeniedException} zu vermeiden.
 *
 * <p>Examples / Beispiele:
 * <ul>
 *   <li>User tries to edit another user's bike listing</li>
 *   <li>Renter tries to accept/reject a booking (only owner can)</li>
 *   <li>Regular user tries to access admin endpoints</li>
 * </ul>
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessDeniedException extends RuntimeException {

    /**
     * @param message description of the denied action
     *                Beschreibung der verweigerten Aktion
     */
    public AccessDeniedException(String message) {
        super(message);
    }
}
