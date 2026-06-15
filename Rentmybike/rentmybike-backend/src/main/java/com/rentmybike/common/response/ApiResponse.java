package com.rentmybike.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified API response wrapper for all endpoints.
 * Einheitliche API-Antwortwrapper für alle Endpunkte.
 *
 * <p>Every response from the API follows this structure:
 * <p>Jede Antwort der API folgt dieser Struktur:
 * <pre>
 * Success / Erfolg:
 * {
 *   "success": true,
 *   "message": "Operation successful",
 *   "data": { ... }
 * }
 *
 * Error / Fehler:
 * {
 *   "success": false,
 *   "message": "What went wrong",
 *   "data": null      ← omitted from JSON / aus JSON weggelassen
 * }
 * </pre>
 *
 * @param <T> the type of data in the response / der Typ der Daten in der Antwort
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // Don't serialize null fields / Null-Felder nicht serialisieren
public class ApiResponse<T> {

    /** Whether the request was successful / Ob die Anfrage erfolgreich war */
    private boolean success;

    /** Human-readable message (both success and error cases) / Menschenlesbare Nachricht (Erfolg und Fehler) */
    private String message;

    /** The response payload — null for error responses / Die Antwortnutzlast — null bei Fehlerantworten */
    private T data;

    // ──────────────────────────────────────────────────────────────────────────
    // Static factory methods / Statische Fabrikmethoden
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates a successful response with data and a message.
     * Erstellt eine erfolgreiche Antwort mit Daten und einer Nachricht.
     *
     * @param data    the response payload / die Antwortnutzlast
     * @param message success message / Erfolgsmeldung
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Creates a successful response with data and a default message.
     * Erstellt eine erfolgreiche Antwort mit Daten und einer Standard-Nachricht.
     *
     * @param data the response payload / die Antwortnutzlast
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Success / Erfolgreich");
    }

    /**
     * Creates an error response (data is null, success is false).
     * Erstellt eine Fehlerantwort (Daten sind null, Erfolg ist false).
     *
     * @param message error description / Fehlerbeschreibung
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
