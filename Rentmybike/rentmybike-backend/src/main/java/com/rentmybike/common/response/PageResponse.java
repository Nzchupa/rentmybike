package com.rentmybike.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standardized pagination wrapper for list endpoints.
 * Standardisierter Paginierungs-Wrapper für Listen-Endpunkte.
 *
 * <p>Used for: GET /bikes, GET /bookings/my/renter, GET /admin/users, etc.
 * <p>Verwendet für: GET /bikes, GET /bookings/my/renter, GET /admin/users, etc.
 *
 * <p>Response structure / Antwortstruktur:
 * <pre>
 * {
 *   "content": [ ... ],
 *   "page": 0,
 *   "size": 20,
 *   "totalElements": 150,
 *   "totalPages": 8,
 *   "last": false
 * }
 * </pre>
 *
 * @param <T> type of items in the page / Typ der Elemente auf der Seite
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /** The items on this page / Die Elemente auf dieser Seite */
    private List<T> content;

    /** Current page number (0-indexed) / Aktuelle Seitennummer (0-basiert) */
    private int page;

    /** Number of items per page / Anzahl der Elemente pro Seite */
    private int size;

    /** Total number of items across all pages / Gesamtanzahl der Elemente über alle Seiten */
    private long totalElements;

    /** Total number of pages / Gesamtanzahl der Seiten */
    private int totalPages;

    /** Whether this is the last page / Ob dies die letzte Seite ist */
    private boolean last;

    // ──────────────────────────────────────────────────────────────────────────
    // Static factory / Statische Fabrik
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Converts a Spring Data {@link Page} into a {@link PageResponse}.
     * Konvertiert eine Spring Data {@link Page} in eine {@link PageResponse}.
     *
     * <p>Usage example / Verwendungsbeispiel:
     * <pre>
     * Page&lt;BikeDto&gt; page = bikeRepository.findAll(pageable).map(bikeMapper::toDto);
     * return PageResponse.from(page);
     * </pre>
     *
     * @param page the Spring Data page / die Spring Data-Seite
     * @param <T>  item type / Elementtyp
     * @return standardized page response / standardisierte Seitenantwort
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
