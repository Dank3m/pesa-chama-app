package com.tablebanking.loanmanagement.entity.enums;

public enum NotificationType {
    // Loan notifications
    LOAN_APPLICATION,
    LOAN_APPROVED,
    LOAN_REJECTED,
    LOAN_DISBURSED,
    LOAN_REPAYMENT,
    LOAN_OVERDUE,

    // Contribution notifications
    CONTRIBUTION_RECEIVED,
    CONTRIBUTION_REMINDER,
    CONTRIBUTION_DEFAULTED,

    // Member notifications
    MEMBER_JOINED,
    MEMBER_REGISTRATION_PENDING,

    // Transaction notifications
    TRANSACTION_RECEIVED,
    DISBURSEMENT_PROCESSED,

    // System notifications
    SYSTEM_ALERT,
    ANNOUNCEMENT
}
