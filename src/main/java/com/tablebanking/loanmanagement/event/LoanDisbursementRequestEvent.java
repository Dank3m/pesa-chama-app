package com.tablebanking.loanmanagement.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanDisbursementRequestEvent {

    private String eventId;
    private String eventType;  // LOAN_DISBURSEMENT_REQUEST

    private UUID loanId;
    private String loanNumber;
    private UUID memberId;
    private String memberName;
    private String phoneNumber;
    private String bankAccount;
    private String bankCode;
    private UUID groupId;
    private String groupName;

    private BigDecimal amount;
    private String disbursementMethod;  // MPESA, BANK

    private Instant timestamp;
}
