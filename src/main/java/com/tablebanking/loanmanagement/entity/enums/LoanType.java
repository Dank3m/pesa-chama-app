package com.tablebanking.loanmanagement.entity.enums;

public enum LoanType {
    REGULAR,              // Standard loan to member
    CONTRIBUTION_DEFAULT, // Loan from defaulted contribution
    EMERGENCY,            // Emergency loan
    GUARANTEED            // Loan to non-member with member guarantor
}
