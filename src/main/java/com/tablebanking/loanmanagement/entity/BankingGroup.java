package com.tablebanking.loanmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "banking_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankingGroup extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "contribution_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal contributionAmount = new BigDecimal("3500.00");

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "KES";

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal interestRate = new BigDecimal("0.10"); // 10%

    @Column(name = "financial_year_start_month", nullable = false)
    @Builder.Default
    private Integer financialYearStartMonth = 12; // December

    @Column(name = "financial_year_start_day", nullable = false)
    @Builder.Default
    private Integer financialYearStartDay = 1;

    @Column(name = "max_members")
    @Builder.Default
    private Integer maxMembers = 50;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Member> members = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FinancialYear> financialYears = new ArrayList<>();

    public void addMember(Member member) {
        members.add(member);
        member.setGroup(this);
    }

    public void removeMember(Member member) {
        members.remove(member);
        member.setGroup(null);
    }
}
