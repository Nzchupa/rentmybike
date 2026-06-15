package com.rentmybike.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a business rule is violated.
 * Wird ausgelöst, wenn eine Geschäftsregel verletzt wird.
 *
 * <p>Maps to HTTP 400 Bad Request / Wird auf HTTP 400 Bad Request gemappt.
 *
 * <p>Examples / Beispiele:
 * <ul>
 *   <li>Email already registered</li>
 *   <li>Bike not available for requested dates</li>
 *   <li>User tries to book their own bike</li>
 *   <li>Review already submitted for this booking</li>
 * </ul>
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessException extends RuntimeException {

    /**
     * @param message description of the business rule violation
     *                Beschreibung der Geschäftsregelverletzung
     */
    public BusinessException(String message) {
        super(message);
    }
}
