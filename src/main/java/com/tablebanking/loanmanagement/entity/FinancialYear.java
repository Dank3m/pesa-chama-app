package com.tablebanking.loanmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "financial_years")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialYear extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private BankingGroup group;

    @Column(name = "year_name", nullable = false, length = 20)
    private String yearName; // e.g., "2024/2025"

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private Boolean isCurrent = false;

    @Column(name = "is_closed", nullable = false)
    @Builder.Default
    private Boolean isClosed = false;

    @Column(name = "total_contributions", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalContributions = BigDecimal.ZERO;

    @Column(name = "total_loans_disbursed", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalLoansDisbursed = BigDecimal.ZERO;

    @Column(name = "total_interest_earned", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalInterestEarned = BigDecimal.ZERO;

    @Column(name = "total_expenses", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalExpenses = BigDecimal.ZERO;

    @OneToMany(mappedBy = "financialYear", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ContributionCycle> contributionCycles = new ArrayList<>();

    @OneToMany(mappedBy = "financialYear", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Loan> loans = new ArrayList<>();

    public boolean isActive() {
        LocalDate now = LocalDate.now();
        return !isClosed && !now.isBefore(startDate) && !now.isAfter(endDate);
    }

    public void addContribution(BigDecimal amount) {
        this.totalContributions = this.totalContributions.add(amount);
    }

    public void addLoanDisbursement(BigDecimal amount) {
        this.totalLoansDisbursed = this.totalLoansDisbursed.add(amount);
    }

    public void addInterestEarned(BigDecimal amount) {
        this.totalInterestEarned = this.totalInterestEarned.add(amount);
    }

    public void addExpense(BigDecimal amount) {
        this.totalExpenses = this.totalExpenses.add(amount);
    }
}
