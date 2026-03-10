package com.tablebanking.loanmanagement.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StkPushRequestEvent {

    private String eventId;
    private String eventType;  // STK_PUSH_REQUEST
    private String collectionRef;
    private String collectionType;  // LOAN_REPAYMENT or CONTRIBUTION
    private UUID sourceId;
    private UUID memberId;
    private String memberName;
    private UUID groupId;
    private BigDecimal amount;
    private BigDecimal originalAmount; // Pre-rounding amount for M-Pesa (which only accepts whole numbers)
    private String phoneNumber;
    private Instant timestamp;
}
