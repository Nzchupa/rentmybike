package com.rentmybike.business.controller;

import com.rentmybike.common.response.ApiResponse;
import com.rentmybike.user.dto.UpgradeToBusinessRequest;
import com.rentmybike.user.dto.UserProfileResponse;
import com.rentmybike.user.entity.User;
import com.rentmybike.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Business account endpoints — base path /api/v1/business.
 * Geschäftskonto-Endpunkte — Basis-Pfad /api/v1/business.
 *
 * <p>Dashboard/stats/calendar/bulk-bike endpoints live in their own
 * controllers (BusinessDashboardController, BikeController#bulkCreate) once
 * implemented — this controller only handles the USER → BUSINESS upgrade.
 * <p>Dashboard-/Statistik-/Kalender-/Massen-Fahrrad-Endpunkte befinden sich in
 * eigenen Controllern, sobald implementiert — dieser Controller behandelt
 * nur das Upgrade USER → BUSINESS.
 */
@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class BusinessController {

    private final UserService userService;

    /**
     * Upgrades the authenticated account to a BUSINESS account.
     * Upgradet das authentifizierte Konto auf ein BUSINESS-Konto.
     *
     * <p>POST /api/v1/business/upgrade
     */
    @PostMapping("/upgrade")
    public ResponseEntity<ApiResponse<UserProfileResponse>> upgrade(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpgradeToBusinessRequest request) {

        UserProfileResponse updated = userService.upgradeToBusiness(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(updated,
                "Upgraded to business account / Auf Geschäftskonto hochgestuft"));
    }
}
