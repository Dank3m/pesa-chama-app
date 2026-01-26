package com.tablebanking.loanmanagement.service;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.*;
import com.tablebanking.loanmanagement.entity.enums.*;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GuaranteedLoanService {

    private final ExternalBorrowerRepository externalBorrowerRepository;
    private final LoanGuarantorRepository guarantorRepository;
    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final MemberBalanceRepository balanceRepository;
    private final FinancialYearRepository financialYearRepository;
    private final TransactionRepository transactionRepository;
    private final BankingGroupRepository groupRepository;
    private final FeatureGateService featureGateService;

    @Value("${app.loan.interest-rate:0.10}")
    private BigDecimal defaultInterestRate;

    @Value("${app.loan.max-guarantee-exposure-multiplier:3}")
    private int maxGuaranteeExposureMultiplier;

    // ==================== External Borrower Management ====================

    /**
     * Register a new external borrower.
     */
    public ExternalBorrowerResponse createExternalBorrower(CreateExternalBorrowerRequest request) {
        // Check if external loans feature is available for this group's subscription
        featureGateService.requireFeature(request.getGroupId(), "externalLoans");

        BankingGroup group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new BusinessException("Group not found"));

        // Check for duplicates
        if (externalBorrowerRepository.existsByGroupIdAndPhoneNumber(request.getGroupId(), request.getPhoneNumber())) {
            throw new BusinessException("An external borrower with this phone number already exists");
        }

        if (request.getNationalId() != null && 
            externalBorrowerRepository.existsByGroupIdAndNationalId(request.getGroupId(), request.getNationalId())) {
            throw new BusinessException("An external borrower with this national ID already exists");
        }

        ExternalBorrower borrower = ExternalBorrower.builder()
                .group(group)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .nationalId(request.getNationalId())
                .address(request.getAddress())
                .employer(request.getEmployer())
                .occupation(request.getOccupation())
                .status(ExternalBorrowerStatus.ACTIVE)
                .notes(request.getNotes())
                .build();

        borrower = externalBorrowerRepository.save(borrower);
        log.info("Created external borrower: {} ({})", borrower.getFullName(), borrower.getPhoneNumber());

        return mapToExternalBorrowerResponse(borrower);
    }

    /**
     * Get external borrower by ID.
     */
    @Transactional(readOnly = true)
    public ExternalBorrowerResponse getExternalBorrower(UUID borrowerId) {
        ExternalBorrower borrower = externalBorrowerRepository.findByIdWithLoans(borrowerId)
                .orElseThrow(() -> new BusinessException("External borrower not found"));
        return mapToExternalBorrowerResponse(borrower);
    }

    /**
     * Get all external borrowers for a group.
     */
    @Transactional(readOnly = true)
    public List<ExternalBorrowerResponse> getExternalBorrowers(UUID groupId) {
        return externalBorrowerRepository.findByGroupId(groupId).stream()
                .map(this::mapToExternalBorrowerResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update external borrower status.
     */
    public ExternalBorrowerResponse updateBorrowerStatus(UUID borrowerId, ExternalBorrowerStatus status) {
        ExternalBorrower borrower = externalBorrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new BusinessException("External borrower not found"));

        borrower.setStatus(status);
        borrower = externalBorrowerRepository.save(borrower);
        log.info("Updated external borrower status: {} -> {}", borrower.getFullName(), status);

        return mapToExternalBorrowerResponse(borrower);
    }

    // ==================== Guaranteed Loan Management ====================

    /**
     * Create a guaranteed loan for an external borrower.
     */
    @CacheEvict(value = {"memberBalance", "memberLoans"}, allEntries = true)
    public GuaranteedLoanResponse createGuaranteedLoan(CreateGuaranteedLoanRequest request) {
        // Validate external borrower
        ExternalBorrower borrower = externalBorrowerRepository.findById(request.getExternalBorrowerId())
                .orElseThrow(() -> new BusinessException("External borrower not found"));

        // Check if external loans feature is available for this group's subscription
        featureGateService.requireFeature(borrower.getGroup().getId(), "externalLoans");

        if (borrower.getStatus() != ExternalBorrowerStatus.ACTIVE) {
            throw new BusinessException("External borrower is not active. Status: " + borrower.getStatus());
        }

        // Validate guarantor member
        Member guarantor = memberRepository.findById(request.getGuarantorMemberId())
                .orElseThrow(() -> new BusinessException("Guarantor member not found"));

        if (guarantor.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException("Guarantor member is not active");
        }

        // Check guarantor's exposure limit
        validateGuarantorExposure(guarantor, request.getPrincipalAmount());

        // Get current financial year
        FinancialYear financialYear = financialYearRepository
                .findCurrentByGroupId(borrower.getGroup().getId())
                .orElseThrow(() -> new BusinessException("No active financial year found"));

        // Calculate interest and loan details
        BigDecimal interestRate = borrower.getGroup().getInterestRate();
        if (interestRate == null) {
            interestRate = defaultInterestRate;
        }
        BigDecimal dailyRate = interestRate.divide(BigDecimal.valueOf(30), 8, RoundingMode.HALF_UP);

        // Create the loan
        Loan loan = Loan.builder()
                .loanNumber(generateLoanNumber())
                .externalBorrower(borrower)
                .member(null)  // External loan, no member
                .financialYear(financialYear)
                .loanType(LoanType.GUARANTEED)
                .principalAmount(request.getPrincipalAmount())
                .interestRate(interestRate)
                .dailyInterestRate(dailyRate)
                .disbursementDate(request.getDisbursementDate())
                .expectedEndDate(financialYear.getEndDate())
                .totalAmountDue(request.getPrincipalAmount())
                .outstandingBalance(request.getPrincipalAmount())
                .status(LoanStatus.DISBURSED)
                .notes(request.getNotes())
                .build();

        loan = loanRepository.save(loan);

        // Create guarantor record
        BigDecimal guaranteePercentage = request.getGuaranteePercentage();
        if (guaranteePercentage == null) {
            guaranteePercentage = new BigDecimal("100.00");
        }

        LoanGuarantor loanGuarantor = LoanGuarantor.builder()
                .loan(loan)
                .member(guarantor)
                .guaranteePercentage(guaranteePercentage)
                .status(GuarantorStatus.ACTIVE)
                .acceptedAt(Instant.now())
                .build();

        guarantorRepository.save(loanGuarantor);
        loan.addGuarantor(loanGuarantor);

        // Update financial year totals
        financialYear.addLoanDisbursement(request.getPrincipalAmount());
        financialYearRepository.save(financialYear);

        // Create disbursement transaction
        createDisbursementTransaction(loan, borrower.getGroup(), financialYear);

        log.info("Created guaranteed loan: {} for borrower {} guaranteed by {}",
                loan.getLoanNumber(), borrower.getFullName(), guarantor.getFullName());

        return mapToGuaranteedLoanResponse(loan);
    }

    /**
     * Add additional guarantor to an existing loan.
     */
    public LoanGuarantorResponse addGuarantor(AddGuarantorRequest request) {
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new BusinessException("Loan not found"));

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new BusinessException("Member not found"));

        // Check if already a guarantor
        if (guarantorRepository.findByLoanIdAndMemberId(request.getLoanId(), request.getMemberId()).isPresent()) {
            throw new BusinessException("Member is already a guarantor for this loan");
        }

        // Validate exposure
        BigDecimal guaranteedAmount = request.getGuaranteedAmount();
        if (guaranteedAmount == null && request.getGuaranteePercentage() != null) {
            guaranteedAmount = loan.getOutstandingBalance()
                    .multiply(request.getGuaranteePercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        validateGuarantorExposure(member, guaranteedAmount != null ? guaranteedAmount : loan.getOutstandingBalance());

        LoanGuarantor guarantor = LoanGuarantor.builder()
                .loan(loan)
                .member(member)
                .guaranteedAmount(request.getGuaranteedAmount())
                .guaranteePercentage(request.getGuaranteePercentage() != null ? 
                        request.getGuaranteePercentage() : new BigDecimal("100.00"))
                .status(GuarantorStatus.ACTIVE)
                .acceptedAt(Instant.now())
                .notes(request.getNotes())
                .build();

        guarantor = guarantorRepository.save(guarantor);
        log.info("Added guarantor {} to loan {}", member.getFullName(), loan.getLoanNumber());

        return mapToGuarantorResponse(guarantor);
    }

    /**
     * Get member's guarantor exposure summary.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "guarantorExposure", key = "#memberId")
    public GuarantorExposureResponse getGuarantorExposure(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException("Member not found"));

        List<LoanGuarantor> activeGuarantees = guarantorRepository.findActiveGuaranteesByMember(memberId);
        BigDecimal totalExposure = guarantorRepository.calculateMemberExposure(memberId);
        BigDecimal totalPaidOnBehalf = guarantorRepository.calculateTotalPaidOnBehalf(memberId);

        return GuarantorExposureResponse.builder()
                .memberId(memberId)
                .memberName(member.getFullName())
                .activeGuaranteesCount(activeGuarantees.size())
                .totalGuaranteedAmount(totalExposure)
                .totalPaidOnBehalf(totalPaidOnBehalf)
                .activeGuarantees(activeGuarantees.stream()
                        .map(this::mapToGuarantorResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Get all guaranteed loans for a group.
     */
    @Transactional(readOnly = true)
    public List<GuaranteedLoanResponse> getGuaranteedLoans(UUID groupId) {
        return loanRepository.findByGroupIdAndLoanType(groupId, LoanType.GUARANTEED).stream()
                .map(this::mapToGuaranteedLoanResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get guaranteed loans by external borrower.
     */
    @Transactional(readOnly = true)
    public List<GuaranteedLoanResponse> getLoansByExternalBorrower(UUID borrowerId) {
        return loanRepository.findByExternalBorrowerId(borrowerId).stream()
                .map(this::mapToGuaranteedLoanResponse)
                .collect(Collectors.toList());
    }

    /**
     * Process guarantor liability when borrower defaults.
     * Transfers liability to guarantor(s).
     */
    @CacheEvict(value = {"memberBalance", "guarantorExposure"}, allEntries = true)
    public void processGuarantorLiability(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new BusinessException("Loan not found"));

        if (loan.getLoanType() != LoanType.GUARANTEED) {
            throw new BusinessException("Not a guaranteed loan");
        }

        List<LoanGuarantor> activeGuarantors = guarantorRepository.findActiveGuarantorsByLoan(loanId);
        if (activeGuarantors.isEmpty()) {
            throw new BusinessException("No active guarantors found for this loan");
        }

        BigDecimal outstandingBalance = loan.getOutstandingBalance();

        for (LoanGuarantor guarantor : activeGuarantors) {
            BigDecimal liability = guarantor.getEffectiveGuaranteedAmount();
            liability = liability.min(outstandingBalance); // Can't exceed outstanding

            // Record the amount guarantor is now liable for
            guarantor.setAmountPaidOnBehalf(liability);
            guarantor.setStatus(GuarantorStatus.DEFAULTED);
            guarantorRepository.save(guarantor);

            // Create a loan for the guarantor member (they now owe this amount)
            // This converts the guarantee into an actual debt
            Member member = guarantor.getMember();
            
            log.warn("Guarantor {} is now liable for {} from defaulted loan {}",
                    member.getFullName(), liability, loan.getLoanNumber());

            outstandingBalance = outstandingBalance.subtract(liability);
            if (outstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }

        // Update original loan status
        loan.setStatus(LoanStatus.DEFAULTED);
        loanRepository.save(loan);

        log.info("Processed guarantor liability for loan {}. Remaining unallocated: {}",
                loan.getLoanNumber(), outstandingBalance);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Validate that guarantor has capacity to guarantee this amount.
     */
    private void validateGuarantorExposure(Member guarantor, BigDecimal additionalAmount) {
        // Get member's total contributions (their stake in the group)
        BigDecimal memberContributions = balanceRepository
                .findLatestByMemberId(guarantor.getId())
                .map(MemberBalance::getTotalContributions)
                .orElse(BigDecimal.ZERO);

        // Get current exposure
        BigDecimal currentExposure = guarantorRepository.calculateMemberExposure(guarantor.getId());

        // Maximum allowed exposure = contributions * multiplier
        BigDecimal maxExposure = memberContributions.multiply(BigDecimal.valueOf(maxGuaranteeExposureMultiplier));

        BigDecimal newTotalExposure = currentExposure.add(additionalAmount);

        if (newTotalExposure.compareTo(maxExposure) > 0) {
            throw new BusinessException(String.format(
                    "Guarantor exposure limit exceeded. Current: %s, Additional: %s, Maximum: %s (based on contributions: %s x %d)",
                    currentExposure, additionalAmount, maxExposure, memberContributions, maxGuaranteeExposureMultiplier));
        }

        log.debug("Guarantor exposure check passed for {}: current={}, additional={}, max={}",
                guarantor.getFullName(), currentExposure, additionalAmount, maxExposure);
    }

    private String generateLoanNumber() {
        String datePart = LocalDate.now().toString().replace("-", "");
        String seqPart = String.format("%06d", System.nanoTime() % 1000000);
        return "GLN" + datePart + seqPart;  // GLN = Guaranteed Loan Number
    }

    private void createDisbursementTransaction(Loan loan, BankingGroup group, FinancialYear year) {
        Transaction transaction = Transaction.builder()
                .transactionNumber(generateTransactionNumber())
                .group(group)
                .member(null)  // External borrower
                .financialYear(year)
                .transactionType(TransactionType.LOAN_DISBURSEMENT)
                .amount(loan.getPrincipalAmount())
                .debitCredit("DEBIT")
                .referenceType("GUARANTEED_LOAN")
                .referenceId(loan.getId())
                .description("Guaranteed loan disbursement to " + loan.getExternalBorrower().getFullName() +
                        " (Loan: " + loan.getLoanNumber() + ")")
                .build();

        transactionRepository.save(transaction);
    }

    private String generateTransactionNumber() {
        String datePart = LocalDate.now().toString().replace("-", "");
        String seqPart = String.format("%06d", System.nanoTime() % 1000000);
        return "TXN" + datePart + seqPart;
    }

    private ExternalBorrowerResponse mapToExternalBorrowerResponse(ExternalBorrower borrower) {
        long activeLoansCount = borrower.getLoans().stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE || l.getStatus() == LoanStatus.DISBURSED)
                .count();

        BigDecimal totalOutstanding = borrower.getLoans().stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE || l.getStatus() == LoanStatus.DISBURSED)
                .map(Loan::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ExternalBorrowerResponse.builder()
                .id(borrower.getId())
                .groupId(borrower.getGroup().getId())
                .firstName(borrower.getFirstName())
                .lastName(borrower.getLastName())
                .fullName(borrower.getFullName())
                .phoneNumber(borrower.getPhoneNumber())
                .email(borrower.getEmail())
                .nationalId(borrower.getNationalId())
                .address(borrower.getAddress())
                .employer(borrower.getEmployer())
                .occupation(borrower.getOccupation())
                .status(borrower.getStatus().name())
                .activeLoansCount((int) activeLoansCount)
                .totalOutstanding(totalOutstanding)
                .notes(borrower.getNotes())
                .build();
    }

    private LoanGuarantorResponse mapToGuarantorResponse(LoanGuarantor guarantor) {
        return LoanGuarantorResponse.builder()
                .id(guarantor.getId())
                .loanId(guarantor.getLoan().getId())
                .loanNumber(guarantor.getLoan().getLoanNumber())
                .memberId(guarantor.getMember().getId())
                .memberName(guarantor.getMember().getFullName())
                .memberPhone(guarantor.getMember().getPhoneNumber())
                .guaranteedAmount(guarantor.getGuaranteedAmount())
                .guaranteePercentage(guarantor.getGuaranteePercentage())
                .effectiveGuaranteedAmount(guarantor.getEffectiveGuaranteedAmount())
                .status(guarantor.getStatus().name())
                .acceptedAt(guarantor.getAcceptedAt())
                .releasedAt(guarantor.getReleasedAt())
                .amountPaidOnBehalf(guarantor.getAmountPaidOnBehalf())
                .notes(guarantor.getNotes())
                .build();
    }

    private GuaranteedLoanResponse mapToGuaranteedLoanResponse(Loan loan) {
        List<LoanGuarantorResponse> guarantorResponses = loan.getGuarantors().stream()
                .map(this::mapToGuarantorResponse)
                .collect(Collectors.toList());

        String primaryGuarantorName = loan.getPrimaryGuarantor() != null ?
                loan.getPrimaryGuarantor().getMember().getFullName() : null;

        return GuaranteedLoanResponse.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .externalBorrowerId(loan.getExternalBorrower().getId())
                .borrowerName(loan.getExternalBorrower().getFullName())
                .borrowerPhone(loan.getExternalBorrower().getPhoneNumber())
                .principalAmount(loan.getPrincipalAmount())
                .interestRate(loan.getInterestRate())
                .disbursementDate(loan.getDisbursementDate())
                .expectedEndDate(loan.getExpectedEndDate())
                .totalInterestAccrued(loan.getTotalInterestAccrued())
                .totalAmountDue(loan.getTotalAmountDue())
                .totalAmountPaid(loan.getTotalAmountPaid())
                .outstandingBalance(loan.getOutstandingBalance())
                .status(loan.getStatus().name())
                .guarantors(guarantorResponses)
                .primaryGuarantorName(primaryGuarantorName)
                .notes(loan.getNotes())
                .createdAt(loan.getCreatedAt())
                .build();
    }
}
