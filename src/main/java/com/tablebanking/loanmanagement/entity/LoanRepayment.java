package com.tablebanking.loanmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loan_repayments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRepayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(name = "payment_number", nullable = false)
    private Integer paymentNumber;

    @Column(name = "payment_date", nullable = false)
    private Instant paymentDate;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "principal_portion", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalPortion;

    @Column(name = "interest_portion", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestPortion;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "reference_number", length = 50)
    private String referenceNumber;

    @Column(name = "received_by")
    private UUID receivedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
