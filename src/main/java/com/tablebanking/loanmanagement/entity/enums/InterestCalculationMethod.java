package com.tablebanking.loanmanagement.entity.enums;

/**
 * Interest calculation methods for loans.
 */
public enum InterestCalculationMethod {
    /**
     * Simple interest: Interest = Principal × Rate × Time
     * Interest calculated only on the original principal amount.
     */
    SIMPLE,

    /**
     * Daily compound interest: Interest compounded daily.
     * A = P(1 + r/365)^(365*t)
     */
    DAILY_COMPOUND,

    /**
     * Monthly compound interest: Interest compounded monthly.
     * A = P(1 + r/12)^(12*t)
     */
    MONTHLY_COMPOUND,

    /**
     * Flat rate: Fixed percentage of principal added as interest.
     * Total Interest = Principal × Rate (regardless of duration)
     */
    FLAT_RATE
}
