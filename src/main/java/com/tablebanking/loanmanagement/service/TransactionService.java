package com.tablebanking.loanmanagement.service;

import com.tablebanking.loanmanagement.controller.TransactionController.TransactionSummaryResponse;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.Transaction;
import com.tablebanking.loanmanagement.entity.enums.TransactionType;
import com.tablebanking.loanmanagement.exception.BusinessException;
import com.tablebanking.loanmanagement.repository.FinancialYearRepository;
import com.tablebanking.loanmanagement.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final FinancialYearRepository financialYearRepository;

    /**
     * Get transactions by group with optional filtering and search
     */
    public PagedResponse<TransactionResponse> getTransactionsByGroup(
            UUID groupId, String debitCreditFilter, String transactionTypeFilter,
            String search, Pageable pageable) {

        Specification<Transaction> spec = buildSpecification(groupId, null, debitCreditFilter, transactionTypeFilter, search);
        Page<Transaction> page = transactionRepository.findAll(spec, pageable);

        return mapToPagedResponse(page);
    }

    /**
     * Get transactions by member with optional filtering and search
     */
    public PagedResponse<TransactionResponse> getTransactionsByMember(
            UUID memberId, String debitCreditFilter, String transactionTypeFilter,
            String search, Pageable pageable) {

        Specification<Transaction> spec = buildSpecification(null, memberId, debitCreditFilter, transactionTypeFilter, search);
        Page<Transaction> page = transactionRepository.findAll(spec, pageable);

        return mapToPagedResponse(page);
    }

    /**
     * Get all transactions for export (no pagination)
     */
    public List<TransactionResponse> getTransactionsForExport(
            UUID groupId, UUID memberId, String debitCreditFilter, String search) {

        Specification<Transaction> spec = buildSpecification(groupId, memberId, debitCreditFilter, null, search);
        List<Transaction> transactions = transactionRepository.findAll(spec);

        return transactions.stream().map(this::mapToResponse).toList();
    }

    /**
     * Get transaction by ID
     */
    public TransactionResponse getTransactionById(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException("Transaction not found"));
        return mapToResponse(transaction);
    }

    /**
     * Get transaction summary
     */
    public TransactionSummaryResponse getTransactionSummary(UUID groupId, UUID financialYearId) {
        UUID yearId = financialYearId != null ? financialYearId
                : financialYearRepository.findCurrentByGroupId(groupId)
                .map(fy -> fy.getId())
                .orElse(null);

        BigDecimal totalCredits = transactionRepository.getTotalCreditsByYear(yearId);
        BigDecimal totalDebits = transactionRepository.getTotalDebitsByYear(yearId);
        long totalCount = yearId != null ? transactionRepository.countByFinancialYearId(yearId) : 0;

        totalCredits = totalCredits != null ? totalCredits : BigDecimal.ZERO;
        totalDebits = totalDebits != null ? totalDebits : BigDecimal.ZERO;

        return TransactionSummaryResponse.builder()
                .totalCredits(totalCredits)
                .totalDebits(totalDebits)
                .netBalance(totalCredits.subtract(totalDebits))
                .totalTransactions(totalCount)
                .build();
    }

    /**
     * Build JPA Specification for dynamic filtering
     */
    private Specification<Transaction> buildSpecification(
            UUID groupId, UUID memberId, String debitCreditFilter,
            String transactionTypeFilter, String search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by group
            if (groupId != null) {
                predicates.add(cb.equal(root.get("group").get("id"), groupId));
            }

            // Filter by member
            if (memberId != null) {
                predicates.add(cb.equal(root.get("member").get("id"), memberId));
            }

            // Filter by debit/credit
            if (debitCreditFilter != null && !debitCreditFilter.isBlank()) {
                predicates.add(cb.equal(root.get("debitCredit"), debitCreditFilter.toUpperCase()));
            }

            // Filter by transaction type
            if (transactionTypeFilter != null && !transactionTypeFilter.isBlank()) {
                try {
                    TransactionType type = TransactionType.valueOf(transactionTypeFilter.toUpperCase());
                    predicates.add(cb.equal(root.get("transactionType"), type));
                } catch (IllegalArgumentException ignored) {
                    // Invalid type, ignore filter
                }
            }

            // Search by description, transaction number, or member name
            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate descriptionMatch = cb.like(cb.lower(root.get("description")), searchPattern);
                Predicate txnNumberMatch = cb.like(cb.lower(root.get("transactionNumber")), searchPattern);
                Predicate memberNameMatch = cb.like(
                        cb.lower(cb.concat(root.get("member").get("firstName"),
                                cb.concat(" ", root.get("member").get("lastName")))),
                        searchPattern);

                predicates.add(cb.or(descriptionMatch, txnNumberMatch, memberNameMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private PagedResponse<TransactionResponse> mapToPagedResponse(Page<Transaction> page) {
        return PagedResponse.<TransactionResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .transactionNumber(t.getTransactionNumber())
                .groupId(t.getGroup().getId())
                .memberId(t.getMember() != null ? t.getMember().getId() : null)
                .memberName(t.getMember() != null ? t.getMember().getFullName() : null)
                .transactionType(t.getTransactionType())
                .transactionDate(t.getTransactionDate())
                .amount(t.getAmount())
                .debitCredit(t.getDebitCredit())
                .description(t.getDescription())
                .referenceType(t.getReferenceType())
                .referenceId(t.getReferenceId())
                .createdAt(t.getCreatedAt())
                .build();
    }
}