package com.tablebanking.loanmanagement.entity.enums;

public enum GuarantorStatus {
    PENDING,    // Awaiting guarantor acceptance
    ACTIVE,     // Actively guaranteeing the loan
    RELEASED,   // Loan paid off, guarantor released
    DEFAULTED,  // Guarantor had to pay on behalf of borrower
    DECLINED    // Guarantor declined to guarantee
}