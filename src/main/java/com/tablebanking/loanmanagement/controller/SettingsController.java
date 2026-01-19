package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.User;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.UserRepository;
import com.tablebanking.loanmanagement.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "Settings", description = "User settings and profile management endpoints")
public class SettingsController {

    private final SettingsService settingsService;
    private final UserRepository userRepository;

    // ==================== PROFILE ENDPOINTS ====================

    @GetMapping("/profile")
    @Operation(summary = "Get current user's profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getUserId(userDetails);
        ProfileResponse profile = settingsService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", profile));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current user's profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = getUserId(userDetails);
        ProfileResponse profile = settingsService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", profile));
    }

    // ==================== PREFERENCES ENDPOINTS ====================

    @GetMapping("/preferences")
    @Operation(summary = "Get user preferences/settings")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> getPreferences(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getUserId(userDetails);
        UserSettingsResponse settings = settingsService.getUserSettings(userId);
        return ResponseEntity.ok(ApiResponse.success("Preferences retrieved", settings));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update user preferences/settings")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> updatePreferences(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateUserSettingsRequest request) {
        UUID userId = getUserId(userDetails);
        UserSettingsResponse settings = settingsService.updateUserSettings(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Preferences updated", settings));
    }

    // ==================== SECURITY ENDPOINTS ====================

    @GetMapping("/security")
    @Operation(summary = "Get security settings")
    public ResponseEntity<ApiResponse<SecuritySettingsResponse>> getSecuritySettings(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getUserId(userDetails);
        SecuritySettingsResponse security = settingsService.getSecuritySettings(userId);
        return ResponseEntity.ok(ApiResponse.success("Security settings retrieved", security));
    }

    @PostMapping("/security/change-password")
    @Operation(summary = "Change user password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = getUserId(userDetails);
        settingsService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @PutMapping("/security/2fa")
    @Operation(summary = "Enable or disable two-factor authentication")
    public ResponseEntity<ApiResponse<SecuritySettingsResponse>> toggle2FA(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody Toggle2FARequest request) {
        UUID userId = getUserId(userDetails);
        SecuritySettingsResponse security = settingsService.toggle2FA(userId, request);
        String message = request.getEnabled() ? "2FA enabled" : "2FA disabled";
        return ResponseEntity.ok(ApiResponse.success(message, security));
    }

    // ==================== HELPER METHODS ====================

    private UUID getUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException("Not authenticated");
        }
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));
        return user.getId();
    }
}
