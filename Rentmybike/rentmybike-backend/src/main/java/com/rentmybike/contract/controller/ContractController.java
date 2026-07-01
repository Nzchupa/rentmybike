package com.rentmybike.contract.controller;

import com.rentmybike.common.response.ApiResponse;
import com.rentmybike.contract.dto.ContractResponse;
import com.rentmybike.contract.service.ContractService;
import com.rentmybike.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for the rental contract auto-generated on booking
 * acceptance — reachable by either participant of the underlying booking.
 * REST-Controller für den bei Buchungsannahme automatisch erstellten
 * Mietvertrag — erreichbar für beide Teilnehmer der zugrunde liegenden
 * Buchung.
 *
 * <p>Endpoint group / Endpunktgruppe:
 * <ul>
 *   <li>GET  /api/v1/bookings/{id}/contract       — view the contract</li>
 *   <li>POST /api/v1/bookings/{id}/contract/accept — click-to-accept (role inferred from caller)</li>
 *   <li>GET  /api/v1/bookings/{id}/contract/pdf    — download as PDF</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    /**
     * View the contract for a booking — accessible to the renter or owner.
     * Vertrag zu einer Buchung ansehen — zugänglich für Mieter oder Eigentümer.
     */
    @GetMapping("/api/v1/bookings/{id}/contract")
    public ResponseEntity<ApiResponse<ContractResponse>> getContract(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        ContractResponse contract = contractService.getContract(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(contract));
    }

    /**
     * Click-to-accept — records timestamp + caller IP for whichever role
     * (owner/renter) the authenticated caller has on this booking.
     * Klick-Zustimmung — erfasst Zeitstempel + Aufrufer-IP für die Rolle
     * (Eigentümer/Mieter), die der authentifizierte Aufrufer bei dieser
     * Buchung hat.
     *
     * <p>POST /api/v1/bookings/{id}/contract/accept
     */
    @PostMapping("/api/v1/bookings/{id}/contract/accept")
    public ResponseEntity<ApiResponse<ContractResponse>> acceptContract(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest request) {

        ContractResponse contract = contractService.accept(id, currentUser.getId(), clientIp(request));
        return ResponseEntity.ok(ApiResponse.success(contract,
                "Contract accepted / Vertrag akzeptiert"));
    }

    /**
     * Download the contract as a PDF.
     * Vertrag als PDF herunterladen.
     *
     * <p>GET /api/v1/bookings/{id}/contract/pdf
     */
    @GetMapping("/api/v1/bookings/{id}/contract/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        byte[] pdf = contractService.generatePdf(id, currentUser.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mietvertrag-" + id + ".pdf\"")
                .body(pdf);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers / Private Hilfsmethoden
    // ──────────────────────────────────────────────────────────────────────────

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // First entry is the original client when passed through a proxy/load balancer.
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
