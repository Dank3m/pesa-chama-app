package com.tablebanking.loanmanagement.entity;

import com.tablebanking.loanmanagement.entity.enums.ContributionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "contributions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contribution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private ContributionCycle cycle;

    @Column(name = "expected_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "paid_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ContributionStatus status = ContributionStatus.PENDING;

    @Column(name = "payment_date")
    private Instant paymentDate;

    @Column(name = "converted_to_loan", nullable = false)
    @Builder.Default
    private Boolean convertedToLoan = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private Loan loan; // Reference to loan if converted

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public BigDecimal getOutstandingAmount() {
        return expectedAmount.subtract(paidAmount);
    }

    public boolean isFullyPaid() {
        return paidAmount.compareTo(expectedAmount) >= 0;
    }

    public void addPayment(BigDecimal amount) {
        this.paidAmount = this.paidAmount.add(amount);
        updateStatus();
    }

    private void updateStatus() {
        if (paidAmount.compareTo(expectedAmount) >= 0) {
            this.status = ContributionStatus.PAID;
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = ContributionStatus.PARTIAL;
        }
    }
}
