package com.tablebanking.loanmanagement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Service for calculating loan interest using daily compounding
 * to achieve a target monthly rate of 10%.
 * 
 * The interest is earned each day such that after all days in the month,
 * the total interest compounds to exactly 10% for that month.
 */
@Service
@Slf4j
public class InterestCalculationService {

    private static final MathContext MATH_CONTEXT = new MathContext(15, RoundingMode.HALF_UP);
    private static final int SCALE = 8;

    /**
     * Calculate the daily interest rate that compounds to the target monthly rate.
     * 
     * Formula: dailyRate = (1 + monthlyRate)^(1/daysInMonth) - 1
     * 
     * For 10% monthly rate over 30 days:
     * dailyRate = (1.10)^(1/30) - 1 ≈ 0.00318 (0.318% daily)
     * 
     * @param monthlyRate The target monthly interest rate (e.g., 0.10 for 10%)
     * @param daysInMonth Number of days in the month
     * @return Daily interest rate as a decimal
     */
    public BigDecimal calculateDailyRate(BigDecimal monthlyRate, int daysInMonth) {
        // dailyRate = (1 + monthlyRate)^(1/daysInMonth) - 1
        double monthlyRateDouble = monthlyRate.doubleValue();
        double base = 1 + monthlyRateDouble;
        double exponent = 1.0 / daysInMonth;
        double dailyRate = Math.pow(base, exponent) - 1;
        
        return BigDecimal.valueOf(dailyRate).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate daily rate for a specific month and year.
     */
    public BigDecimal calculateDailyRateForMonth(BigDecimal monthlyRate, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();
        return calculateDailyRate(monthlyRate, daysInMonth);
    }

    /**
     * Calculate daily rate for the current date.
     */
    public BigDecimal calculateDailyRateForDate(BigDecimal monthlyRate, LocalDate date) {
        YearMonth yearMonth = YearMonth.from(date);
        int daysInMonth = yearMonth.lengthOfMonth();
        return calculateDailyRate(monthlyRate, daysInMonth);
    }

    /**
     * Calculate interest for a single day using compound interest.
     * 
     * @param principal The principal/balance on which to calculate interest
     * @param dailyRate The daily interest rate
     * @return Interest amount for that day
     */
    public BigDecimal calculateDailyInterest(BigDecimal principal, BigDecimal dailyRate) {
        return principal.multiply(dailyRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate the total interest over a period with daily compounding.
     * 
     * @param principal Starting principal
     * @param dailyRate Daily interest rate
     * @param days Number of days
     * @return Total interest accrued
     */
    public BigDecimal calculateCompoundInterest(BigDecimal principal, BigDecimal dailyRate, int days) {
        // Final = Principal * (1 + dailyRate)^days
        // Interest = Final - Principal
        
        double rate = dailyRate.doubleValue();
        double principalDouble = principal.doubleValue();
        double finalAmount = principalDouble * Math.pow(1 + rate, days);
        double interest = finalAmount - principalDouble;
        
        return BigDecimal.valueOf(interest).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate the balance after a number of days with daily compounding.
     * 
     * @param principal Starting principal
     * @param dailyRate Daily interest rate
     * @param days Number of days
     * @return Final balance
     */
    public BigDecimal calculateBalanceAfterDays(BigDecimal principal, BigDecimal dailyRate, int days) {
        double rate = dailyRate.doubleValue();
        double principalDouble = principal.doubleValue();
        double finalAmount = principalDouble * Math.pow(1 + rate, days);
        
        return BigDecimal.valueOf(finalAmount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate interest for a date range, accounting for varying days per month.
     * 
     * @param principal Starting principal
     * @param monthlyRate Monthly interest rate
     * @param startDate Start date
     * @param endDate End date (inclusive)
     * @return Total interest accrued
     */
    public InterestCalculationResult calculateInterestForPeriod(
            BigDecimal principal, 
            BigDecimal monthlyRate, 
            LocalDate startDate, 
            LocalDate endDate) {
        
        BigDecimal currentBalance = principal;
        BigDecimal totalInterest = BigDecimal.ZERO;
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            BigDecimal dailyRate = calculateDailyRateForDate(monthlyRate, currentDate);
            BigDecimal dailyInterest = calculateDailyInterest(currentBalance, dailyRate);
            
            currentBalance = currentBalance.add(dailyInterest);
            totalInterest = totalInterest.add(dailyInterest);
            currentDate = currentDate.plusDays(1);
        }
        
        return InterestCalculationResult.builder()
                .startingPrincipal(principal)
                .endingBalance(currentBalance)
                .totalInterest(totalInterest)
                .daysCalculated((int) ChronoUnit.DAYS.between(startDate, endDate) + 1)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    /**
     * Calculate expected total at end of loan term.
     * 
     * @param principal Loan principal
     * @param monthlyRate Monthly interest rate
     * @param months Number of months
     * @return Expected total amount
     */
    public BigDecimal calculateExpectedTotal(BigDecimal principal, BigDecimal monthlyRate, int months) {
        // Assuming average 30 days per month for estimation
        double rate = monthlyRate.doubleValue();
        double principalDouble = principal.doubleValue();
        double finalAmount = principalDouble * Math.pow(1 + rate, months);
        
        return BigDecimal.valueOf(finalAmount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Verify that daily compounding reaches the target monthly rate.
     * This is useful for validation and testing.
     */
    public boolean verifyMonthlyRate(BigDecimal monthlyRate, int daysInMonth) {
        BigDecimal dailyRate = calculateDailyRate(monthlyRate, daysInMonth);
        BigDecimal principal = new BigDecimal("10000.00");
        BigDecimal finalBalance = calculateBalanceAfterDays(principal, dailyRate, daysInMonth);
        BigDecimal actualRate = finalBalance.subtract(principal)
                .divide(principal, SCALE, RoundingMode.HALF_UP);
        
        // Check if actual rate is within 0.01% of target
        BigDecimal tolerance = new BigDecimal("0.0001");
        BigDecimal difference = actualRate.subtract(monthlyRate).abs();
        
        boolean verified = difference.compareTo(tolerance) <= 0;
        
        log.debug("Monthly rate verification: target={}, actual={}, difference={}, verified={}",
                monthlyRate, actualRate, difference, verified);
        
        return verified;
    }

    /**
     * Result of interest calculation for a period.
     */
    @lombok.Data
    @lombok.Builder
    public static class InterestCalculationResult {
        private BigDecimal startingPrincipal;
        private BigDecimal endingBalance;
        private BigDecimal totalInterest;
        private int daysCalculated;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
