package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.LoanRepayment;
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
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, UUID> {

    List<LoanRepayment> findByLoanId(UUID loanId);

    List<LoanRepayment> findByLoanIdOrderByPaymentNumberAsc(UUID loanId);

    Optional<LoanRepayment> findByLoanIdAndPaymentNumber(UUID loanId, Integer paymentNumber);

    @Query("SELECT COALESCE(MAX(lr.paymentNumber), 0) FROM LoanRepayment lr WHERE lr.loan.id = :loanId")
    int getMaxPaymentNumber(@Param("loanId") UUID loanId);

    @Query("SELECT COALESCE(SUM(lr.amount), 0) FROM LoanRepayment lr WHERE lr.loan.id = :loanId")
    BigDecimal getTotalRepaymentsByLoan(@Param("loanId") UUID loanId);

    @Query("SELECT lr FROM LoanRepayment lr WHERE lr.loan.member.id = :memberId " +
           "ORDER BY lr.paymentDate DESC")
    List<LoanRepayment> findByMemberId(@Param("memberId") UUID memberId);

    @Query("SELECT lr FROM LoanRepayment lr WHERE lr.paymentDate BETWEEN :startDate AND :endDate")
    List<LoanRepayment> findByDateRange(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    Optional<LoanRepayment> findByReferenceNumber(String referenceNumber);
}
