package com.tablebanking.loanmanagement.entity;

import com.tablebanking.loanmanagement.entity.enums.GuarantorStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a member who guarantees a loan.
 * A loan can have multiple guarantors, and a member can guarantee multiple loans.
 */
@Entity
@Table(name = "loan_guarantors", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"loan_id", "member_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanGuarantor extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * The amount this guarantor is responsible for.
     * If null, guarantor is responsible for entire loan.
     */
    @Column(name = "guaranteed_amount", precision = 15, scale = 2)
    private BigDecimal guaranteedAmount;

    /**
     * Percentage of loan this guarantor covers (0-100).
     * Used when multiple guarantors share responsibility.
     */
    @Column(name = "guarantee_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal guaranteePercentage = new BigDecimal("100.00");

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private GuarantorStatus status = GuarantorStatus.ACTIVE;

    /**
     * Date when the member agreed to be guarantor.
     */
    @Column(name = "accepted_at")
    private Instant acceptedAt;

    /**
     * Date when guarantor was released (loan paid off or transferred).
     */
    @Column(name = "released_at")
    private Instant releasedAt;

    /**
     * Amount the guarantor has paid on behalf of borrower (in case of default).
     */
    @Column(name = "amount_paid_on_behalf", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal amountPaidOnBehalf = BigDecimal.ZERO;

    @Column(name = "notes")
    private String notes;

    /**
     * Calculate the effective guaranteed amount based on loan balance.
     */
    public BigDecimal getEffectiveGuaranteedAmount() {
        if (guaranteedAmount != null) {
            return guaranteedAmount;
        }
        if (loan != null && guaranteePercentage != null) {
            return loan.getOutstandingBalance()
                    .multiply(guaranteePercentage)
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        }
        return loan != null ? loan.getOutstandingBalance() : BigDecimal.ZERO;
    }

    /**
     * Check if this guarantor is still active/liable.
     */
    public boolean isActive() {
        return status == GuarantorStatus.ACTIVE && releasedAt == null;
    }

    /**
     * Release the guarantor (loan paid off).
     */
    public void release() {
        this.status = GuarantorStatus.RELEASED;
        this.releasedAt = Instant.now();
    }
}
