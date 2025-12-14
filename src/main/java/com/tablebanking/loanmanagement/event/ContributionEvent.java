package com.tablebanking.loanmanagement.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionEvent {
    private String eventId;
    private String eventType;
    private UUID contributionId;
    private UUID memberId;
    private String phoneNumber;
    private String email;
    private UUID groupId;
    private String groupName;
    private LocalDate dueDate;
    private String memberName;
    private LocalDate cycleMonth;
    private BigDecimal expectedAmount;
    private BigDecimal paidAmount;
    private String status;
    private Instant timestamp;
}
