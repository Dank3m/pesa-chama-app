package com.tablebanking.loanmanagement.controller;

import com.tablebanking.loanmanagement.dto.notification.NotificationCountDTO;
import com.tablebanking.loanmanagement.dto.notification.NotificationDTO;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.ApiResponse;
import com.tablebanking.loanmanagement.entity.User;
import com.tablebanking.loanmanagement.repository.UserRepository;
import com.tablebanking.loanmanagement.service.InAppNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "In-App Notifications", description = "In-app notification management endpoints")
public class InAppNotificationController {

    private final InAppNotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get notifications for current user")
    public ResponseEntity<ApiResponse<Page<NotificationDTO>>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {

        UUID userId = getUserId(userDetails);
        Page<NotificationDTO> notifications = notificationService.getNotifications(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<ApiResponse<NotificationCountDTO>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = getUserId(userDetails);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(
                NotificationCountDTO.builder().unreadCount(count).build()
        ));
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = getUserId(userDetails);
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PostMapping("/mark-read")
    @Operation(summary = "Mark multiple notifications as read")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markMultipleAsRead(
            @RequestBody List<UUID> notificationIds,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = getUserId(userDetails);
        int count = notificationService.markAsRead(notificationIds, userId);
        return ResponseEntity.ok(ApiResponse.success(
                count + " notifications marked as read",
                Map.of("count", count)
        ));
    }

    @PostMapping("/mark-all-read")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = getUserId(userDetails);
        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(
                count + " notifications marked as read",
                Map.of("count", count)
        ));
    }

    private UUID getUserId(UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }
}
