package com.tablebanking.loanmanagement.dto.notification;

import com.tablebanking.loanmanagement.entity.enums.NotificationType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private UUID id;
    private NotificationType type;
    private String title;
    private String message;
    private String referenceType;
    private UUID referenceId;
    private Boolean isRead;
    private Instant createdAt;
    private Instant readAt;
    private String actorName;
}
