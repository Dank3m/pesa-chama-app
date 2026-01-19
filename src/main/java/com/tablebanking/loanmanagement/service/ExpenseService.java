package com.tablebanking.loanmanagement.service;

import com.tablebanking.loanmanagement.dto.request.RequestDTOs.*;
import com.tablebanking.loanmanagement.dto.response.ResponseDTOs.*;
import com.tablebanking.loanmanagement.entity.*;
import com.tablebanking.loanmanagement.entity.enums.ExpenseCategory;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final BankingGroupRepository groupRepository;
    private final FinancialYearRepository financialYearRepository;
    private final LoanRepository loanRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Create a new expense.
     */
    public ExpenseResponse createExpense(CreateExpenseRequest request, UUID createdBy) {
        BankingGroup group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new BusinessException("Group not found"));

        FinancialYear financialYear = financialYearRepository
                .findCurrentByGroupId(group.getId())
                .orElseThrow(() -> new BusinessException("No active financial year found"));

        ExpenseCategory category;
        try {
            category = ExpenseCategory.valueOf(request.getCategory().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid expense category: " + request.getCategory());
        }

        Loan loan = null;
        if (request.getLoanId() != null) {
            loan = loanRepository.findById(request.getLoanId())
                    .orElseThrow(() -> new BusinessException("Loan not found"));
        }

        Expense expense = Expense.builder()
                .group(group)
                .financialYear(financialYear)
                .category(category)
                .amount(request.getAmount())
                .expenseDate(request.getExpenseDate())
                .description(request.getDescription())
                .vendor(request.getVendor())
                .receiptNumber(request.getReceiptNumber())
                .loan(loan)
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();

        expense = expenseRepository.save(expense);

        // Update financial year total expenses
        financialYear.addExpense(expense.getAmount());
        financialYearRepository.save(financialYear);

        // Create transaction record
        createExpenseTransaction(expense);

        log.info("Expense created: {} - {} for group: {}", expense.getId(), expense.getDescription(), group.getName());

        return mapToResponse(expense);
    }

    /**
     * Update an existing expense.
     */
    public ExpenseResponse updateExpense(UpdateExpenseRequest request, UUID updatedBy) {
        Expense expense = expenseRepository.findById(request.getExpenseId())
                .orElseThrow(() -> new BusinessException("Expense not found"));

        if (expense.getIsDeleted()) {
            throw new BusinessException("Cannot update a deleted expense");
        }

        BigDecimal oldAmount = expense.getAmount();

        if (request.getCategory() != null) {
            try {
                expense.setCategory(ExpenseCategory.valueOf(request.getCategory().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid expense category: " + request.getCategory());
            }
        }

        if (request.getAmount() != null) {
            expense.setAmount(request.getAmount());
        }

        if (request.getExpenseDate() != null) {
            expense.setExpenseDate(request.getExpenseDate());
        }

        if (request.getDescription() != null) {
            expense.setDescription(request.getDescription());
        }

        if (request.getVendor() != null) {
            expense.setVendor(request.getVendor());
        }

        if (request.getReceiptNumber() != null) {
            expense.setReceiptNumber(request.getReceiptNumber());
        }

        if (request.getLoanId() != null) {
            Loan loan = loanRepository.findById(request.getLoanId())
                    .orElseThrow(() -> new BusinessException("Loan not found"));
            expense.setLoan(loan);
        }

        if (request.getNotes() != null) {
            expense.setNotes(request.getNotes());
        }

        expense.setUpdatedBy(updatedBy);
        expense = expenseRepository.save(expense);

        // Update financial year totals if amount changed
        if (request.getAmount() != null && !oldAmount.equals(request.getAmount())) {
            FinancialYear financialYear = expense.getFinancialYear();
            BigDecimal difference = request.getAmount().subtract(oldAmount);
            financialYear.addExpense(difference);
            financialYearRepository.save(financialYear);
        }

        log.info("Expense updated: {}", expense.getId());

        return mapToResponse(expense);
    }

    /**
     * Soft delete an expense.
     */
    public void deleteExpense(UUID expenseId, UUID deletedBy) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new BusinessException("Expense not found"));

        if (expense.getIsDeleted()) {
            throw new BusinessException("Expense already deleted");
        }

        expense.setIsDeleted(true);
        expense.setUpdatedBy(deletedBy);
        expenseRepository.save(expense);

        // Update financial year totals
        FinancialYear financialYear = expense.getFinancialYear();
        financialYear.addExpense(expense.getAmount().negate());
        financialYearRepository.save(financialYear);

        log.info("Expense deleted: {}", expenseId);
    }

    /**
     * Get expense by ID.
     */
    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(UUID expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new BusinessException("Expense not found"));

        if (expense.getIsDeleted()) {
            throw new BusinessException("Expense not found");
        }

        return mapToResponse(expense);
    }

    /**
     * Get expenses by group with pagination.
     */
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpensesByGroup(UUID groupId, Pageable pageable) {
        return expenseRepository.findByGroupId(groupId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get expenses by financial year with pagination.
     */
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpensesByFinancialYear(UUID financialYearId, Pageable pageable) {
        return expenseRepository.findByFinancialYearId(financialYearId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get expenses by category.
     */
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByCategory(UUID groupId, String category) {
        ExpenseCategory expenseCategory;
        try {
            expenseCategory = ExpenseCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid expense category: " + category);
        }

        return expenseRepository.findByGroupIdAndCategory(groupId, expenseCategory)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get expenses by date range.
     */
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByDateRange(UUID groupId, LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByGroupIdAndDateRange(groupId, startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get expense summary for a financial year.
     */
    @Transactional(readOnly = true)
    public ExpenseSummaryResponse getExpenseSummary(UUID groupId, UUID financialYearId) {
        // If no financial year specified, get current
        UUID yearId = financialYearId;
        if (yearId == null) {
            FinancialYear currentYear = financialYearRepository
                    .findCurrentByGroupId(groupId)
                    .orElseThrow(() -> new BusinessException("No active financial year found"));
            yearId = currentYear.getId();
        }

        BigDecimal totalExpenses = expenseRepository.sumTotalByFinancialYear(yearId);
        int expenseCount = expenseRepository.countByFinancialYear(yearId);
        BigDecimal transactionFees = expenseRepository.sumTransactionFeesByFinancialYear(yearId);
        BigDecimal agmExpenses = expenseRepository.sumAgmExpensesByFinancialYear(yearId);
        BigDecimal administrativeExpenses = expenseRepository.sumAdministrativeExpensesByFinancialYear(yearId);
        BigDecimal otherExpenses = totalExpenses
                .subtract(transactionFees)
                .subtract(agmExpenses)
                .subtract(administrativeExpenses);

        return ExpenseSummaryResponse.builder()
                .groupId(groupId)
                .financialYearId(yearId)
                .totalExpenses(totalExpenses)
                .expenseCount(expenseCount)
                .transactionFees(transactionFees)
                .agmExpenses(agmExpenses)
                .administrativeExpenses(administrativeExpenses)
                .otherExpenses(otherExpenses)
                .build();
    }

    // Private helper methods

    private void createExpenseTransaction(Expense expense) {
        Transaction transaction = Transaction.builder()
                .transactionNumber(generateTransactionNumber())
                .group(expense.getGroup())
                .financialYear(expense.getFinancialYear())
                .transactionType(TransactionType.EXPENSE)
                .amount(expense.getAmount())
                .debitCredit("DEBIT")
                .referenceType("EXPENSE")
                .referenceId(expense.getId())
                .description(expense.getCategory().name() + ": " + expense.getDescription())
                .createdBy(expense.getCreatedBy())
                .build();

        transactionRepository.save(transaction);
    }

    private String generateTransactionNumber() {
        String datePart = LocalDate.now().toString().replace("-", "");
        String seqPart = String.format("%06d", System.nanoTime() % 1000000);
        return "TXN" + datePart + seqPart;
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .groupId(expense.getGroup().getId())
                .financialYearId(expense.getFinancialYear().getId())
                .yearName(expense.getFinancialYear().getYearName())
                .category(expense.getCategory().name())
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .description(expense.getDescription())
                .vendor(expense.getVendor())
                .receiptNumber(expense.getReceiptNumber())
                .loanId(expense.getLoan() != null ? expense.getLoan().getId() : null)
                .loanNumber(expense.getLoan() != null ? expense.getLoan().getLoanNumber() : null)
                .notes(expense.getNotes())
                .createdBy(expense.getCreatedBy())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}
