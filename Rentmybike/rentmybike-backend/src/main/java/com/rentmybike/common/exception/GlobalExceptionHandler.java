package com.rentmybike.common.exception;

import com.rentmybike.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

/**
 * Global exception handler — catches all exceptions thrown from any @RestController
 * and returns a consistent {@link ApiResponse} structure.
 *
 * Globaler Ausnahmebehandler — fängt alle Ausnahmen aus @RestControllern
 * und gibt eine einheitliche {@link ApiResponse}-Struktur zurück.
 *
 * <p>Every error the client sees goes through here, ensuring consistent format:
 * <p>Jeder Fehler, den der Client sieht, geht hier durch, um einheitliches Format zu gewährleisten:
 * <pre>
 * {
 *   "success": false,
 *   "message": "Human-readable error / Menschenlesbarer Fehler",
 *   "data": null
 * }
 * </pre>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ──────────────────────────────────────────────────────────────────────────
    // Business / domain exceptions / Geschäfts- / Domänenausnahmen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 404 — Resource not found / Ressource nicht gefunden.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.debug("Resource not found: {} / Ressource nicht gefunden: {}", ex.getMessage(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 400 — Business rule violation / Geschäftsregelverletzung.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.debug("Business rule violation: {} / Geschäftsregelverletzung: {}", ex.getMessage(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 403 — Custom access denied / Benutzerdefinierter Zugriff verweigert.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {} / Zugriff verweigert: {}", ex.getMessage(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Spring Security exceptions / Spring Security-Ausnahmen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 403 — Spring Security's access denied / Spring Securitys Zugriff verweigert.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleSpringAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied / Zugriff verweigert"));
    }

    /**
     * 401 — Bad credentials (wrong password) / Falsche Anmeldedaten.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid credentials / Ungültige Anmeldedaten"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Validation exceptions / Validierungsausnahmen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 422 — @Valid annotation triggered validation failure.
     * 422 — @Valid Annotation hat Validierungsfehler ausgelöst.
     *
     * <p>Collects all field errors into a readable message.
     * <p>Sammelt alle Feldfehler in eine lesbare Nachricht.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        // Collect all field-level validation errors / Alle Feld-Validierungsfehler sammeln
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.debug("Validation failed: {} / Validierung fehlgeschlagen: {}", errors, errors);
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("Validation failed / Validierung fehlgeschlagen: " + errors));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Spring Web exceptions / Spring Web-Ausnahmen
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Handles ResponseStatusException thrown directly with a specific status.
     * Verarbeitet ResponseStatusException, die direkt mit einem bestimmten Status geworfen wird.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ApiResponse.error(ex.getReason() != null ? ex.getReason() : ex.getMessage()));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Catch-all / Auffangnetz
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 500 — Unexpected error fallback. Logs full stack trace for debugging.
     * 500 — Unerwarteter Fehler-Fallback. Protokolliert vollständigen Stack-Trace zum Debuggen.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        // Always log unexpected errors with full stack trace
        // Unerwartete Fehler immer mit vollständigem Stack-Trace protokollieren
        log.error("Unexpected error / Unerwarteter Fehler: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "An unexpected error occurred. Please try again later. / " +
                        "Ein unerwarteter Fehler ist aufgetreten. Bitte versuche es später erneut."));
    }
}
