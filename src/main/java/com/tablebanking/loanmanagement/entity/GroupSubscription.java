package com.tablebanking.loanmanagement.entity;

import com.tablebanking.loanmanagement.entity.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "group_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupSubscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private BankingGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "auto_renew", nullable = false)
    @Builder.Default
    private Boolean autoRenew = true;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "is_grandfathered", nullable = false)
    @Builder.Default
    private Boolean isGrandfathered = false;

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }

    public boolean isExpired() {
        if (endDate == null) {
            return false; // No end date means never expires (grandfathered or free)
        }
        return LocalDate.now().isAfter(endDate);
    }

    public boolean isExpiringSoon(int daysThreshold) {
        if (endDate == null) {
            return false;
        }
        LocalDate warningDate = LocalDate.now().plusDays(daysThreshold);
        return !isExpired() && endDate.isBefore(warningDate);
    }

    public void cancel(String reason) {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancellationReason = reason;
        this.autoRenew = false;
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
    }

    public void suspend() {
        this.status = SubscriptionStatus.SUSPENDED;
    }

    public void reactivate() {
        this.status = SubscriptionStatus.ACTIVE;
    }
}
