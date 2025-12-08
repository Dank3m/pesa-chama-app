package com.tablebanking.loanmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "member_balances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberBalance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_year_id", nullable = false)
    private FinancialYear financialYear;

    @Column(name = "total_contributions", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalContributions = BigDecimal.ZERO;

    @Column(name = "total_loans_taken", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalLoansTaken = BigDecimal.ZERO;

    @Column(name = "total_loan_repayments", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalLoanRepayments = BigDecimal.ZERO;

    @Column(name = "outstanding_loan_balance", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal outstandingLoanBalance = BigDecimal.ZERO;

    @Column(name = "share_value", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal shareValue = BigDecimal.ZERO;

    @Column(name = "last_calculated_at", nullable = false)
    @Builder.Default
    private Instant lastCalculatedAt = Instant.now();

    public void addContribution(BigDecimal amount) {
        this.totalContributions = this.totalContributions.add(amount);
        this.lastCalculatedAt = Instant.now();
    }

    public void addLoan(BigDecimal amount) {
        this.totalLoansTaken = this.totalLoansTaken.add(amount);
        this.outstandingLoanBalance = this.outstandingLoanBalance.add(amount);
        this.lastCalculatedAt = Instant.now();
    }

    public void addRepayment(BigDecimal amount) {
        this.totalLoanRepayments = this.totalLoanRepayments.add(amount);
        this.outstandingLoanBalance = this.outstandingLoanBalance.subtract(amount);
        if (this.outstandingLoanBalance.compareTo(BigDecimal.ZERO) < 0) {
            this.outstandingLoanBalance = BigDecimal.ZERO;
        }
        this.lastCalculatedAt = Instant.now();
    }

    public void recalculateShareValue(BigDecimal totalGroupValue, int totalActiveMembers) {
        if (totalActiveMembers > 0 && totalGroupValue.compareTo(BigDecimal.ZERO) > 0) {
            this.shareValue = totalGroupValue.divide(
                BigDecimal.valueOf(totalActiveMembers), 
                2, 
                java.math.RoundingMode.HALF_UP
            );
        }
        this.lastCalculatedAt = Instant.now();
    }
}
