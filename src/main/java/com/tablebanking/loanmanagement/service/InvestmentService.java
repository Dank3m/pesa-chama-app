package com.tablebanking.loanmanagement.service;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.*;
import com.tablebanking.loanmanagement.entity.enums.InvestmentStatus;
import com.tablebanking.loanmanagement.entity.enums.InvestmentType;
import com.tablebanking.loanmanagement.entity.enums.TransactionType;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final BankingGroupRepository groupRepository;
    private final FinancialYearRepository financialYearRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Create a new investment.
     */
    public InvestmentResponse createInvestment(CreateInvestmentRequest request, UUID createdBy) {
        BankingGroup group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new BusinessException("Group not found"));

        FinancialYear financialYear = financialYearRepository
                .findCurrentByGroupId(group.getId())
                .orElseThrow(() -> new BusinessException("No active financial year found"));

        InvestmentType type;
        try {
            type = InvestmentType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid investment type: " + request.getType());
        }

        Investment investment = Investment.builder()
                .group(group)
                .financialYear(financialYear)
                .type(type)
                .name(request.getName())
                .description(request.getDescription())
                .amount(request.getAmount())
                .currentValue(request.getCurrentValue() != null ? request.getCurrentValue() : request.getAmount())
                .investmentDate(request.getInvestmentDate())
                .maturityDate(request.getMaturityDate())
                .status(InvestmentStatus.ACTIVE)
                .receiptNumber(request.getReceiptNumber())
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();

        investment = investmentRepository.save(investment);

        // Create transaction record
        createInvestmentTransaction(investment);

        log.info("Investment created: {} - {} for group: {}", investment.getId(), investment.getName(), group.getName());

        return mapToResponse(investment);
    }

    /**
     * Update an existing investment.
     */
    public InvestmentResponse updateInvestment(UpdateInvestmentRequest request, UUID updatedBy) {
        Investment investment = investmentRepository.findById(request.getInvestmentId())
                .orElseThrow(() -> new BusinessException("Investment not found"));

        if (investment.getIsDeleted()) {
            throw new BusinessException("Cannot update a deleted investment");
        }

        if (request.getType() != null) {
            try {
                investment.setType(InvestmentType.valueOf(request.getType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid investment type: " + request.getType());
            }
        }

        if (request.getName() != null) {
            investment.setName(request.getName());
        }

        if (request.getDescription() != null) {
            investment.setDescription(request.getDescription());
        }

        if (request.getAmount() != null) {
            investment.setAmount(request.getAmount());
        }

        if (request.getCurrentValue() != null) {
            investment.setCurrentValue(request.getCurrentValue());
        }

        if (request.getInvestmentDate() != null) {
            investment.setInvestmentDate(request.getInvestmentDate());
        }

        if (request.getMaturityDate() != null) {
            investment.setMaturityDate(request.getMaturityDate());
        }

        if (request.getStatus() != null) {
            try {
                investment.setStatus(InvestmentStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid investment status: " + request.getStatus());
            }
        }

        if (request.getReceiptNumber() != null) {
            investment.setReceiptNumber(request.getReceiptNumber());
        }

        if (request.getNotes() != null) {
            investment.setNotes(request.getNotes());
        }

        investment.setUpdatedBy(updatedBy);
        investment = investmentRepository.save(investment);

        log.info("Investment updated: {}", investment.getId());

        return mapToResponse(investment);
    }

    /**
     * Soft delete an investment.
     */
    public void deleteInvestment(UUID investmentId, UUID deletedBy) {
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new BusinessException("Investment not found"));

        if (investment.getIsDeleted()) {
            throw new BusinessException("Investment already deleted");
        }

        investment.setIsDeleted(true);
        investment.setUpdatedBy(deletedBy);
        investmentRepository.save(investment);

        log.info("Investment deleted: {}", investmentId);
    }

    /**
     * Get investment by ID.
     */
    @Transactional(readOnly = true)
    public InvestmentResponse getInvestmentById(UUID investmentId) {
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new BusinessException("Investment not found"));

        if (investment.getIsDeleted()) {
            throw new BusinessException("Investment not found");
        }

        return mapToResponse(investment);
    }

    /**
     * Get investments by group with pagination.
     */
    @Transactional(readOnly = true)
    public Page<InvestmentResponse> getInvestmentsByGroup(UUID groupId, Pageable pageable) {
        return investmentRepository.findByGroupId(groupId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get investment summary for a group.
     */
    @Transactional(readOnly = true)
    public InvestmentSummaryResponse getInvestmentSummary(UUID groupId) {
        BigDecimal totalInvested = investmentRepository.sumTotalAmountByGroup(groupId);
        BigDecimal currentValue = investmentRepository.sumCurrentValueByGroup(groupId);
        int activeCount = investmentRepository.countByGroupAndStatus(groupId, InvestmentStatus.ACTIVE);
        int maturedCount = investmentRepository.countByGroupAndStatus(groupId, InvestmentStatus.MATURED);
        int redeemedCount = investmentRepository.countByGroupAndStatus(groupId, InvestmentStatus.REDEEMED);
        int totalCount = investmentRepository.countByGroup(groupId);

        return InvestmentSummaryResponse.builder()
                .groupId(groupId)
                .totalInvested(totalInvested)
                .currentValue(currentValue)
                .activeCount(activeCount)
                .maturedCount(maturedCount)
                .redeemedCount(redeemedCount)
                .totalCount(totalCount)
                .build();
    }

    // Private helper methods

    private void createInvestmentTransaction(Investment investment) {
        Transaction transaction = Transaction.builder()
                .transactionNumber(generateTransactionNumber())
                .group(investment.getGroup())
                .financialYear(investment.getFinancialYear())
                .transactionType(TransactionType.INVESTMENT)
                .amount(investment.getAmount())
                .debitCredit("DEBIT")
                .referenceType("INVESTMENT")
                .referenceId(investment.getId())
                .description(investment.getType().name() + ": " + investment.getName())
                .createdBy(investment.getCreatedBy())
                .build();

        transactionRepository.save(transaction);
    }

    private String generateTransactionNumber() {
        String datePart = LocalDate.now().toString().replace("-", "");
        String seqPart = String.format("%06d", System.nanoTime() % 1000000);
        return "TXN" + datePart + seqPart;
    }

    private InvestmentResponse mapToResponse(Investment investment) {
        return InvestmentResponse.builder()
                .id(investment.getId())
                .groupId(investment.getGroup().getId())
                .financialYearId(investment.getFinancialYear().getId())
                .yearName(investment.getFinancialYear().getYearName())
                .type(investment.getType().name())
                .name(investment.getName())
                .description(investment.getDescription())
                .amount(investment.getAmount())
                .currentValue(investment.getCurrentValue())
                .investmentDate(investment.getInvestmentDate())
                .maturityDate(investment.getMaturityDate())
                .status(investment.getStatus().name())
                .receiptNumber(investment.getReceiptNumber())
                .notes(investment.getNotes())
                .createdBy(investment.getCreatedBy())
                .createdAt(investment.getCreatedAt())
                .updatedAt(investment.getUpdatedAt())
                .build();
    }
}
