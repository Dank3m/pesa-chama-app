package com.tablebanking.loanmanagement.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tablebanking.loanmanagement.entity.User;
import com.tablebanking.loanmanagement.entity.enums.UserRole;
import com.tablebanking.loanmanagement.repository.UserRepository;
import com.tablebanking.loanmanagement.service.InAppNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Listens to Kafka events and creates in-app notifications
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final InAppNotificationService notificationService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.loan-events:loan-events}", groupId = "notification-group")
    public void handleLoanEvent(String message) {
        try {
            LoanEvent event = objectMapper.readValue(message, LoanEvent.class);
            log.info("Received loan event: {} for loan {}", event.getEventType(), event.getLoanNumber());

            switch (event.getEventType()) {
                case "LOAN_APPLIED" -> handleLoanApplied(event);
                case "LOAN_APPROVED" -> handleLoanApproved(event);
                case "LOAN_REJECTED" -> handleLoanRejected(event);
                case "LOAN_DISBURSED" -> handleLoanDisbursed(event);
                case "LOAN_REPAYMENT" -> handleLoanRepayment(event);
                default -> log.debug("Unhandled loan event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Failed to process loan event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.contribution-events:contribution-events}", groupId = "notification-group")
    public void handleContributionEvent(String message) {
        try {
            ContributionEvent event = objectMapper.readValue(message, ContributionEvent.class);
            log.info("Received contribution event: {} for member {}", event.getEventType(), event.getMemberName());

            switch (event.getEventType()) {
                case "CONTRIBUTION_RECEIVED", "CONTRIBUTION_PARTIAL" -> handleContributionReceived(event);
                case "CONTRIBUTION_DEFAULTED" -> handleContributionDefaulted(event);
                default -> log.debug("Unhandled contribution event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Failed to process contribution event: {}", e.getMessage(), e);
        }
    }

    // ==================== LOAN EVENT HANDLERS ====================

    private void handleLoanApplied(LoanEvent event) {
        // Notify admins and treasurers about new loan application
        List<UUID> adminUserIds = getAdminAndTreasurerUserIds(event.getGroupId());

        if (!adminUserIds.isEmpty()) {
            String amount = formatCurrency(event.getAmount());
            notificationService.notifyAdminsNewLoanApplication(
                    adminUserIds,
                    event.getMemberName(),
                    event.getLoanNumber(),
                    amount,
                    event.getLoanId(),
                    event.getGroupId()
            );
        }
    }

    private void handleLoanApproved(LoanEvent event) {
        // Notify member their loan was approved
        UUID userId = getUserIdByMemberId(event.getMemberId());
        if (userId != null) {
            String amount = formatCurrency(event.getAmount());
            notificationService.notifyLoanApproved(
                    event.getMemberId(),
                    userId,
                    event.getLoanNumber(),
                    amount,
                    event.getGroupId()
            );
        }
    }

    private void handleLoanRejected(LoanEvent event) {
        // Notify member their loan was rejected
        UUID userId = getUserIdByMemberId(event.getMemberId());
        if (userId != null) {
            notificationService.notifyLoanRejected(
                    event.getMemberId(),
                    userId,
                    event.getLoanNumber(),
                    null, // reason would come from event if available
                    event.getGroupId()
            );
        }
    }

    private void handleLoanDisbursed(LoanEvent event) {
        // Notify member their loan was disbursed
        UUID userId = getUserIdByMemberId(event.getMemberId());
        if (userId != null) {
            String amount = formatCurrency(event.getAmount());
            notificationService.notifyLoanDisbursed(
                    event.getMemberId(),
                    userId,
                    event.getLoanNumber(),
                    amount,
                    event.getGroupId()
            );
        }

        // Also notify admins about disbursement
        List<UUID> adminUserIds = getAdminAndTreasurerUserIds(event.getGroupId());
        // Filter out the current user if they're an admin
        UUID memberUserId = getUserIdByMemberId(event.getMemberId());
        if (memberUserId != null) {
            adminUserIds = adminUserIds.stream()
                    .filter(id -> !id.equals(memberUserId))
                    .collect(Collectors.toList());
        }
    }

    private void handleLoanRepayment(LoanEvent event) {
        // Notify admins about repayment
        List<UUID> adminUserIds = getAdminAndTreasurerUserIds(event.getGroupId());

        if (!adminUserIds.isEmpty()) {
            String amount = formatCurrency(event.getAmount());
            notificationService.notifyAdminsLoanRepayment(
                    adminUserIds,
                    event.getMemberName(),
                    event.getLoanNumber(),
                    amount,
                    event.getLoanId(),
                    event.getGroupId()
            );
        }

        // Notify member about their repayment confirmation
        UUID userId = getUserIdByMemberId(event.getMemberId());
        if (userId != null) {
            String amount = formatCurrency(event.getAmount());
            notificationService.notifyContributionConfirmed(
                    userId,
                    amount,
                    "Loan Repayment",
                    event.getGroupId()
            );
        }
    }

    // ==================== CONTRIBUTION EVENT HANDLERS ====================

    private void handleContributionReceived(ContributionEvent event) {
        // Notify admins about contribution
        List<UUID> adminUserIds = getAdminAndTreasurerUserIds(event.getGroupId());
        String cycleMonth = formatCycleMonth(event.getCycleMonth());

        if (!adminUserIds.isEmpty()) {
            String amount = formatCurrency(event.getPaidAmount());
            notificationService.notifyAdminsContributionReceived(
                    adminUserIds,
                    event.getMemberName(),
                    amount,
                    cycleMonth,
                    event.getContributionId(),
                    event.getGroupId()
            );
        }

        // Notify member about confirmation
        UUID userId = getUserIdByMemberId(event.getMemberId());
        if (userId != null) {
            String amount = formatCurrency(event.getPaidAmount());
            notificationService.notifyContributionConfirmed(
                    userId,
                    amount,
                    cycleMonth,
                    event.getGroupId()
            );
        }
    }

    private void handleContributionDefaulted(ContributionEvent event) {
        // Notify member about defaulted contribution
        UUID userId = getUserIdByMemberId(event.getMemberId());
        if (userId != null) {
            String amount = formatCurrency(event.getExpectedAmount());
            String cycleMonth = formatCycleMonth(event.getCycleMonth());
            notificationService.createNotification(
                    userId,
                    com.tablebanking.loanmanagement.entity.enums.NotificationType.CONTRIBUTION_DEFAULTED,
                    "Contribution Defaulted",
                    String.format("Your contribution of %s for %s has been marked as defaulted and converted to a loan.",
                            amount, cycleMonth),
                    "CONTRIBUTION",
                    event.getContributionId(),
                    null,
                    event.getGroupId()
            );
        }
    }

    // ==================== HELPER METHODS ====================

    private List<UUID> getAdminAndTreasurerUserIds(UUID groupId) {
        return userRepository.findByMemberGroupIdAndRoleIn(
                groupId,
                List.of(UserRole.ADMIN, UserRole.TREASURER)
        ).stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }

    private UUID getUserIdByMemberId(UUID memberId) {
        return userRepository.findByMemberId(memberId)
                .map(User::getId)
                .orElse(null);
    }

    private String formatCurrency(java.math.BigDecimal amount) {
        if (amount == null) return "KES 0";
        return "KES " + String.format("%,.0f", amount);
    }

    private String formatCycleMonth(java.time.LocalDate cycleMonth) {
        if (cycleMonth == null) return "Unknown";
        return cycleMonth.getMonth().toString() + " " + cycleMonth.getYear();
    }
}
