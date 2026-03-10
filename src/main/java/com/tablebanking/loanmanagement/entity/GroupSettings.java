package com.tablebanking.loanmanagement.entity;

import com.tablebanking.loanmanagement.entity.enums.InterestCalculationMethod;
import com.tablebanking.loanmanagement.entity.enums.InterestRatePeriod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "group_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false, unique = true)
    private BankingGroup group;

    // ==================== Financial Year Settings ====================

    @Column(name = "financial_year_start_month", nullable = false)
    @Builder.Default
    private Integer financialYearStartMonth = 12; // December

    @Column(name = "financial_year_end_month", nullable = false)
    @Builder.Default
    private Integer financialYearEndMonth = 11; // November

    // ==================== Contribution Settings ====================

    @Column(name = "default_contribution_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal defaultContributionAmount = new BigDecimal("3500.00");

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "KES";

    @Column(name = "allow_partial_contributions", nullable = false)
    @Builder.Default
    private Boolean allowPartialContributions = true;

    // ==================== Loan Settings ====================

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal interestRate = new BigDecimal("0.10"); // 10%

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_rate_period", nullable = false, length = 20)
    @Builder.Default
    private InterestRatePeriod interestRatePeriod = InterestRatePeriod.MONTHLY;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_calculation_method", nullable = false, length = 30)
    @Builder.Default
    private InterestCalculationMethod interestCalculationMethod = InterestCalculationMethod.SIMPLE;

    @Column(name = "max_loan_duration_months", nullable = false)
    @Builder.Default
    private Integer maxLoanDurationMonths = 12;

    @Column(name = "grace_period_days", nullable = false)
    @Builder.Default
    private Integer gracePeriodDays = 0;

    @Column(name = "max_loan_multiplier", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal maxLoanMultiplier = new BigDecimal("3.00");

    @Column(name = "require_guarantors", nullable = false)
    @Builder.Default
    private Boolean requireGuarantors = false;

    @Column(name = "min_guarantors", nullable = false)
    @Builder.Default
    private Integer minGuarantors = 1;

    // ==================== Scheduler Settings ====================

    @Column(name = "contribution_check_cron", length = 50)
    @Builder.Default
    private String contributionCheckCron = "0 0 9 L * ?"; // Last day of month at 9 AM

    @Column(name = "interest_accrual_cron", length = 50)
    @Builder.Default
    private String interestAccrualCron = "0 0 0 * * ?"; // Daily at midnight

    @Column(name = "overdue_check_cron", length = 50)
    @Builder.Default
    private String overdueCheckCron = "0 0 8 * * ?"; // Daily at 8 AM

    @Column(name = "reminder_days_before_due", nullable = false)
    @Builder.Default
    private Integer reminderDaysBeforeDue = 3;

    // ==================== Penalty Settings ====================

    @Column(name = "late_penalty_rate", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal latePenaltyRate = new BigDecimal("0.05"); // 5%

    @Column(name = "enable_penalties", nullable = false)
    @Builder.Default
    private Boolean enablePenalties = true;

    // ==================== Helper Methods ====================

    /**
     * Creates default settings for a group.
     */
    public static GroupSettings createDefaultForGroup(BankingGroup group) {
        return GroupSettings.builder()
                .group(group)
                .defaultContributionAmount(group.getContributionAmount())
                .interestRate(group.getInterestRate())
                .currency(group.getCurrency())
                .financialYearStartMonth(group.getFinancialYearStartMonth())
                .build();
    }
}
