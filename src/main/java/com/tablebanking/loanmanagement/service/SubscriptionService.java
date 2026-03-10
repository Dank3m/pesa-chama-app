package com.tablebanking.loanmanagement.service;

import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.*;
import com.tablebanking.loanmanagement.entity.enums.SubscriptionPlanType;
import com.tablebanking.loanmanagement.entity.enums.SubscriptionStatus;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriptionService {

    private final SubscriptionPlanRepository planRepository;
    private final GroupSubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final BankingGroupRepository groupRepository;
    private final MemberRepository memberRepository;

    /**
     * Get all available subscription plans.
     */
    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getAvailablePlans(UUID groupId) {
        GroupSubscription currentSubscription = subscriptionRepository.findActiveByGroupId(groupId)
                .orElse(null);

        UUID currentPlanId = currentSubscription != null ? currentSubscription.getPlan().getId() : null;

        return planRepository.findAllActivePlans().stream()
                .map(plan -> mapToPlanResponse(plan, currentPlanId))
                .collect(Collectors.toList());
    }

    /**
     * Get the current subscription for a group.
     */
    @Transactional(readOnly = true)
    public GroupSubscriptionResponse getCurrentSubscription(UUID groupId) {
        GroupSubscription subscription = subscriptionRepository.findActiveByGroupId(groupId)
                .orElseThrow(() -> new BusinessException("No active subscription found for this group"));

        return mapToSubscriptionResponse(subscription);
    }

    /**
     * Get subscription overview including current subscription, available plans, and payment history.
     */
    @Transactional(readOnly = true)
    public SubscriptionOverviewResponse getSubscriptionOverview(UUID groupId) {
        GroupSubscription subscription = subscriptionRepository.findActiveByGroupId(groupId)
                .orElse(null);

        List<SubscriptionPlanResponse> availablePlans = getAvailablePlans(groupId);

        List<SubscriptionPaymentResponse> recentPayments = paymentRepository
                .findByGroupIdOrderByCreatedAtDesc(groupId, PageRequest.of(0, 10))
                .stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());

        int currentMemberCount = memberRepository.countActiveByGroup(groupId);
        Integer maxMembers = subscription != null ? subscription.getPlan().getMaxMembers() : Integer.valueOf(10);
        boolean canAddMore = maxMembers == null || currentMemberCount < maxMembers;

        return SubscriptionOverviewResponse.builder()
                .currentSubscription(subscription != null ? mapToSubscriptionResponse(subscription) : null)
                .availablePlans(availablePlans)
                .recentPayments(recentPayments)
                .currentMemberCount(currentMemberCount)
                .maxMembersAllowed(maxMembers)
                .canAddMoreMembers(canAddMore)
                .build();
    }

    /**
     * Create a FREE subscription for a new group.
     */
    public GroupSubscription createFreeSubscription(BankingGroup group) {
        SubscriptionPlan freePlan = planRepository.findByName(SubscriptionPlanType.FREE)
                .orElseThrow(() -> new BusinessException("FREE plan not found in system"));

        GroupSubscription subscription = GroupSubscription.builder()
                .group(group)
                .plan(freePlan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(null) // FREE plan doesn't expire
                .autoRenew(false)
                .isGrandfathered(false)
                .build();

        subscription = subscriptionRepository.save(subscription);

        // Update group with subscription reference
        group.setCurrentSubscription(subscription);
        groupRepository.save(group);

        log.info("Created FREE subscription for group: {}", group.getName());

        return subscription;
    }

    /**
     * Upgrade a group's subscription to a new plan.
     * This creates a pending subscription that will be activated upon payment.
     */
    public GroupSubscription initiateUpgrade(UUID groupId, UUID planId) {
        BankingGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("Group not found"));

        SubscriptionPlan newPlan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("Plan not found"));

        // Check if they're already on this plan
        GroupSubscription currentSubscription = subscriptionRepository.findActiveByGroupId(groupId)
                .orElse(null);

        if (currentSubscription != null &&
                currentSubscription.getPlan().getId().equals(planId)) {
            throw new BusinessException("Group is already on the " + newPlan.getDisplayName() + " plan");
        }

        // Verify member count doesn't exceed new plan limit
        int currentMemberCount = memberRepository.countActiveByGroup(groupId);
        if (newPlan.getMaxMembers() != null && currentMemberCount > newPlan.getMaxMembers()) {
            throw new BusinessException(String.format(
                    "Cannot downgrade to %s plan. Current member count (%d) exceeds plan limit (%d)",
                    newPlan.getDisplayName(), currentMemberCount, newPlan.getMaxMembers()));
        }

        // If upgrading to FREE plan, just activate it
        if (newPlan.getName() == SubscriptionPlanType.FREE) {
            return activateSubscription(group, newPlan, currentSubscription);
        }

        // For paid plans, return the current subscription (payment will trigger activation)
        log.info("Upgrade initiated for group {} to plan {}", group.getName(), newPlan.getDisplayName());

        return currentSubscription;
    }

    /**
     * Activate a subscription after successful payment.
     */
    public GroupSubscription activateSubscription(BankingGroup group, SubscriptionPlan plan,
                                                   GroupSubscription previousSubscription) {
        // Deactivate previous subscription
        if (previousSubscription != null && previousSubscription.getStatus() == SubscriptionStatus.ACTIVE) {
            previousSubscription.setStatus(SubscriptionStatus.CANCELLED);
            previousSubscription.setCancellationReason("Upgraded to " + plan.getDisplayName());
            subscriptionRepository.save(previousSubscription);
        }

        // Calculate subscription period
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = null;

        if (plan.getName() != SubscriptionPlanType.FREE) {
            // Paid plans have an end date
            if ("YEARLY".equals(plan.getBillingPeriod())) {
                endDate = startDate.plusYears(1);
            } else {
                endDate = startDate.plusMonths(1);
            }
        }

        // Create new subscription
        GroupSubscription newSubscription = GroupSubscription.builder()
                .group(group)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(startDate)
                .endDate(endDate)
                .autoRenew(plan.getName() != SubscriptionPlanType.FREE)
                .isGrandfathered(false)
                .build();

        newSubscription = subscriptionRepository.save(newSubscription);

        // Update group reference
        group.setCurrentSubscription(newSubscription);
        groupRepository.save(group);

        log.info("Activated {} subscription for group: {}", plan.getDisplayName(), group.getName());

        return newSubscription;
    }

    /**
     * Cancel a subscription.
     */
    public GroupSubscription cancelSubscription(UUID groupId, String reason) {
        GroupSubscription subscription = subscriptionRepository.findActiveByGroupId(groupId)
                .orElseThrow(() -> new BusinessException("No active subscription found"));

        if (subscription.getIsGrandfathered()) {
            throw new BusinessException("Cannot cancel a grandfathered subscription");
        }

        // Don't actually cancel immediately - disable auto-renewal
        subscription.setAutoRenew(false);
        subscription.setCancellationReason(reason);

        subscription = subscriptionRepository.save(subscription);

        log.info("Subscription auto-renewal disabled for group: {}. Reason: {}",
                subscription.getGroup().getName(), reason);

        return subscription;
    }

    /**
     * Get expiring subscriptions for reminder notifications.
     */
    @Transactional(readOnly = true)
    public List<GroupSubscription> getExpiringSoon(int daysThreshold) {
        LocalDate warningDate = LocalDate.now().plusDays(daysThreshold);
        return subscriptionRepository.findExpiringSoon(LocalDate.now(), warningDate);
    }

    /**
     * Get expired subscriptions that need to be processed.
     */
    @Transactional(readOnly = true)
    public List<GroupSubscription> getExpiredSubscriptions() {
        return subscriptionRepository.findExpiredSubscriptions(LocalDate.now());
    }

    /**
     * Mark expired subscriptions as expired and downgrade to FREE plan.
     */
    public void processExpiredSubscriptions() {
        List<GroupSubscription> expiredSubscriptions = getExpiredSubscriptions();

        for (GroupSubscription subscription : expiredSubscriptions) {
            subscription.expire();
            subscriptionRepository.save(subscription);

            // Downgrade to FREE plan
            SubscriptionPlan freePlan = planRepository.findByName(SubscriptionPlanType.FREE)
                    .orElseThrow(() -> new BusinessException("FREE plan not found"));

            activateSubscription(subscription.getGroup(), freePlan, subscription);

            log.info("Subscription expired for group: {}. Downgraded to FREE plan.",
                    subscription.getGroup().getName());
        }
    }

    /**
     * Get subscriptions ready for auto-renewal.
     */
    @Transactional(readOnly = true)
    public List<GroupSubscription> getSubscriptionsForAutoRenewal() {
        return subscriptionRepository.findSubscriptionsForAutoRenewal(LocalDate.now());
    }

    // Mapping methods

    private SubscriptionPlanResponse mapToPlanResponse(SubscriptionPlan plan, UUID currentPlanId) {
        return SubscriptionPlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName().name())
                .displayName(plan.getDisplayName())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .currency(plan.getCurrency())
                .billingPeriod(plan.getBillingPeriod())
                .maxMembers(plan.getMaxMembers())
                .unlimitedMembers(plan.isUnlimitedMembers())
                .features(plan.getFeatures())
                .sortOrder(plan.getSortOrder())
                .isCurrentPlan(plan.getId().equals(currentPlanId))
                .build();
    }

    private GroupSubscriptionResponse mapToSubscriptionResponse(GroupSubscription subscription) {
        Integer daysUntilExpiry = null;
        boolean isExpiringSoon = false;

        if (subscription.getEndDate() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), subscription.getEndDate());
            daysUntilExpiry = (int) days;
            isExpiringSoon = days <= 7 && days > 0;
        }

        return GroupSubscriptionResponse.builder()
                .id(subscription.getId())
                .groupId(subscription.getGroup().getId())
                .groupName(subscription.getGroup().getName())
                .plan(mapToPlanResponse(subscription.getPlan(), subscription.getPlan().getId()))
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

    private SubscriptionPaymentResponse mapToPaymentResponse(SubscriptionPayment payment) {
        return SubscriptionPaymentResponse.builder()
                .id(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .subscriptionId(payment.getSubscription().getId())
                .planName(payment.getPlan().getDisplayName())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod().name())
                .paymentReference(payment.getPaymentReference())
                .status(payment.getStatus().name())
                .paidByName(payment.getPaidBy() != null ?
                        payment.getPaidBy().getMember().getFullName() : null)
                .paidAt(payment.getPaidAt())
                .periodStart(payment.getPeriodStart())
                .periodEnd(payment.getPeriodEnd())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
