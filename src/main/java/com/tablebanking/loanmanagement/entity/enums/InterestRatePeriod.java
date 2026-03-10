package com.tablebanking.loanmanagement.entity.enums;

/**
 * Defines the period for which the interest rate applies.
 * For example, if interestRate = 0.10 (10%) and period = MONTHLY,
 * it means 10% interest is charged per month.
 */
public enum InterestRatePeriod {
    DAILY,      // Interest rate applies per day
    WEEKLY,     // Interest rate applies per week
    MONTHLY,    // Interest rate applies per month
    YEARLY      // Interest rate applies per year (annual)
}
