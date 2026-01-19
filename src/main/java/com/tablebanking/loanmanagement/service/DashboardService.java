package com.tablebanking.loanmanagement.service;

import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.*;
import com.tablebanking.loanmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final MemberRepository memberRepository;
    private final ContributionRepository contributionRepository;
    private final ContributionCycleRepository cycleRepository;
    private final LoanRepository loanRepository;
    private final FinancialYearRepository financialYearRepository;

    public DashboardResponse getOverview(UUID groupId, UUID financialYearId) {
        UUID fyId = financialYearId != null ? financialYearId
                : financialYearRepository.findCurrentByGroupId(groupId)
                .map(FinancialYear::getId).orElse(null);

        BigDecimal totalContributions = orZero(contributionRepository.sumPaidByGroupAndYear(groupId, fyId));
        BigDecimal totalDisbursements = orZero(loanRepository.sumDisbursedByGroupAndYear(groupId, fyId));
        BigDecimal totalInterest = orZero(loanRepository.sumInterestByGroup(groupId));

        BigDecimal totalBalance = totalContributions.add(totalInterest).subtract(totalDisbursements);

        return DashboardResponse.builder()
                .totalBalance(totalBalance)
                .totalContributions(totalContributions)
                .activeLoans(loanRepository.countActiveByGroup(groupId))
                .memberCount(memberRepository.countActiveByGroup(groupId))
                .collectionRate(calculateCollectionRate(groupId))
                .monthlyActivity(getMonthlyActivity(groupId))
                .fundAllocation(getFundAllocation(totalContributions, totalDisbursements, totalInterest))
                .recentTransactions(getRecentTransactions(groupId, 5))
                .build();
    }

    public MemberDashboardResponse getMemberDashboard(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        return MemberDashboardResponse.builder()
                .memberId(memberId)
                .memberName(member.getFullName())
                .totalContributions(orZero(contributionRepository.sumPaidByMember(memberId)))
                .outstandingLoans(orZero(loanRepository.sumOutstandingByMember(memberId)))
                .activeLoansCount(loanRepository.countActiveByMember(memberId))
                .build();
    }

    private BigDecimal calculateCollectionRate(UUID groupId) {
            return cycleRepository.findCurrentByGroup(groupId)
                .map(cycle -> {
                    BigDecimal expected = orZero(contributionRepository.sumExpectedByCycle(cycle.getId()));
                    BigDecimal collected = orZero(contributionRepository.sumPaidByCycle(cycle.getId()));
                    if (expected.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
                    return collected.divide(expected, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP);
                })
                .orElse(BigDecimal.ZERO);
    }

    private List<MonthlyActivityDTO> getMonthlyActivity(UUID groupId) {
        List<MonthlyActivityDTO> activity = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = 11; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            activity.add(MonthlyActivityDTO.builder()
                    .name(month.format(DateTimeFormatter.ofPattern("MMM")))
                    .month(month.toString())
                    .contributions(orZero(contributionRepository.sumPaidByGroupAndMonth(groupId, month.getYear(), month.getMonthValue())))
                    .disbursements(orZero(loanRepository.sumDisbursedByGroupAndMonth(groupId, month.getYear(), month.getMonthValue())))
                    .build());
        }
        return activity;
    }

    private List<FundAllocationDTO> getFundAllocation(BigDecimal contributions, BigDecimal disbursements, BigDecimal interest) {
        BigDecimal total = contributions.add(interest).add(disbursements);
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return List.of(
                    new FundAllocationDTO("Loans Disbursed", 40, "#2D60FF"),
                    new FundAllocationDTO("Interest Earned", 30, "#16DBCC"),
                    new FundAllocationDTO("Expenses", 15, "#FFBB38"),
                    new FundAllocationDTO("Available", 15, "#FF82AC")
            );
        }

        BigDecimal available = total.subtract(disbursements);
        return List.of(
                new FundAllocationDTO("Loans Disbursed", pct(disbursements, total), "#2D60FF"),
                new FundAllocationDTO("Interest Earned", pct(interest, total), "#16DBCC"),
                new FundAllocationDTO("Expenses", 0, "#FFBB38"),
                new FundAllocationDTO("Available", pct(available, total), "#FF82AC")
        );
    }

    private List<TransactionDTO> getRecentTransactions(UUID groupId, int limit) {
        List<TransactionDTO> transactions = new ArrayList<>();

        contributionRepository.findRecentPaidByGroup(groupId, PageRequest.of(0, limit))
                .forEach(c -> transactions.add(new TransactionDTO(
                        c.getId().toString(), "CONTRIBUTION", c.getPaidAmount(),
                        c.getPaymentDate() != null ? c.getPaymentDate().toString() : "",
                        "Monthly Contribution", c.getMember().getFullName(), "Contribution")));

        loanRepository.findRecentDisbursedByGroup(groupId, PageRequest.of(0, limit))
                .forEach(l -> transactions.add(new TransactionDTO(
                        l.getId().toString(), "DISBURSEMENT", l.getPrincipalAmount(),
                        l.getDisbursementDate().toString(),
                        "Loan - " + l.getLoanNumber(), l.getBorrowerName(), "Loan")));

        return transactions.stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private BigDecimal orZero(BigDecimal val) { return val != null ? val : BigDecimal.ZERO; }
    private int pct(BigDecimal val, BigDecimal total) {
        return total.compareTo(BigDecimal.ZERO) == 0 ? 0
                : val.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue();
    }
}
