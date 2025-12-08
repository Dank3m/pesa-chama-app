package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.Transaction;
import com.tablebanking.loanmanagement.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

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
}
