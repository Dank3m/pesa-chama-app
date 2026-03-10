package com.tablebanking.loanmanagement.service;

import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.FeatureAccessResponse;
import com.tablebanking.loanmanagement.entity.GroupSubscription;
import com.tablebanking.loanmanagement.entity.SubscriptionPlan;
import com.tablebanking.loanmanagement.entity.enums.SubscriptionPlanType;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.GroupSubscriptionRepository;
import com.tablebanking.loanmanagement.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FeatureGateService {

    private final GroupSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;

    // Feature to minimum required plan mapping
    private static final Map<String, SubscriptionPlanType> FEATURE_REQUIREMENTS = Map.of(
            "loans", SubscriptionPlanType.STANDARD,
            "sms", SubscriptionPlanType.STANDARD,
            "externalLoans", SubscriptionPlanType.PREMIUM,
            "apiAccess", SubscriptionPlanType.PREMIUM,
            "prioritySupport", SubscriptionPlanType.PREMIUM
    );

    /**
     * Check if a group has access to a specific feature.
     */
    public boolean hasFeature(UUID groupId, String feature) {
        GroupSubscription subscription = subscriptionRepository.findActiveByGroupId(groupId)
                .orElse(null);

        if (subscription == null) {
            // No subscription found - only basic features available
            return isBasicFeature(feature);
        }

        SubscriptionPlan plan = subscription.getPlan();
        return plan.hasFeature(feature);
    }

    /**
     * Check feature access and throw exception if not available.
     */
    public void requireFeature(UUID groupId, String feature) {
        if (!hasFeature(groupId, feature)) {
            SubscriptionPlanType requiredPlan = FEATURE_REQUIREMENTS.getOrDefault(feature, SubscriptionPlanType.STANDARD);
            throw new FeatureNotAvailableException(feature, requiredPlan.name());
        }
    }

    /**
     * Get detailed feature access information.
     */
    public FeatureAccessResponse checkFeatureAccess(UUID groupId, String feature) {
        GroupSubscription subscription = subscriptionRepository.findActiveByGroupId(groupId)
                .orElse(null);

        String currentPlan = "NONE";
        boolean hasAccess = false;

        if (subscription != null) {
            currentPlan = subscription.getPlan().getName().name();
            hasAccess = subscription.getPlan().hasFeature(feature);
        } else {
            hasAccess = isBasicFeature(feature);
        }

        SubscriptionPlanType requiredPlan = FEATURE_REQUIREMENTS.getOrDefault(feature, SubscriptionPlanType.FREE);

        String message;
        if (hasAccess) {
            message = "Feature '" + feature + "' is available on your current plan.";
        } else {
            message = "Feature '" + feature + "' requires " + requiredPlan.name() + " plan or higher. " +
                    "Please upgrade to access this feature.";
        }

        return FeatureAccessResponse.builder()
                .feature(feature)
                .hasAccess(hasAccess)
                .currentPlan(currentPlan)
                .requiredPlan(requiredPlan.name())
                .message(message)
                .build();
    }

    /**
     * Check if group can add more members based on their plan limits.
     */
    public boolean canAddMembers(UUID groupId, int currentMemberCount) {
        GroupSubscription subscription = subscriptionRepository.findActiveByGroupId(groupId)
                .orElse(null);

        if (subscription == null) {
            // Default to FREE plan limit
            return currentMemberCount < 10;
        }

        Integer maxMembers = subscription.getPlan().getMaxMembers();
        if (maxMembers == null) {
            return true; // Unlimited
        }

        return currentMemberCount < maxMembers;
    }

    /**
     * Get the maximum number of members allowed for a group's plan.
     */
    public Integer getMaxMembersAllowed(UUID groupId) {
        GroupSubscription subscription = subscriptionRepository.findActiveByGroupId(groupId)
                .orElse(null);

        if (subscription == null) {
            // Default to FREE plan limit
            SubscriptionPlan freePlan = planRepository.findByName(SubscriptionPlanType.FREE)
                    .orElse(null);
            return freePlan != null ? freePlan.getMaxMembers() : 10;
        }

        return subscription.getPlan().getMaxMembers();
    }

    /**
     * Check if a feature is available on the FREE plan.
     */
    private boolean isBasicFeature(String feature) {
        return switch (feature) {
            case "contributions", "basicReports" -> true;
            default -> false;
        };
    }

    /**
     * Custom exception for feature not available.
     */
    public static class FeatureNotAvailableException extends BusinessException {
        private final String feature;
        private final String requiredPlan;

        public FeatureNotAvailableException(String feature, String requiredPlan) {
            super(String.format("Feature '%s' requires %s plan or higher. Please upgrade to access this feature.",
                    feature, requiredPlan));
            this.feature = feature;
            this.requiredPlan = requiredPlan;
        }

        public String getFeature() {
            return feature;
        }

        public String getRequiredPlan() {
            return requiredPlan;
        }
    }
}
