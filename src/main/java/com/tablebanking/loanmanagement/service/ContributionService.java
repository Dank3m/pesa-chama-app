package com.tablebanking.loanmanagement.service;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.*;
import com.tablebanking.loanmanagement.entity.enums.*;
import com.tablebanking.loanmanagement.event.ContributionEvent;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContributionService {

    private final ContributionRepository contributionRepository;
    private final ContributionCycleRepository cycleRepository;
    private final MemberRepository memberRepository;
    private final FinancialYearRepository financialYearRepository;
    private final MemberBalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;
    private final BankingGroupRepository groupRepository;
    private final LoanService loanService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.contribution-events:contribution-events}")
    private String contributionEventsTopic;

    /**
     * Record a contribution payment from a member.
     */
    @CacheEvict(value = {"memberBalance", "cycleContributions"}, allEntries = true)
    public ContributionResponse recordContribution(RecordContributionRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new BusinessException("Member not found"));

        ContributionCycle cycle = cycleRepository.findById(request.getCycleId())
                .orElseThrow(() -> new BusinessException("Contribution cycle not found"));

        if (cycle.getStatus() == CycleStatus.CLOSED) {
            throw new BusinessException("Cannot record contributions for a closed cycle");
        }

        // Get or create contribution record
        Contribution contribution = contributionRepository
                .findByMemberIdAndCycleId(member.getId(), cycle.getId())
                .orElseGet(() -> createContribution(member, cycle));

        if (contribution.isFullyPaid()) {
            throw new BusinessException("Contribution for this cycle is already fully paid");
        }

        BigDecimal amountToRecord = request.getAmount();
        BigDecimal outstanding = contribution.getOutstandingAmount();

        if (amountToRecord.compareTo(outstanding) > 0) {
            log.warn("Payment {} exceeds outstanding {}. Recording outstanding amount only.",
                    amountToRecord, outstanding);
            amountToRecord = outstanding;
        }

        // Update contribution
        contribution.addPayment(amountToRecord);
        contribution.setPaymentDate(Instant.now());
        contribution.setNotes(request.getNotes());
        contribution = contributionRepository.save(contribution);

        // Update cycle total
        cycle.addContribution(amountToRecord);
        cycleRepository.save(cycle);

        // Update member balance
        updateMemberBalance(member, cycle.getFinancialYear(), amountToRecord);

        // Update financial year total
        FinancialYear year = cycle.getFinancialYear();
        year.addContribution(amountToRecord);
        financialYearRepository.save(year);

        // Create transaction record
        createContributionTransaction(member, cycle, contribution, amountToRecord, request.getReferenceNumber());

        // Publish event
        publishContributionEvent(contribution, "CONTRIBUTION_RECEIVED");

        log.info("Recorded contribution: Member={}, Cycle={}, Amount={}, Status={}",
                member.getMemberNumber(), cycle.getCycleMonth(), amountToRecord, contribution.getStatus());

        return mapToContributionResponse(contribution);
    }

    /**
     * Create contribution records for all active members in a cycle.
     */
    public List<Contribution> initializeCycleContributions(ContributionCycle cycle) {
        BankingGroup group = cycle.getFinancialYear().getGroup();
        List<Member> activeMembers = memberRepository.findActiveMembers(group.getId());

        BigDecimal expectedAmount = group.getContributionAmount();

        List<Contribution> contributions = activeMembers.stream()
                .filter(member -> !contributionRepository
                        .findByMemberIdAndCycleId(member.getId(), cycle.getId()).isPresent())
                .map(member -> {
                    Contribution contribution = Contribution.builder()
                            .member(member)
                            .cycle(cycle)
                            .expectedAmount(expectedAmount)
                            .status(ContributionStatus.PENDING)
                            .build();
                    return contributionRepository.save(contribution);
                })
                .collect(Collectors.toList());

        log.info("Initialized {} contributions for cycle {}", contributions.size(), cycle.getCycleMonth());

        return contributions;
    }

    /**
     * Process defaulted contributions at end of cycle.
     * Converts unpaid/partially paid contributions to loans.
     */
    @CacheEvict(value = {"memberBalance", "memberLoans"}, allEntries = true)
    public int processDefaultedContributions(UUID cycleId) {
        ContributionCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new BusinessException("Cycle not found"));

        if (!cycle.isPastDue()) {
            throw new BusinessException("Cycle is not yet past due date");
        }

        List<Contribution> defaultedContributions = contributionRepository
                .findDefaultedContributions(cycleId);

        int processedCount = 0;

        for (Contribution contribution : defaultedContributions) {
            if (contribution.getOutstandingAmount().compareTo(BigDecimal.ZERO) > 0) {
                try {
                    // Convert to loan
                    loanService.createLoanFromDefaultedContribution(contribution);
                    processedCount++;

                    publishContributionEvent(contribution, "CONTRIBUTION_DEFAULTED");

                    log.info("Converted defaulted contribution to loan: Member={}, Amount={}",
                            contribution.getMember().getMemberNumber(),
                            contribution.getOutstandingAmount());
                } catch (Exception e) {
                    log.error("Failed to convert contribution to loan for member {}: {}",
                            contribution.getMember().getMemberNumber(), e.getMessage());
                }
            }
        }

        // Mark cycle as processed
        cycle.setIsProcessed(true);
        cycle.setProcessedAt(Instant.now());
        cycle.setStatus(CycleStatus.CLOSED);
        cycleRepository.save(cycle);

        log.info("Processed {} defaulted contributions for cycle {}", processedCount, cycle.getCycleMonth());

        return processedCount;
    }

    /**
     * Get contribution by ID.
     */
    @Transactional(readOnly = true)
    public ContributionResponse getContributionById(UUID id) {
        Contribution contribution = contributionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Contribution not found"));
        return mapToContributionResponse(contribution);
    }

    /**
     * Get contributions by member.
     */
    @Transactional(readOnly = true)
    public List<ContributionResponse> getContributionsByMember(UUID memberId) {
        return contributionRepository.findByMemberId(memberId).stream()
                .map(this::mapToContributionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get contributions by cycle.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "cycleContributions", key = "#cycleId")
    public List<ContributionResponse> getContributionsByCycle(UUID cycleId) {
        return contributionRepository.findByCycleIdWithMembers(cycleId).stream()
                .map(this::mapToContributionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get contribution status for a member in a specific cycle.
     */
    @Transactional(readOnly = true)
    public ContributionResponse getMemberContributionForCycle(UUID memberId, UUID cycleId) {
        Contribution contribution = contributionRepository
                .findByMemberIdAndCycleId(memberId, cycleId)
                .orElseThrow(() -> new BusinessException("Contribution not found for this cycle"));
        return mapToContributionResponse(contribution);
    }

    /**
     * Get pending contributions for a cycle.
     */
    @Transactional(readOnly = true)
    public List<ContributionResponse> getPendingContributions(UUID cycleId) {
        return contributionRepository.findUnpaidContributions(cycleId).stream()
                .map(this::mapToContributionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new contribution cycle.
     */
    public ContributionCycleResponse createContributionCycle(UUID financialYearId, LocalDate cycleMonth) {
        FinancialYear year = financialYearRepository.findById(financialYearId)
                .orElseThrow(() -> new BusinessException("Financial year not found"));

        // Normalize to first day of month
        LocalDate normalizedMonth = cycleMonth.withDayOfMonth(1);

        // Check if cycle already exists
        if (cycleRepository.findByFinancialYearIdAndCycleMonth(financialYearId, normalizedMonth).isPresent()) {
            throw new BusinessException("Contribution cycle already exists for this month");
        }

        // Calculate due date (last day of month)
        LocalDate dueDate = normalizedMonth.with(TemporalAdjusters.lastDayOfMonth());

        ContributionCycle cycle = ContributionCycle.builder()
                .financialYear(year)
                .cycleMonth(normalizedMonth)
                .dueDate(dueDate)
                .expectedAmount(year.getGroup().getContributionAmount())
                .status(CycleStatus.OPEN)
                .build();

        cycle = cycleRepository.save(cycle);

        // Initialize contributions for all active members
        initializeCycleContributions(cycle);

        log.info("Created contribution cycle for {} with due date {}", normalizedMonth, dueDate);

        return mapToCycleResponse(cycle);
    }

    /**
     * Get current contribution cycle for a group.
     */
    @Transactional(readOnly = true)
    public ContributionCycleResponse getCurrentCycle(UUID groupId) {
        ContributionCycle cycle = cycleRepository
                .findCurrentCycleByGroupId(groupId, LocalDate.now())
                .or(() -> cycleRepository.findLatestOpenCycleByGroupId(groupId))
                .orElseThrow(() -> new BusinessException("No active contribution cycle found"));

        return mapToCycleResponse(cycle);
    }

    /**
     * Get all cycles for a financial year.
     */
    @Transactional(readOnly = true)
    public List<ContributionCycleResponse> getCyclesByYear(UUID financialYearId) {
        return cycleRepository.findByFinancialYearIdOrderByCycleMonthAsc(financialYearId).stream()
                .map(this::mapToCycleResponse)
                .collect(Collectors.toList());
    }

    // Private helper methods

    private Contribution createContribution(Member member, ContributionCycle cycle) {
        return Contribution.builder()
                .member(member)
                .cycle(cycle)
                .expectedAmount(cycle.getExpectedAmount())
                .status(ContributionStatus.PENDING)
                .build();
    }

    private void updateMemberBalance(Member member, FinancialYear year, BigDecimal amount) {
        MemberBalance balance = balanceRepository
                .findByMemberIdAndFinancialYearId(member.getId(), year.getId())
                .orElseGet(() -> MemberBalance.builder()
                        .member(member)
                        .financialYear(year)
                        .build());

        balance.addContribution(amount);
        balanceRepository.save(balance);
    }

    private void createContributionTransaction(Member member, ContributionCycle cycle,
                                               Contribution contribution, BigDecimal amount, String reference) {
        Transaction transaction = Transaction.builder()
                .transactionNumber(generateTransactionNumber())
                .group(member.getGroup())
                .member(member)
                .financialYear(cycle.getFinancialYear())
                .transactionType(TransactionType.CONTRIBUTION)
                .amount(amount)
                .debitCredit("CREDIT")
                .referenceType("CONTRIBUTION")
                .referenceId(contribution.getId())
                .description("Monthly contribution for " + cycle.getCycleMonth())
                .build();

        transactionRepository.save(transaction);
    }

    private String generateTransactionNumber() {
        String datePart = LocalDate.now().toString().replace("-", "");
        String seqPart = String.format("%06d", System.nanoTime() % 1000000);
        return "TXN" + datePart + seqPart;
    }

    private void publishContributionEvent(Contribution contribution, String eventType) {
        try {
            ContributionEvent event = ContributionEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .contributionId(contribution.getId())
                    .memberId(contribution.getMember().getId())
                    .memberName(contribution.getMember().getFullName())
                    .cycleMonth(contribution.getCycle().getCycleMonth())
                    .expectedAmount(contribution.getExpectedAmount())
                    .paidAmount(contribution.getPaidAmount())
                    .status(contribution.getStatus().name())
                    .timestamp(Instant.now())
                    .build();

            kafkaTemplate.send(contributionEventsTopic, contribution.getId().toString(), event);
            log.debug("Published contribution event: {} for contribution: {}", eventType, contribution.getId());
        } catch (Exception e) {
            log.error("Failed to publish contribution event: {}", e.getMessage());
        }
    }

    private ContributionResponse mapToContributionResponse(Contribution contribution) {
        return ContributionResponse.builder()
                .id(contribution.getId())
                .memberId(contribution.getMember().getId())
                .memberName(contribution.getMember().getFullName())
                .cycleId(contribution.getCycle().getId())
                .cycleMonth(contribution.getCycle().getCycleMonth())
                .expectedAmount(contribution.getExpectedAmount())
                .paidAmount(contribution.getPaidAmount())
                .outstandingAmount(contribution.getOutstandingAmount())
                .status(contribution.getStatus())
                .paymentDate(contribution.getPaymentDate())
                .convertedToLoan(contribution.getConvertedToLoan())
                .notes(contribution.getNotes())
                .build();
    }

    private ContributionCycleResponse mapToCycleResponse(ContributionCycle cycle) {
        long paidCount = contributionRepository.countByCycleIdAndStatus(cycle.getId(), ContributionStatus.PAID);
        long pendingCount = contributionRepository.countByCycleIdAndStatus(cycle.getId(), ContributionStatus.PENDING);
        long partialCount = contributionRepository.countByCycleIdAndStatus(cycle.getId(), ContributionStatus.PARTIAL);

        return ContributionCycleResponse.builder()
                .id(cycle.getId())
                .financialYearId(cycle.getFinancialYear().getId())
                .cycleMonth(cycle.getCycleMonth())
                .dueDate(cycle.getDueDate())
                .expectedAmount(cycle.getExpectedAmount())
                .status(cycle.getStatus())
                .totalCollected(cycle.getTotalCollected())
                .totalMembers((int) (paidCount + pendingCount + partialCount))
                .paidCount((int) paidCount)
                .pendingCount((int) (pendingCount + partialCount))
                .isProcessed(cycle.getIsProcessed())
                .build();
    }
}