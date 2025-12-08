package com.tablebanking.loanmanagement.entity;

import com.tablebanking.loanmanagement.entity.enums.CycleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contribution_cycles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributionCycle extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_year_id", nullable = false)
    private FinancialYear financialYear;

    @Column(name = "cycle_month", nullable = false)
    private LocalDate cycleMonth; // First day of the month

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate; // Usually last day of month

    @Column(name = "expected_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal expectedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CycleStatus status = CycleStatus.OPEN;

    @Column(name = "total_collected", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalCollected = BigDecimal.ZERO;

    @Column(name = "is_processed", nullable = false)
    @Builder.Default
    private Boolean isProcessed = false;

    @Column(name = "processed_at")
    private Instant processedAt;

    @OneToMany(mappedBy = "cycle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Contribution> contributions = new ArrayList<>();

    public boolean isPastDue() {
        return LocalDate.now().isAfter(dueDate);
    }

    public void addContribution(BigDecimal amount) {
        this.totalCollected = this.totalCollected.add(amount);
    }

    public int getMonthNumber() {
        return cycleMonth.getMonthValue();
    }

    public int getYear() {
        return cycleMonth.getYear();
    }
}
