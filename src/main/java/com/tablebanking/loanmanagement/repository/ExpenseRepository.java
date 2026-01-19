package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.Expense;
import com.tablebanking.loanmanagement.entity.enums.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    // Find by group (non-deleted)
    @Query("SELECT e FROM Expense e WHERE e.group.id = :groupId AND e.isDeleted = false ORDER BY e.expenseDate DESC")
    List<Expense> findByGroupId(@Param("groupId") UUID groupId);

    // Find by group with pagination
    @Query("SELECT e FROM Expense e WHERE e.group.id = :groupId AND e.isDeleted = false ORDER BY e.expenseDate DESC")
    Page<Expense> findByGroupId(@Param("groupId") UUID groupId, Pageable pageable);

    // Find by financial year
    @Query("SELECT e FROM Expense e WHERE e.financialYear.id = :yearId AND e.isDeleted = false ORDER BY e.expenseDate DESC")
    List<Expense> findByFinancialYearId(@Param("yearId") UUID financialYearId);

    // Find by financial year with pagination
    @Query("SELECT e FROM Expense e WHERE e.financialYear.id = :yearId AND e.isDeleted = false ORDER BY e.expenseDate DESC")
    Page<Expense> findByFinancialYearId(@Param("yearId") UUID financialYearId, Pageable pageable);

    // Find by category
    @Query("SELECT e FROM Expense e WHERE e.group.id = :groupId AND e.category = :category AND e.isDeleted = false ORDER BY e.expenseDate DESC")
    List<Expense> findByGroupIdAndCategory(@Param("groupId") UUID groupId, @Param("category") ExpenseCategory category);

    // Find by date range
    @Query("SELECT e FROM Expense e WHERE e.group.id = :groupId AND e.expenseDate BETWEEN :startDate AND :endDate AND e.isDeleted = false ORDER BY e.expenseDate DESC")
    List<Expense> findByGroupIdAndDateRange(
            @Param("groupId") UUID groupId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Find by loan
    @Query("SELECT e FROM Expense e WHERE e.loan.id = :loanId AND e.isDeleted = false")
    List<Expense> findByLoanId(@Param("loanId") UUID loanId);

    // Sum total expenses by group
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.group.id = :groupId AND e.isDeleted = false")
    BigDecimal sumTotalByGroup(@Param("groupId") UUID groupId);

    // Sum total expenses by financial year
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.financialYear.id = :yearId AND e.isDeleted = false")
    BigDecimal sumTotalByFinancialYear(@Param("yearId") UUID financialYearId);

    // Sum by category for a financial year
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.financialYear.id = :yearId AND e.category = :category AND e.isDeleted = false")
    BigDecimal sumByCategoryAndFinancialYear(@Param("yearId") UUID financialYearId, @Param("category") ExpenseCategory category);

    // Count expenses by financial year
    @Query("SELECT COUNT(e) FROM Expense e WHERE e.financialYear.id = :yearId AND e.isDeleted = false")
    int countByFinancialYear(@Param("yearId") UUID financialYearId);

    // Sum transaction fees (for stats)
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.financialYear.id = :yearId AND e.category IN ('TRANSACTION_FEE', 'DISBURSEMENT_FEE', 'BANK_CHARGES') AND e.isDeleted = false")
    BigDecimal sumTransactionFeesByFinancialYear(@Param("yearId") UUID financialYearId);

    // Sum AGM expenses
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.financialYear.id = :yearId AND e.category IN ('AGM_VENUE', 'AGM_CATERING', 'AGM_OTHER') AND e.isDeleted = false")
    BigDecimal sumAgmExpensesByFinancialYear(@Param("yearId") UUID financialYearId);

    // Sum administrative expenses
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.financialYear.id = :yearId AND e.category IN ('ADMINISTRATIVE', 'COMMUNICATION', 'LEGAL') AND e.isDeleted = false")
    BigDecimal sumAdministrativeExpensesByFinancialYear(@Param("yearId") UUID financialYearId);
}
