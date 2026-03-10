package com.tablebanking.loanmanagement.scheduler;

import com.tablebanking.loanmanagement.entity.GroupSubscription;
import com.tablebanking.loanmanagement.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduler for subscription-related jobs.
 * Handles subscription expiry, renewal reminders, and auto-renewals.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;

    /**
     * Check and process expired subscriptions.
     * Runs at midnight daily. Expired subscriptions are downgraded to FREE.
     */
    @Scheduled(cron = "${app.scheduler.subscription-expiry-cron:0 0 0 * * ?}")
    @Transactional
    public void processExpiredSubscriptions() {
        log.info("Starting subscription expiry check");

        try {
            subscriptionService.processExpiredSubscriptions();
            log.info("Subscription expiry check completed");
        } catch (Exception e) {
            log.error("Error processing expired subscriptions: {}", e.getMessage(), e);
        }
    }

    /**
     * Send reminders for subscriptions expiring soon.
     * Runs at 9 AM daily. Sends reminders 3 days before expiry.
     */
    @Scheduled(cron = "${app.scheduler.subscription-reminder-cron:0 0 9 * * ?}")
    @Transactional(readOnly = true)
    public void sendExpiryReminders() {
        log.info("Starting subscription expiry reminder check");

        try {
            // Get subscriptions expiring in the next 3 days
            List<GroupSubscription> expiringSoon = subscriptionService.getExpiringSoon(3);

            if (!expiringSoon.isEmpty()) {
                log.info("Found {} subscriptions expiring soon", expiringSoon.size());

                for (GroupSubscription subscription : expiringSoon) {
                    try {
                        // In a full implementation, this would publish an event to send notifications
                        log.info("Subscription expiring soon: Group={}, Plan={}, ExpiryDate={}",
                                subscription.getGroup().getName(),
                                subscription.getPlan().getDisplayName(),
                                subscription.getEndDate());

                        // TODO: Publish notification event
                        // kafkaTemplate.send("notification-events", NotificationEvent.builder()
                        //     .type("SUBSCRIPTION_EXPIRING")
                        //     .groupId(subscription.getGroup().getId())
                        //     .message("Your subscription expires on " + subscription.getEndDate())
                        //     .build());

                    } catch (Exception e) {
                        log.error("Failed to process reminder for group {}: {}",
                                subscription.getGroup().getName(), e.getMessage());
                    }
                }
            }

            log.info("Subscription expiry reminder check completed");
        } catch (Exception e) {
            log.error("Error sending expiry reminders: {}", e.getMessage(), e);
        }
    }

    /**
     * Process auto-renewals for subscriptions expiring today.
     * Runs at 6 AM daily.
     */
    @Scheduled(cron = "${app.scheduler.subscription-renewal-cron:0 0 6 * * ?}")
    @Transactional
    public void processAutoRenewals() {
        log.info("Starting subscription auto-renewal processing");

        try {
            List<GroupSubscription> forRenewal = subscriptionService.getSubscriptionsForAutoRenewal();

            if (!forRenewal.isEmpty()) {
                log.info("Found {} subscriptions for auto-renewal", forRenewal.size());

                for (GroupSubscription subscription : forRenewal) {
                    try {
                        // In a full implementation, this would initiate an automatic payment
                        log.info("Auto-renewal needed for Group={}, Plan={}",
                                subscription.getGroup().getName(),
                                subscription.getPlan().getDisplayName());

                        // TODO: Initiate automatic payment
                        // If the group has a saved payment method, attempt charge
                        // billingService.initiateAutoRenewalPayment(subscription);

                    } catch (Exception e) {
                        log.error("Failed to process auto-renewal for group {}: {}",
                                subscription.getGroup().getName(), e.getMessage());
                    }
                }
            }

            log.info("Subscription auto-renewal processing completed");
        } catch (Exception e) {
            log.error("Error processing auto-renewals: {}", e.getMessage(), e);
        }
    }

    /**
     * Weekly cleanup of stale pending payments.
     * Runs every Sunday at 3 AM.
     */
    @Scheduled(cron = "${app.scheduler.payment-cleanup-cron:0 0 3 ? * SUN}")
    @Transactional
    public void cleanupStalePendingPayments() {
        log.info("Starting stale payment cleanup");

        try {
            // TODO: Implement cleanup of payments that have been pending for too long
            // subscriptionPaymentRepository.findStalePendingPayments(cutoff)
            //     .forEach(payment -> {
            //         payment.markAsFailed("Payment timed out");
            //         subscriptionPaymentRepository.save(payment);
            //     });

            log.info("Stale payment cleanup completed");
        } catch (Exception e) {
            log.error("Error during payment cleanup: {}", e.getMessage(), e);
        }
    }
}
