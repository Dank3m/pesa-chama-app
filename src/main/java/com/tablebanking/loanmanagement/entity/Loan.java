package com.tablebanking.loanmanagement.entity;

import com.tablebanking.loanmanagement.entity.enums.LoanStatus;
import com.tablebanking.loanmanagement.entity.enums.LoanType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan extends BaseEntity {

    @Column(name = "loan_number", nullable = false, unique = true, length = 30)
    private String loanNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_year_id", nullable = false)
    private FinancialYear financialYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 30)
    private LoanType loanType;

    @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate; // Monthly rate as decimal (0.10 = 10%)

    @Column(name = "daily_interest_rate", nullable = false, precision = 10, scale = 8)
    private BigDecimal dailyInterestRate; // Calculated daily rate for compounding

    @Column(name = "disbursement_date", nullable = false)
    private LocalDate disbursementDate;

    @Column(name = "expected_end_date", nullable = false)
    private LocalDate expectedEndDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "total_interest_accrued", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalInterestAccrued = BigDecimal.ZERO;

    @Column(name = "total_amount_due", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmountDue;

    @Column(name = "total_amount_paid", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalAmountPaid = BigDecimal.ZERO;

    @Column(name = "outstanding_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal outstandingBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private LoanStatus status = LoanStatus.PENDING;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_contribution_id")
    private Contribution sourceContribution; // If from defaulted contribution

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LoanInterestAccrual> interestAccruals = new ArrayList<>();

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LoanRepayment> repayments = new ArrayList<>();

    /**
     * External borrower (for guaranteed loans to non-members).
     * If set, this is a guaranteed loan; member_id should be null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "external_borrower_id")
    private ExternalBorrower externalBorrower;

    /**
     * List of guarantors for this loan.
     */
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LoanGuarantor> guarantors = new ArrayList<>();

    /**
     * Check if this is a guaranteed loan (to non-member).
     */
    public boolean isGuaranteedLoan() {
        return loanType == LoanType.GUARANTEED && externalBorrower != null;
    }

    /**
     * Get the borrower name (works for both member and external borrower).
     */
    public String getBorrowerName() {
        if (externalBorrower != null) {
            return externalBorrower.getFullName();
        }
        if (member != null) {
            return member.getFullName();
        }
        return "Unknown";
    }

    /**
     * Get primary guarantor (first guarantor or the one with highest percentage).
     */
    public LoanGuarantor getPrimaryGuarantor() {
        return guarantors.stream()
                .filter(LoanGuarantor::isActive)
                .max((g1, g2) -> g1.getGuaranteePercentage().compareTo(g2.getGuaranteePercentage()))
                .orElse(null);
    }

    /**
     * Add a guarantor to this loan.
     */
    public void addGuarantor(LoanGuarantor guarantor) {
        guarantors.add(guarantor);
        guarantor.setLoan(this);
    }

    /**
     * Release all guarantors (when loan is paid off).
     */
    public void releaseAllGuarantors() {
        guarantors.forEach(LoanGuarantor::release);
    }


    public boolean isActive() {
        return status == LoanStatus.ACTIVE || status == LoanStatus.DISBURSED;
    }

    public boolean isPaidOff() {
        return outstandingBalance.compareTo(BigDecimal.ZERO) <= 0;
    }

    public void accrueInterest(BigDecimal interest) {
        this.totalInterestAccrued = this.totalInterestAccrued.add(interest);
        this.outstandingBalance = this.outstandingBalance.add(interest);
        this.totalAmountDue = this.totalAmountDue.add(interest);
    }

    public void makePayment(BigDecimal amount) {
        this.totalAmountPaid = this.totalAmountPaid.add(amount);
        this.outstandingBalance = this.outstandingBalance.subtract(amount);
        
        if (this.outstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            this.outstandingBalance = BigDecimal.ZERO;
            this.status = LoanStatus.PAID_OFF;
            this.actualEndDate = LocalDate.now();
        }
    }

    public int getDaysActive() {
        LocalDate endDate = actualEndDate != null ? actualEndDate : LocalDate.now();
        return (int) java.time.temporal.ChronoUnit.DAYS.between(disbursementDate, endDate);
    }
}
