package com.tablebanking.loanmanagement.service;

import com.tablebanking.loanmanagement.dto.notification.NotificationCountDTO;
import com.tablebanking.loanmanagement.dto.notification.NotificationDTO;
import com.tablebanking.loanmanagement.entity.InAppNotification;
import com.tablebanking.loanmanagement.entity.enums.NotificationType;
import com.tablebanking.loanmanagement.repository.InAppNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InAppNotificationService {

    private final InAppNotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Create and broadcast a notification to a specific user
     */
    public InAppNotification createNotification(UUID userId, NotificationType type, String title,
                                                 String message, String referenceType, UUID referenceId,
                                                 String actorName, UUID groupId) {
        InAppNotification notification = InAppNotification.builder()
                .userId(userId)
                .groupId(groupId)
                .type(type)
                .title(title)
                .message(message)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .actorName(actorName)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Created notification for user {}: {}", userId, title);

        // Broadcast to user via WebSocket
        broadcastToUser(userId, notification);

        return notification;
    }

    /**
     * Create notifications for multiple users (e.g., all admins/treasurers)
     */
    public void createNotificationsForUsers(List<UUID> userIds, NotificationType type, String title,
                                            String message, String referenceType, UUID referenceId,
                                            String actorName, UUID groupId) {
        for (UUID userId : userIds) {
            createNotification(userId, type, title, message, referenceType, referenceId, actorName, groupId);
        }
    }

    /**
     * Broadcast notification to user via WebSocket
     */
    private void broadcastToUser(UUID userId, InAppNotification notification) {
        try {
            NotificationDTO dto = mapToDTO(notification);
            // Send to user-specific queue
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    dto
            );
            // Also send updated count
            long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notification-count",
                    NotificationCountDTO.builder().unreadCount(unreadCount).build()
            );
            log.debug("Broadcasted notification to user {}", userId);
        } catch (Exception e) {
            log.warn("Failed to broadcast notification to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Get paginated notifications for a user
     */
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToDTO);
    }

    /**
     * Get unread notification count for a user
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Mark a single notification as read
     */
    public void markAsRead(UUID notificationId, UUID userId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getUserId().equals(userId) && !notification.getIsRead()) {
                notification.markAsRead();
                notificationRepository.save(notification);
                // Send updated count
                broadcastUnreadCount(userId);
            }
        });
    }

    /**
     * Mark multiple notifications as read
     */
    public int markAsRead(List<UUID> notificationIds, UUID userId) {
        int count = notificationRepository.markAsReadByIds(notificationIds, userId, Instant.now());
        if (count > 0) {
            broadcastUnreadCount(userId);
        }
        return count;
    }

    /**
     * Mark all notifications as read for a user
     */
    public int markAllAsRead(UUID userId) {
        int count = notificationRepository.markAllAsReadByUserId(userId, Instant.now());
        if (count > 0) {
            broadcastUnreadCount(userId);
        }
        return count;
    }

    /**
     * Broadcast updated unread count to user
     */
    private void broadcastUnreadCount(UUID userId) {
        try {
            long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notification-count",
                    NotificationCountDTO.builder().unreadCount(unreadCount).build()
            );
        } catch (Exception e) {
            log.warn("Failed to broadcast count to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Map entity to DTO
     */
    private NotificationDTO mapToDTO(InAppNotification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .actorName(notification.getActorName())
                .build();
    }

    // ==================== CONVENIENCE METHODS FOR SPECIFIC EVENTS ====================

    /**
     * Notify member about loan application status
     */
    public void notifyLoanApproved(UUID memberId, UUID userId, String loanNumber, String amount, UUID groupId) {
        createNotification(
                userId,
                NotificationType.LOAN_APPROVED,
                "Loan Approved",
                String.format("Your loan application %s for %s has been approved.", loanNumber, amount),
                "LOAN",
                null,
                null,
                groupId
        );
    }

    public void notifyLoanRejected(UUID memberId, UUID userId, String loanNumber, String reason, UUID groupId) {
        createNotification(
                userId,
                NotificationType.LOAN_REJECTED,
                "Loan Rejected",
                String.format("Your loan application %s has been rejected. Reason: %s", loanNumber, reason != null ? reason : "Not specified"),
                "LOAN",
                null,
                null,
                groupId
        );
    }

    public void notifyLoanDisbursed(UUID memberId, UUID userId, String loanNumber, String amount, UUID groupId) {
        createNotification(
                userId,
                NotificationType.LOAN_DISBURSED,
                "Loan Disbursed",
                String.format("Your loan %s of %s has been disbursed to your account.", loanNumber, amount),
                "LOAN",
                null,
                null,
                groupId
        );
    }

    /**
     * Notify admins/treasurers about new loan application
     */
    public void notifyAdminsNewLoanApplication(List<UUID> adminUserIds, String memberName, String loanNumber,
                                                String amount, UUID loanId, UUID groupId) {
        for (UUID userId : adminUserIds) {
            createNotification(
                    userId,
                    NotificationType.LOAN_APPLICATION,
                    "New Loan Application",
                    String.format("%s has applied for a loan of %s. Review and approve.", memberName, amount),
                    "LOAN",
                    loanId,
                    memberName,
                    groupId
            );
        }
    }

    /**
     * Notify admins about loan repayment
     */
    public void notifyAdminsLoanRepayment(List<UUID> adminUserIds, String memberName, String loanNumber,
                                           String amount, UUID loanId, UUID groupId) {
        for (UUID userId : adminUserIds) {
            createNotification(
                    userId,
                    NotificationType.LOAN_REPAYMENT,
                    "Loan Repayment Received",
                    String.format("%s made a repayment of %s on loan %s.", memberName, amount, loanNumber),
                    "LOAN",
                    loanId,
                    memberName,
                    groupId
            );
        }
    }

    /**
     * Notify admins about contribution received
     */
    public void notifyAdminsContributionReceived(List<UUID> adminUserIds, String memberName, String amount,
                                                  String cycleMonth, UUID contributionId, UUID groupId) {
        for (UUID userId : adminUserIds) {
            createNotification(
                    userId,
                    NotificationType.CONTRIBUTION_RECEIVED,
                    "Contribution Received",
                    String.format("%s contributed %s for %s.", memberName, amount, cycleMonth),
                    "CONTRIBUTION",
                    contributionId,
                    memberName,
                    groupId
            );
        }
    }

    /**
     * Notify member about contribution confirmation
     */
    public void notifyContributionConfirmed(UUID userId, String amount, String cycleMonth, UUID groupId) {
        createNotification(
                userId,
                NotificationType.CONTRIBUTION_RECEIVED,
                "Contribution Confirmed",
                String.format("Your contribution of %s for %s has been recorded.", amount, cycleMonth),
                "CONTRIBUTION",
                null,
                null,
                groupId
        );
    }
}
