package com.tablebanking.loanmanagement.entity;

import com.tablebanking.loanmanagement.entity.enums.SubscriptionPaymentMethod;
import com.tablebanking.loanmanagement.entity.enums.SubscriptionPaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "subscription_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPayment extends BaseEntity {

    @Column(name = "payment_number", nullable = false, unique = true, length = 30)
    private String paymentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private GroupSubscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private BankingGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "KES";

    @Column(name = "payment_method", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private SubscriptionPaymentMethod paymentMethod;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SubscriptionPaymentStatus status = SubscriptionPaymentStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by")
    private User paidBy;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    public boolean isPending() {
        return status == SubscriptionPaymentStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == SubscriptionPaymentStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == SubscriptionPaymentStatus.FAILED;
    }

    public void markAsCompleted(String reference) {
        this.status = SubscriptionPaymentStatus.COMPLETED;
        this.paymentReference = reference;
        this.paidAt = Instant.now();
    }

    public void markAsFailed(String reason) {
        this.status = SubscriptionPaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public void markAsRefunded() {
        this.status = SubscriptionPaymentStatus.REFUNDED;
    }
}
