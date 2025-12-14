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
public class LoanEvent {
    private String eventId;
    private String eventType;
    private UUID loanId;
    private String loanNumber;
    private UUID memberId;
    private String phoneNumber;
    private String email;
    private UUID groupId;
    private String groupName;
    private LocalDate dueDate;
    private LocalDate disbursementDate;
    private String memberName;
    private BigDecimal amount;
    private BigDecimal outstandingBalance;
    private String status;
    private Instant timestamp;
}
