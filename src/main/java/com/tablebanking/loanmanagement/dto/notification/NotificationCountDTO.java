package com.tablebanking.loanmanagement.dto.notification;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCountDTO {
    private long unreadCount;
}
