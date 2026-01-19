package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.InAppNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InAppNotificationRepository extends JpaRepository<InAppNotification, UUID> {

    // Get notifications for a user, ordered by newest first
    Page<InAppNotification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Get unread notifications for a user
    List<InAppNotification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);

    // Count unread notifications for a user
    long countByUserIdAndIsReadFalse(UUID userId);

    // Mark all notifications as read for a user
    @Modifying
    @Query("UPDATE InAppNotification n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") UUID userId, @Param("readAt") Instant readAt);

    // Mark specific notifications as read
    @Modifying
    @Query("UPDATE InAppNotification n SET n.isRead = true, n.readAt = :readAt WHERE n.id IN :ids AND n.userId = :userId")
    int markAsReadByIds(@Param("ids") List<UUID> ids, @Param("userId") UUID userId, @Param("readAt") Instant readAt);

    // Delete old read notifications (for cleanup)
    @Modifying
    @Query("DELETE FROM InAppNotification n WHERE n.isRead = true AND n.readAt < :before")
    int deleteOldReadNotifications(@Param("before") Instant before);

    // Get notifications by group (for admin broadcast)
    List<InAppNotification> findByGroupIdOrderByCreatedAtDesc(UUID groupId, Pageable pageable);
}
