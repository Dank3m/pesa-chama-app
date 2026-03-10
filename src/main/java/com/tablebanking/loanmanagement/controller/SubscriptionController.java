package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.User;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.UserRepository;
import com.tablebanking.loanmanagement.service.FeatureGateService;
import com.tablebanking.loanmanagement.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Subscription management endpoints")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final FeatureGateService featureGateService;
    private final UserRepository userRepository;

    @GetMapping("/plans")
    @Operation(summary = "Get all available subscription plans")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>> getAvailablePlans(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID groupId = getGroupId(userDetails);
        List<SubscriptionPlanResponse> plans = subscriptionService.getAvailablePlans(groupId);
        return ResponseEntity.ok(ApiResponse.success("Available plans retrieved", plans));
    }

    @GetMapping("/current")
    @Operation(summary = "Get current subscription for the group")
    public ResponseEntity<ApiResponse<GroupSubscriptionResponse>> getCurrentSubscription(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID groupId = getGroupId(userDetails);
        GroupSubscriptionResponse subscription = subscriptionService.getCurrentSubscription(groupId);
        return ResponseEntity.ok(ApiResponse.success("Current subscription retrieved", subscription));
    }

    @GetMapping("/overview")
    @Operation(summary = "Get subscription overview including plans and payment history")
    public ResponseEntity<ApiResponse<SubscriptionOverviewResponse>> getSubscriptionOverview(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID groupId = getGroupId(userDetails);
        SubscriptionOverviewResponse overview = subscriptionService.getSubscriptionOverview(groupId);
        return ResponseEntity.ok(ApiResponse.success("Subscription overview retrieved", overview));
    }

    @PostMapping("/upgrade")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Initiate subscription upgrade")
    public ResponseEntity<ApiResponse<GroupSubscriptionResponse>> upgradeSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpgradeSubscriptionRequest request) {
        UUID groupId = getGroupId(userDetails);
        var subscription = subscriptionService.initiateUpgrade(groupId, request.getPlanId());
        return ResponseEntity.ok(ApiResponse.success("Upgrade initiated. Please complete payment.",
                mapToResponse(subscription)));
    }

    @PostMapping("/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel subscription auto-renewal")
    public ResponseEntity<ApiResponse<GroupSubscriptionResponse>> cancelSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CancelSubscriptionRequest request) {
        UUID groupId = getGroupId(userDetails);
        var subscription = subscriptionService.cancelSubscription(groupId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(
                "Subscription will not auto-renew. Access continues until the end of current period.",
                mapToResponse(subscription)));
    }

    @GetMapping("/features/{feature}")
    @Operation(summary = "Check if a specific feature is available")
    public ResponseEntity<ApiResponse<FeatureAccessResponse>> checkFeatureAccess(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String feature) {
        UUID groupId = getGroupId(userDetails);
        FeatureAccessResponse access = featureGateService.checkFeatureAccess(groupId, feature);
        return ResponseEntity.ok(ApiResponse.success(access));
    }

    @GetMapping("/features")
    @Operation(summary = "Check multiple features at once")
    public ResponseEntity<ApiResponse<List<FeatureAccessResponse>>> checkMultipleFeatures(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam List<String> features) {
        UUID groupId = getGroupId(userDetails);
        List<FeatureAccessResponse> accessList = features.stream()
                .map(feature -> featureGateService.checkFeatureAccess(groupId, feature))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(accessList));
    }

    // Helper methods

    private UUID getGroupId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException("Not authenticated");
        }
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));
        if (user.getMember() == null || user.getMember().getGroup() == null) {
            throw new BusinessException("User is not associated with a group");
        }
        return user.getMember().getGroup().getId();
    }

    private UUID getUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException("Not authenticated");
        }
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));
        return user.getId();
    }

    private GroupSubscriptionResponse mapToResponse(
            com.tablebanking.loanmanagement.entity.GroupSubscription subscription) {
        if (subscription == null) {
            return null;
        }

        Integer daysUntilExpiry = null;
        boolean isExpiringSoon = false;

        if (subscription.getEndDate() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(), subscription.getEndDate());
            daysUntilExpiry = (int) days;
            isExpiringSoon = days <= 7 && days > 0;
        }

        return GroupSubscriptionResponse.builder()
                .id(subscription.getId())
                .groupId(subscription.getGroup().getId())
                .groupName(subscription.getGroup().getName())
                .plan(SubscriptionPlanResponse.builder()
                        .id(subscription.getPlan().getId())
                        .name(subscription.getPlan().getName().name())
                        .displayName(subscription.getPlan().getDisplayName())
                        .description(subscription.getPlan().getDescription())
                        .price(subscription.getPlan().getPrice())
                        .currency(subscription.getPlan().getCurrency())
                        .billingPeriod(subscription.getPlan().getBillingPeriod())
                        .maxMembers(subscription.getPlan().getMaxMembers())
                        .unlimitedMembers(subscription.getPlan().isUnlimitedMembers())
                        .features(subscription.getPlan().getFeatures())
                        .sortOrder(subscription.getPlan().getSortOrder())
                        .isCurrentPlan(true)
                        .build())
                .status(subscription.getStatus().name())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .autoRenew(subscription.getAutoRenew())
                .isGrandfathered(subscription.getIsGrandfathered())
                .isExpiringSoon(isExpiringSoon)
                .daysUntilExpiry(daysUntilExpiry)
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}
