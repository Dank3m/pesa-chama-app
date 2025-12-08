package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.LoanInterestAccrual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanInterestAccrualRepository extends JpaRepository<LoanInterestAccrual, UUID> {

    List<LoanInterestAccrual> findByLoanId(UUID loanId);

    List<LoanInterestAccrual> findByLoanIdOrderByAccrualDateAsc(UUID loanId);

    Optional<LoanInterestAccrual> findByLoanIdAndAccrualDate(UUID loanId, LocalDate accrualDate);

    @Query("SELECT COALESCE(SUM(lia.interestAmount), 0) FROM LoanInterestAccrual lia WHERE lia.loan.id = :loanId")
    BigDecimal getTotalInterestByLoan(@Param("loanId") UUID loanId);

    @Query("SELECT lia FROM LoanInterestAccrual lia WHERE lia.loan.id = :loanId " +
           "AND lia.accrualDate BETWEEN :startDate AND :endDate ORDER BY lia.accrualDate ASC")
    List<LoanInterestAccrual> findByLoanIdAndDateRange(
            @Param("loanId") UUID loanId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT MAX(lia.accrualDate) FROM LoanInterestAccrual lia WHERE lia.loan.id = :loanId")
    Optional<LocalDate> findLastAccrualDateByLoanId(@Param("loanId") UUID loanId);

    boolean existsByLoanIdAndAccrualDate(UUID loanId, LocalDate accrualDate);
}
