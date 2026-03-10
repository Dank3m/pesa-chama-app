package com.tablebanking.loanmanagement.service;

import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.BankingGroup;
import com.tablebanking.loanmanagement.repository.BankingGroupRepository;
import com.tablebanking.loanmanagement.repository.ContributionRepository;
import com.tablebanking.loanmanagement.repository.LoanRepository;
import com.tablebanking.loanmanagement.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SuperAdminService {

    private final BankingGroupRepository groupRepository;
    private final MemberRepository memberRepository;
    private final ContributionRepository contributionRepository;
    private final LoanRepository loanRepository;

    /**
     * Get summary of all groups for SUPER_ADMIN dashboard.
     */
    public AllGroupsSummaryResponse getAllGroupsSummary() {
        List<BankingGroup> allGroups = groupRepository.findAll();
        List<BankingGroup> activeGroups = allGroups.stream()
                .filter(BankingGroup::getIsActive)
                .toList();

        int totalMembers = 0;
        int activeMembers = 0;
        BigDecimal totalContributions = BigDecimal.ZERO;
        BigDecimal totalLoansOutstanding = BigDecimal.ZERO;

        List<GroupSummaryItem> groupSummaries = allGroups.stream()
                .map(this::mapToGroupSummaryItem)
                .collect(Collectors.toList());

        for (GroupSummaryItem summary : groupSummaries) {
            totalMembers += summary.getMemberCount();
            activeMembers += summary.getActiveMemberCount();
            if (summary.getTotalContributions() != null) {
                totalContributions = totalContributions.add(summary.getTotalContributions());
            }
            if (summary.getTotalLoansOutstanding() != null) {
                totalLoansOutstanding = totalLoansOutstanding.add(summary.getTotalLoansOutstanding());
            }
        }

        return AllGroupsSummaryResponse.builder()
                .totalGroups(allGroups.size())
                .activeGroups(activeGroups.size())
                .totalMembers(totalMembers)
                .activeMembers(activeMembers)
                .totalContributions(totalContributions)
                .totalLoansOutstanding(totalLoansOutstanding)
                .groups(groupSummaries)
                .build();
    }

    /**
     * Get all groups with their settings.
     */
    public List<GroupResponse> getAllGroups() {
        return groupRepository.findAllWithSettings().stream()
                .map(this::mapToGroupResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all active groups.
     */
    public List<GroupResponse> getActiveGroups() {
        return groupRepository.findAllActiveWithSettings().stream()
                .map(this::mapToGroupResponse)
                .collect(Collectors.toList());
    }

    private GroupSummaryItem mapToGroupSummaryItem(BankingGroup group) {
        int memberCount = memberRepository.countByGroupId(group.getId());
        int activeMemberCount = groupRepository.countActiveMembers(group.getId());

        // Get total contributions for the group
        BigDecimal totalContributions = contributionRepository.getTotalContributionsByGroupId(group.getId());
        if (totalContributions == null) {
            totalContributions = BigDecimal.ZERO;
        }

        // Get total outstanding loans for the group
        BigDecimal totalLoansOutstanding = loanRepository.getTotalOutstandingByGroupId(group.getId());
        if (totalLoansOutstanding == null) {
            totalLoansOutstanding = BigDecimal.ZERO;
        }

        return GroupSummaryItem.builder()
                .id(group.getId())
                .name(group.getName())
                .isActive(group.getIsActive())
                .memberCount(memberCount)
                .activeMemberCount(activeMemberCount)
                .contributionAmount(group.getContributionAmount())
                .interestRate(group.getInterestRate())
                .currency(group.getCurrency())
                .totalContributions(totalContributions)
                .totalLoansOutstanding(totalLoansOutstanding)
                .createdAt(group.getCreatedAt())
                .build();
    }

    private GroupResponse mapToGroupResponse(BankingGroup group) {
        int memberCount = groupRepository.countActiveMembers(group.getId());

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .contributionAmount(group.getContributionAmount())
                .currency(group.getCurrency())
                .interestRate(group.getInterestRate())
                .financialYearStartMonth(group.getFinancialYearStartMonth())
                .maxMembers(group.getMaxMembers())
                .currentMemberCount(memberCount)
                .isActive(group.getIsActive())
                .createdAt(group.getCreatedAt())
                .build();
    }
}
