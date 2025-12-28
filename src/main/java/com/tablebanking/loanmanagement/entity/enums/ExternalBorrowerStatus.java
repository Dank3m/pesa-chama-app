package com.tablebanking.loanmanagement.entity.enums;

public enum ExternalBorrowerStatus {
    ACTIVE,      // Can receive loans
    SUSPENDED,   // Temporarily cannot receive loans
    BLACKLISTED, // Permanently banned (e.g., defaulted without resolution)
    INACTIVE     // No longer borrowing
}