package com.tablebanking.loanmanagement.service;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.*;
import com.tablebanking.loanmanagement.entity.enums.*;
import com.tablebanking.loanmanagement.event.LoanEvent;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final FinancialYearRepository financialYearRepository;
    private final ContributionRepository contributionRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final LoanInterestAccrualRepository accrualRepository;
    private final MemberBalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;
    private final InterestCalculationService interestCalculationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.loan.default-interest-rate:0.10}")
    private BigDecimal defaultInterestRate;

    @Value("${app.loan.max-loan-duration-months:12}")
    private int maxLoanDurationMonths;

    @Value("${app.kafka.topics.loan-events:loan-events}")
    private String loanEventsTopic;

    private static final AtomicLong loanSequence = new AtomicLong(System.currentTimeMillis() % 1000000);

    /**
     * Apply for a regular loan.
     */
    public LoanResponse applyForLoan(LoanApplicationRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new BusinessException("Member not found"));

        if (!member.isActive()) {
            throw new BusinessException("Only active members can apply for loans");
        }

        // Check for existing active loans
        long activeLoans = loanRepository.countActiveLoansByMember(member.getId());
        if (activeLoans > 0) {
            throw new BusinessException("Member has existing active loans. Please clear them first.");
        }

        // Get current financial year
        FinancialYear currentYear = financialYearRepository
                .findCurrentByGroupId(member.getGroup().getId())
                .orElseThrow(() -> new BusinessException("No active financial year found"));

        // Validate loan amount against member's contribution history
        validateLoanAmount(member, request.getAmount(), currentYear);

        int durationMonths = request.getDurationMonths() != null ?
                request.getDurationMonths() : maxLoanDurationMonths;

        if (durationMonths > maxLoanDurationMonths) {
            throw new BusinessException("Maximum loan duration is " + maxLoanDurationMonths + " months");
        }

        BigDecimal interestRate = member.getGroup().getInterestRate();
        BigDecimal dailyRate = interestCalculationService.calculateDailyRateForDate(
                interestRate, LocalDate.now());

        Loan loan = Loan.builder()
                .loanNumber(generateLoanNumber())
                .member(member)
                .financialYear(currentYear)
                .loanType(LoanType.REGULAR)
                .principalAmount(request.getAmount())
                .interestRate(interestRate)
                .dailyInterestRate(dailyRate)
                .disbursementDate(LocalDate.now())
                .expectedEndDate(LocalDate.now().plusMonths(durationMonths))
                .totalAmountDue(request.getAmount())
                .outstandingBalance(request.getAmount())
                .status(LoanStatus.PENDING)
                .notes(request.getNotes())
                .build();

        loan = loanRepository.save(loan);

        // Publish event
        publishLoanEvent(loan, "LOAN_APPLIED");

        log.info("Loan application created: {} for member: {}", loan.getLoanNumber(), member.getMemberNumber());

        return mapToLoanResponse(loan);
    }

    /**
     * Approve a pending loan.
     */
    @CacheEvict(value = {"memberLoans", "memberBalance"}, allEntries = true)
    public LoanResponse approveLoan(ApproveLoanRequest request, UUID approvedBy) {
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new BusinessException("Loan not found"));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new BusinessException("Only pending loans can be approved");
        }

        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovedBy(approvedBy);
        loan.setApprovedAt(Instant.now());

        if (request.getNotes() != null) {
            loan.setNotes(loan.getNotes() + "\nApproval: " + request.getNotes());
        }

        loan = loanRepository.save(loan);

        publishLoanEvent(loan, "LOAN_APPROVED");

        log.info("Loan approved: {}", loan.getLoanNumber());

        return mapToLoanResponse(loan);
    }

    /**
     * Disburse an approved loan.
     */
    @CacheEvict(value = {"memberLoans", "memberBalance"}, allEntries = true)
    public LoanResponse disburseLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new BusinessException("Loan not found"));

        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new BusinessException("Only approved loans can be disbursed");
        }

        loan.setStatus(LoanStatus.DISBURSED);
        loan.setDisbursementDate(LocalDate.now());

        // Recalculate daily rate for disbursement date
        BigDecimal dailyRate = interestCalculationService.calculateDailyRateForDate(
                loan.getInterestRate(), LocalDate.now());
        loan.setDailyInterestRate(dailyRate);

        loan = loanRepository.save(loan);

        // Update member balance
        updateMemberBalanceForDisbursement(loan);

        // Create transaction record
        createDisbursementTransaction(loan);

        // Update financial year totals
        loan.getFinancialYear().addLoanDisbursement(loan.getPrincipalAmount());
        financialYearRepository.save(loan.getFinancialYear());

        publishLoanEvent(loan, "LOAN_DISBURSED");

        log.info("Loan disbursed: {} Amount: {}", loan.getLoanNumber(), loan.getPrincipalAmount());

        return mapToLoanResponse(loan);
    }

    /**
     * Create a loan from a defaulted contribution.
     */
    @CacheEvict(value = {"memberLoans", "memberBalance"}, allEntries = true)
    public Loan createLoanFromDefaultedContribution(Contribution contribution) {
        Member member = contribution.getMember();
        FinancialYear financialYear = contribution.getCycle().getFinancialYear();
        BigDecimal defaultAmount = contribution.getOutstandingAmount();

        BigDecimal interestRate = member.getGroup().getInterestRate();
        BigDecimal dailyRate = interestCalculationService.calculateDailyRateForDate(
                interestRate, LocalDate.now());

        Loan loan = Loan.builder()
                .loanNumber(generateLoanNumber())
                .member(member)
                .financialYear(financialYear)
                .loanType(LoanType.CONTRIBUTION_DEFAULT)
                .principalAmount(defaultAmount)
                .interestRate(interestRate)
                .dailyInterestRate(dailyRate)
                .disbursementDate(LocalDate.now())
                .expectedEndDate(LocalDate.now().plusMonths(maxLoanDurationMonths))
                .totalAmountDue(defaultAmount)
                .outstandingBalance(defaultAmount)
                .status(LoanStatus.ACTIVE) // Auto-activated for defaults
                .sourceContribution(contribution)
                .notes("Auto-generated from defaulted contribution for " +
                        contribution.getCycle().getCycleMonth())
                .build();

        loan = loanRepository.save(loan);

        // Update contribution
        contribution.setConvertedToLoan(true);
        contribution.setLoan(loan);
        contribution.setStatus(ContributionStatus.CONVERTED_TO_LOAN);
        contributionRepository.save(contribution);

        // Update member balance
        updateMemberBalanceForDisbursement(loan);

        // Create transaction
        createDisbursementTransaction(loan);

        // Update financial year
        financialYear.addLoanDisbursement(loan.getPrincipalAmount());
        financialYearRepository.save(financialYear);

        publishLoanEvent(loan, "LOAN_CREATED_FROM_DEFAULT");

        log.info("Created loan {} from defaulted contribution for member {}",
                loan.getLoanNumber(), member.getMemberNumber());

        return loan;
    }

    /**
     * Process loan repayment.
     */
    @CacheEvict(value = {"memberLoans", "memberBalance"}, allEntries = true)
    public LoanRepaymentResponse makeRepayment(LoanRepaymentRequest request, UUID receivedBy) {
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new BusinessException("Loan not found"));

        if (!loan.isActive()) {
            throw new BusinessException("Can only make payments on active loans");
        }

        BigDecimal paymentAmount = request.getAmount();
        if (paymentAmount.compareTo(loan.getOutstandingBalance()) > 0) {
            paymentAmount = loan.getOutstandingBalance();
        }

        // Allocate payment to interest first, then principal
        BigDecimal interestPortion;
        BigDecimal principalPortion;

        if (loan.getTotalInterestAccrued().compareTo(loan.getTotalAmountPaid()) > 0) {
            BigDecimal unpaidInterest = loan.getTotalInterestAccrued()
                    .subtract(calculatePaidInterest(loan));
            interestPortion = paymentAmount.min(unpaidInterest);
            principalPortion = paymentAmount.subtract(interestPortion);
        } else {
            interestPortion = BigDecimal.ZERO;
            principalPortion = paymentAmount;
        }

        int paymentNumber = repaymentRepository.getMaxPaymentNumber(loan.getId()) + 1;

        // Update loan
        loan.makePayment(paymentAmount);
        loan = loanRepository.save(loan);

        // Create repayment record
        LoanRepayment repayment = LoanRepayment.builder()
                .loan(loan)
                .paymentNumber(paymentNumber)
                .paymentDate(Instant.now())
                .amount(paymentAmount)
                .principalPortion(principalPortion)
                .interestPortion(interestPortion)
                .balanceAfter(loan.getOutstandingBalance())
                .paymentMethod(request.getPaymentMethod())
                .referenceNumber(request.getReferenceNumber())
                .receivedBy(receivedBy)
                .notes(request.getNotes())
                .build();

        repayment = repaymentRepository.save(repayment);

        // Update member balance
        updateMemberBalanceForRepayment(loan, paymentAmount);

        // Create transaction
        createRepaymentTransaction(loan, repayment);

        publishLoanEvent(loan, loan.isPaidOff() ? "LOAN_PAID_OFF" : "LOAN_PAYMENT_RECEIVED");

        log.info("Repayment processed for loan {}: Amount={}, Balance={}",
                loan.getLoanNumber(), paymentAmount, loan.getOutstandingBalance());

        return mapToRepaymentResponse(repayment);
    }

    /**
     * Accrue daily interest for a loan.
     */
    public LoanInterestAccrual accrueInterestForLoan(Loan loan, LocalDate accrualDate) {
        // Check if already accrued for this date
        if (accrualRepository.existsByLoanIdAndAccrualDate(loan.getId(), accrualDate)) {
            log.debug("Interest already accrued for loan {} on {}", loan.getLoanNumber(), accrualDate);
            return null;
        }

        BigDecimal dailyRate = interestCalculationService.calculateDailyRateForDate(
                loan.getInterestRate(), accrualDate);

        BigDecimal openingBalance = loan.getOutstandingBalance();
        BigDecimal interestAmount = interestCalculationService.calculateDailyInterest(
                openingBalance, dailyRate);
        BigDecimal closingBalance = openingBalance.add(interestAmount);

        LoanInterestAccrual accrual = LoanInterestAccrual.builder()
                .loan(loan)
                .accrualDate(accrualDate)
                .openingBalance(openingBalance)
                .interestAmount(interestAmount)
                .closingBalance(closingBalance)
                .isCompounded(true)
                .build();

        accrual = accrualRepository.save(accrual);

        // Update loan
        loan.accrueInterest(interestAmount);
        loanRepository.save(loan);

        // Update financial year
        loan.getFinancialYear().addInterestEarned(interestAmount);
        financialYearRepository.save(loan.getFinancialYear());

        log.debug("Interest accrued for loan {}: Date={}, Amount={}, NewBalance={}",
                loan.getLoanNumber(), accrualDate, interestAmount, closingBalance);

        return accrual;
    }

    /**
     * Get loan by ID.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "loan", key = "#loanId")
    public LoanResponse getLoanById(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new BusinessException("Loan not found"));
        return mapToLoanResponse(loan);
    }

    /**
     * Get loan with full details.
     */
    @Transactional(readOnly = true)
    public LoanDetailResponse getLoanDetails(UUID loanId) {
        Loan loan = loanRepository.findByIdWithRepayments(loanId)
                .orElseThrow(() -> new BusinessException("Loan not found"));

        List<LoanInterestAccrual> recentAccruals = accrualRepository
                .findByLoanIdOrderByAccrualDateAsc(loanId)
                .stream()
                .limit(30)
                .toList();

        return LoanDetailResponse.builder()
                .loan(mapToLoanResponse(loan))
                .repayments(loan.getRepayments().stream()
                        .map(this::mapToRepaymentResponse)
                        .collect(Collectors.toList()))
                .recentAccruals(recentAccruals.stream()
                        .map(this::mapToAccrualResponse)
                        .collect(Collectors.toList()))
                .schedule(calculateLoanSchedule(loan))
                .build();
    }

    /**
     * Get loans by member.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "memberLoans", key = "#memberId")
    public List<LoanResponse> getLoansByMember(UUID memberId) {
        return loanRepository.findByMemberId(memberId).stream()
                .map(this::mapToLoanResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get active loans by member.
     */
    @Transactional(readOnly = true)
    public List<LoanSummaryResponse> getActiveLoansByMember(UUID memberId) {
        return loanRepository.findActiveLoansByMemberId(memberId).stream()
                .map(this::mapToSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all active loans for interest accrual processing.
     */
    @Transactional(readOnly = true)
    public List<Loan> getLoansForInterestAccrual(LocalDate date) {
        return loanRepository.findLoansForInterestAccrual(date);
    }

    /**
     * Get overdue loans.
     */
    @Transactional(readOnly = true)
    public List<LoanResponse> getOverdueLoans() {
        return loanRepository.findOverdueLoans(LocalDate.now()).stream()
                .map(this::mapToLoanResponse)
                .collect(Collectors.toList());
    }

    // Private helper methods

    private void validateLoanAmount(Member member, BigDecimal requestedAmount, FinancialYear year) {
        // Members can borrow up to 3x their total contributions
        BigDecimal totalContributions = contributionRepository
                .getTotalContributionsByMemberAndYear(member.getId(), year.getId());

        BigDecimal maxLoanAmount = totalContributions.multiply(new BigDecimal("3"));

        if (requestedAmount.compareTo(maxLoanAmount) > 0) {
            throw new BusinessException(
                    String.format("Maximum loan amount is %.2f (3x your contributions of %.2f)",
                            maxLoanAmount, totalContributions));
        }
    }

    private String generateLoanNumber() {
        String datePart = LocalDate.now().toString().replace("-", "");
        String seqPart = String.format("%06d", loanSequence.incrementAndGet() % 1000000);
        return "LN" + datePart + seqPart;
    }

    private void updateMemberBalanceForDisbursement(Loan loan) {
        MemberBalance balance = balanceRepository
                .findByMemberIdAndFinancialYearId(loan.getMember().getId(), loan.getFinancialYear().getId())
                .orElseGet(() -> MemberBalance.builder()
                        .member(loan.getMember())
                        .financialYear(loan.getFinancialYear())
                        .build());

        balance.addLoan(loan.getPrincipalAmount());
        balanceRepository.save(balance);
    }

    private void updateMemberBalanceForRepayment(Loan loan, BigDecimal amount) {
        MemberBalance balance = balanceRepository
                .findByMemberIdAndFinancialYearId(loan.getMember().getId(), loan.getFinancialYear().getId())
                .orElseThrow(() -> new BusinessException("Member balance not found"));

        balance.addRepayment(amount);
        balanceRepository.save(balance);
    }

    private void createDisbursementTransaction(Loan loan) {
        Transaction transaction = Transaction.builder()
                .transactionNumber(generateTransactionNumber())
                .group(loan.getMember().getGroup())
                .member(loan.getMember())
                .financialYear(loan.getFinancialYear())
                .transactionType(TransactionType.LOAN_DISBURSEMENT)
                .amount(loan.getPrincipalAmount())
                .debitCredit("DEBIT")
                .referenceType("LOAN")
                .referenceId(loan.getId())
                .description("Loan disbursement: " + loan.getLoanNumber())
                .build();

        transactionRepository.save(transaction);
    }

    private void createRepaymentTransaction(Loan loan, LoanRepayment repayment) {
        Transaction transaction = Transaction.builder()
                .transactionNumber(generateTransactionNumber())
                .group(loan.getMember().getGroup())
                .member(loan.getMember())
                .financialYear(loan.getFinancialYear())
                .transactionType(TransactionType.LOAN_REPAYMENT)
                .amount(repayment.getAmount())
                .debitCredit("CREDIT")
                .referenceType("REPAYMENT")
                .referenceId(repayment.getId())
                .description("Loan repayment: " + loan.getLoanNumber() + " #" + repayment.getPaymentNumber())
                .build();

        transactionRepository.save(transaction);
    }

    private String generateTransactionNumber() {
        String datePart = LocalDate.now().toString().replace("-", "");
        String seqPart = String.format("%06d", System.nanoTime() % 1000000);
        return "TXN" + datePart + seqPart;
    }

    private BigDecimal calculatePaidInterest(Loan loan) {
        return repaymentRepository.findByLoanId(loan.getId()).stream()
                .map(LoanRepayment::getInterestPortion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LoanScheduleResponse calculateLoanSchedule(Loan loan) {
        BigDecimal estimatedTotalInterest = interestCalculationService.calculateExpectedTotal(
                        loan.getPrincipalAmount(),
                        loan.getInterestRate(),
                        (int) java.time.temporal.ChronoUnit.MONTHS.between(
                                loan.getDisbursementDate(), loan.getExpectedEndDate()))
                .subtract(loan.getPrincipalAmount());

        BigDecimal dailyInterestAmount = interestCalculationService.calculateDailyInterest(
                loan.getOutstandingBalance(), loan.getDailyInterestRate());

        return LoanScheduleResponse.builder()
                .totalPrincipal(loan.getPrincipalAmount())
                .estimatedTotalInterest(estimatedTotalInterest)
                .estimatedTotalPayable(loan.getPrincipalAmount().add(estimatedTotalInterest))
                .estimatedEndDate(loan.getExpectedEndDate())
                .dailyInterestAmount(dailyInterestAmount)
                .monthlyInterestRate(loan.getInterestRate().multiply(new BigDecimal("100")))
                .build();
    }

    private void publishLoanEvent(Loan loan, String eventType) {
        try {
            Member member = loan.getMember();
            BankingGroup group = member.getGroup();

            LoanEvent event = LoanEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .loanId(loan.getId())
                    .loanNumber(loan.getLoanNumber())
                    .memberId(member.getId())
                    .memberName(member.getFullName())
                    .phoneNumber(member.getPhoneNumber())
                    .email(member.getEmail())
                    .groupId(group.getId())
                    .groupName(group.getName())
                    .amount(loan.getPrincipalAmount())
                    .outstandingBalance(loan.getOutstandingBalance())
                    .dueDate(loan.getExpectedEndDate())
                    .disbursementDate(loan.getDisbursementDate())
                    .status(loan.getStatus().name())
                    .timestamp(Instant.now())
                    .build();

            kafkaTemplate.send(loanEventsTopic, loan.getId().toString(), event);
            log.debug("Published loan event: {} for loan: {}", eventType, loan.getLoanNumber());
        } catch (Exception e) {
            log.error("Failed to publish loan event: {}", e.getMessage());
        }
    }

    private LoanResponse mapToLoanResponse(Loan loan) {
        return LoanResponse.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .memberId(loan.getMember().getId())
                .memberName(loan.getMember().getFullName())
                .loanType(loan.getLoanType())
                .principalAmount(loan.getPrincipalAmount())
                .interestRate(loan.getInterestRate())
                .dailyInterestRate(loan.getDailyInterestRate())
                .disbursementDate(loan.getDisbursementDate())
                .expectedEndDate(loan.getExpectedEndDate())
                .actualEndDate(loan.getActualEndDate())
                .totalInterestAccrued(loan.getTotalInterestAccrued())
                .totalAmountDue(loan.getTotalAmountDue())
                .totalAmountPaid(loan.getTotalAmountPaid())
                .outstandingBalance(loan.getOutstandingBalance())
                .status(loan.getStatus())
                .daysActive(loan.getDaysActive())
                .createdAt(loan.getCreatedAt())
                .build();
    }

    private LoanSummaryResponse mapToSummaryResponse(Loan loan) {
        return LoanSummaryResponse.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .loanType(loan.getLoanType())
                .principalAmount(loan.getPrincipalAmount())
                .outstandingBalance(loan.getOutstandingBalance())
                .status(loan.getStatus())
                .disbursementDate(loan.getDisbursementDate())
                .build();
    }

    private LoanRepaymentResponse mapToRepaymentResponse(LoanRepayment repayment) {
        return LoanRepaymentResponse.builder()
                .id(repayment.getId())
                .loanId(repayment.getLoan().getId())
                .paymentNumber(repayment.getPaymentNumber())
                .paymentDate(repayment.getPaymentDate())
                .amount(repayment.getAmount())
                .principalPortion(repayment.getPrincipalPortion())
                .interestPortion(repayment.getInterestPortion())
                .balanceAfter(repayment.getBalanceAfter())
                .paymentMethod(repayment.getPaymentMethod())
                .referenceNumber(repayment.getReferenceNumber())
                .build();
    }

    private InterestAccrualResponse mapToAccrualResponse(LoanInterestAccrual accrual) {
        return InterestAccrualResponse.builder()
                .id(accrual.getId())
                .accrualDate(accrual.getAccrualDate())
                .openingBalance(accrual.getOpeningBalance())
                .interestAmount(accrual.getInterestAmount())
                .closingBalance(accrual.getClosingBalance())
                .build();
    }
}