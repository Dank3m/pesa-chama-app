package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.Transaction;
import com.tablebanking.loanmanagement.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByTransactionNumber(String transactionNumber);

    List<Transaction> findByGroupId(UUID groupId);

    Page<Transaction> findByGroupId(UUID groupId, Pageable pageable);

    List<Transaction> findByMemberId(UUID memberId);

    Page<Transaction> findByMemberId(UUID memberId, Pageable pageable);

    List<Transaction> findByFinancialYearId(UUID financialYearId);

    List<Transaction> findByTransactionType(TransactionType transactionType);

    @Query("SELECT t FROM Transaction t WHERE t.group.id = :groupId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findByGroupIdAndDateRange(
            @Param("groupId") UUID groupId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT t FROM Transaction t WHERE t.member.id = :memberId " +
           "AND t.financialYear.id = :yearId ORDER BY t.transactionDate DESC")
    List<Transaction> findByMemberIdAndYearId(
            @Param("memberId") UUID memberId,
            @Param("yearId") UUID financialYearId);

    @Query("SELECT COALESCE(SUM(CASE WHEN t.debitCredit = 'CREDIT' THEN t.amount ELSE 0 END), 0) " +
           "FROM Transaction t WHERE t.financialYear.id = :yearId")
    BigDecimal getTotalCreditsByYear(@Param("yearId") UUID financialYearId);

    @Query("SELECT COALESCE(SUM(CASE WHEN t.debitCredit = 'DEBIT' THEN t.amount ELSE 0 END), 0) " +
           "FROM Transaction t WHERE t.financialYear.id = :yearId")
    BigDecimal getTotalDebitsByYear(@Param("yearId") UUID financialYearId);

    @Query("SELECT t FROM Transaction t WHERE t.referenceType = :refType AND t.referenceId = :refId")
    List<Transaction> findByReference(
            @Param("refType") String referenceType,
            @Param("refId") UUID referenceId);

    // Filter by group and debit/credit
    Page<Transaction> findByGroupIdAndDebitCredit(UUID groupId, String debitCredit, Pageable pageable);

    // Filter by group and transaction type
    Page<Transaction> findByGroupIdAndTransactionType(UUID groupId, TransactionType transactionType, Pageable pageable);

    // Filter by group, debit/credit and transaction type
    @Query("SELECT t FROM Transaction t WHERE t.group.id = :groupId " +
            "AND t.debitCredit = :debitCredit AND t.transactionType = :transactionType")
    Page<Transaction> findByGroupIdAndDebitCreditAndTransactionType(
            @Param("groupId") UUID groupId,
            @Param("debitCredit") String debitCredit,
            @Param("transactionType") TransactionType transactionType,
            Pageable pageable);

    // Filter by member and debit/credit
    Page<Transaction> findByMemberIdAndDebitCredit(UUID memberId, String debitCredit, Pageable pageable);

    // Filter by member and transaction type
    Page<Transaction> findByMemberIdAndTransactionType(UUID memberId, TransactionType transactionType, Pageable pageable);

    // Filter by member, debit/credit and transaction type
    @Query("SELECT t FROM Transaction t WHERE t.member.id = :memberId " +
            "AND t.debitCredit = :debitCredit AND t.transactionType = :transactionType")
    Page<Transaction> findByMemberIdAndDebitCreditAndTransactionType(
            @Param("memberId") UUID memberId,
            @Param("debitCredit") String debitCredit,
            @Param("transactionType") TransactionType transactionType,
            Pageable pageable);

    // Count by financial year
    long countByFinancialYearId(UUID financialYearId);
}
