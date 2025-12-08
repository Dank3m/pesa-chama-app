package com.tablebanking.loanmanagement.service;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.*;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FinancialYearService {

    private final FinancialYearRepository financialYearRepository;
    private final BankingGroupRepository groupRepository;
    private final MemberRepository memberRepository;
    private final MemberBalanceRepository balanceRepository;
    private final ContributionCycleRepository cycleRepository;
    private final ContributionService contributionService;

    /**
     * Create a new financial year for a group.
     * Financial year runs from December to November.
     */
    @CacheEvict(value = "financialYear", allEntries = true)
    public FinancialYearResponse createFinancialYear(CreateFinancialYearRequest request) {
        BankingGroup group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new BusinessException("Banking group not found"));

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        // Validate dates
        if (endDate.isBefore(startDate) || endDate.equals(startDate)) {
            throw new BusinessException("End date must be after start date");
        }

        // Generate year name (e.g., "2024/2025")
        String yearName = request.getYearName();
        if (yearName == null || yearName.isBlank()) {
            yearName = startDate.getYear() + "/" + endDate.getYear();
        }

        // Check for duplicate year name
        if (financialYearRepository.existsByGroupIdAndYearName(group.getId(), yearName)) {
            throw new BusinessException("Financial year " + yearName + " already exists");
        }

        // Set previous current year to not current
        financialYearRepository.findCurrentByGroupId(group.getId())
                .ifPresent(current -> {
                    current.setIsCurrent(false);
                    financialYearRepository.save(current);
                });

        FinancialYear financialYear = FinancialYear.builder()
                .group(group)
                .yearName(yearName)
                .startDate(startDate)
                .endDate(endDate)
                .isCurrent(true)
                .isClosed(false)
                .build();

        financialYear = financialYearRepository.save(financialYear);

        // Initialize contribution cycles for each month
        createContributionCyclesForYear(financialYear);

        // Initialize member balances for all active members
        initializeMemberBalances(financialYear);

        log.info("Created financial year {} for group {}", yearName, group.getName());

        return mapToResponse(financialYear);
    }

    /**
     * Create default financial year starting in December.
     */
    public FinancialYearResponse createDefaultFinancialYear(UUID groupId) {
        LocalDate now = LocalDate.now();
        int startYear = now.getMonthValue() >= Month.DECEMBER.getValue() ? 
                now.getYear() : now.getYear() - 1;
        
        LocalDate startDate = LocalDate.of(startYear, Month.DECEMBER, 1);
        LocalDate endDate = LocalDate.of(startYear + 1, Month.NOVEMBER, 30);

        CreateFinancialYearRequest request = CreateFinancialYearRequest.builder()
                .groupId(groupId)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        return createFinancialYear(request);
    }

    /**
     * Get financial year by ID.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "financialYear", key = "#id")
    public FinancialYearResponse getFinancialYearById(UUID id) {
        FinancialYear year = financialYearRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Financial year not found"));
        return mapToResponse(year);
    }

    /**
     * Get current financial year for a group.
     */
    @Transactional(readOnly = true)
    public FinancialYearResponse getCurrentFinancialYear(UUID groupId) {
        FinancialYear year = financialYearRepository.findCurrentByGroupId(groupId)
                .orElseThrow(() -> new BusinessException("No current financial year found"));
        return mapToResponse(year);
    }

    /**
     * Get all financial years for a group.
     */
    @Transactional(readOnly = true)
    public List<FinancialYearResponse> getFinancialYearsByGroup(UUID groupId) {
        return financialYearRepository.findByGroupIdOrderByStartDateDesc(groupId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Close a financial year.
     */
    @CacheEvict(value = "financialYear", allEntries = true)
    public FinancialYearResponse closeFinancialYear(UUID yearId) {
        FinancialYear year = financialYearRepository.findById(yearId)
                .orElseThrow(() -> new BusinessException("Financial year not found"));

        if (year.getIsClosed()) {
            throw new BusinessException("Financial year is already closed");
        }

        // Process any remaining open cycles
        year.getContributionCycles().stream()
                .filter(cycle -> !cycle.getIsProcessed())
                .forEach(cycle -> {
                    try {
                        contributionService.processDefaultedContributions(cycle.getId());
                    } catch (Exception e) {
                        log.error("Error processing cycle {}: {}", cycle.getCycleMonth(), e.getMessage());
                    }
                });

        year.setIsClosed(true);
        year.setIsCurrent(false);
        year = financialYearRepository.save(year);

        log.info("Closed financial year: {}", year.getYearName());

        return mapToResponse(year);
    }

    /**
     * Get financial year summary with statistics.
     */
    @Transactional(readOnly = true)
    public FinancialYearSummary getFinancialYearSummary(UUID yearId) {
        FinancialYear year = financialYearRepository.findById(yearId)
                .orElseThrow(() -> new BusinessException("Financial year not found"));

        BigDecimal totalOutstanding = balanceRepository.getTotalOutstandingLoansByYear(yearId);
        BigDecimal netPosition = year.getTotalContributions()
                .add(year.getTotalInterestEarned())
                .subtract(year.getTotalLoansDisbursed())
                .add(totalOutstanding);

        return FinancialYearSummary.builder()
                .yearId(year.getId())
                .yearName(year.getYearName())
                .totalContributions(year.getTotalContributions())
                .totalLoansDisbursed(year.getTotalLoansDisbursed())
                .totalInterestEarned(year.getTotalInterestEarned())
                .totalOutstandingLoans(totalOutstanding)
                .netPosition(netPosition)
                .build();
    }

    // Private helper methods

    private void createContributionCyclesForYear(FinancialYear year) {
        LocalDate currentMonth = year.getStartDate().withDayOfMonth(1);
        LocalDate endMonth = year.getEndDate().withDayOfMonth(1);

        while (!currentMonth.isAfter(endMonth)) {
            LocalDate dueDate = currentMonth.with(TemporalAdjusters.lastDayOfMonth());
            
            ContributionCycle cycle = ContributionCycle.builder()
                    .financialYear(year)
                    .cycleMonth(currentMonth)
                    .dueDate(dueDate)
                    .expectedAmount(year.getGroup().getContributionAmount())
                    .build();

            cycleRepository.save(cycle);
            currentMonth = currentMonth.plusMonths(1);
        }

        log.info("Created {} contribution cycles for year {}", 
                12, year.getYearName());
    }

    private void initializeMemberBalances(FinancialYear year) {
        List<Member> activeMembers = memberRepository.findActiveMembers(year.getGroup().getId());
        
        for (Member member : activeMembers) {
            if (!balanceRepository.findByMemberIdAndFinancialYearId(member.getId(), year.getId()).isPresent()) {
                MemberBalance balance = MemberBalance.builder()
                        .member(member)
                        .financialYear(year)
                        .build();
                balanceRepository.save(balance);
            }
        }

        log.info("Initialized balances for {} members in year {}", 
                activeMembers.size(), year.getYearName());
    }

    private FinancialYearResponse mapToResponse(FinancialYear year) {
        return FinancialYearResponse.builder()
                .id(year.getId())
                .groupId(year.getGroup().getId())
                .yearName(year.getYearName())
                .startDate(year.getStartDate())
                .endDate(year.getEndDate())
                .isCurrent(year.getIsCurrent())
                .isClosed(year.getIsClosed())
                .totalContributions(year.getTotalContributions())
                .totalLoansDisbursed(year.getTotalLoansDisbursed())
                .totalInterestEarned(year.getTotalInterestEarned())
                .createdAt(year.getCreatedAt())
                .build();
    }
}
