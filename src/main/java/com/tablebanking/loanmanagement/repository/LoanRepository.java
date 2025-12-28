package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.Loan;
import com.tablebanking.loanmanagement.entity.enums.LoanStatus;
import com.tablebanking.loanmanagement.entity.enums.LoanType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    Optional<Loan> findByLoanNumber(String loanNumber);

    List<Loan> findByMemberId(UUID memberId);

    Page<Loan> findByMemberId(UUID memberId, Pageable pageable);

    List<Loan> findByMemberIdAndStatus(UUID memberId, LoanStatus status);

    List<Loan> findByFinancialYearId(UUID financialYearId);

    List<Loan> findByStatus(LoanStatus status);
    List<Loan> findByMemberGroupIdAndStatus(UUID groupId, LoanStatus status);


    @Query("SELECT l FROM Loan l WHERE l.status IN ('DISBURSED', 'ACTIVE')")
    List<Loan> findActiveLoans();

    @Query("SELECT l FROM Loan l WHERE l.financialYear.group.id = :groupId " +
           "AND l.status IN ('DISBURSED', 'ACTIVE')")
    List<Loan> findActiveLoansByGroupId(@Param("groupId") UUID groupId);

    @Query("SELECT l FROM Loan l WHERE l.member.id = :memberId " +
           "AND l.status IN ('DISBURSED', 'ACTIVE')")
    List<Loan> findActiveLoansByMemberId(@Param("memberId") UUID memberId);

    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l " +
           "WHERE l.member.id = :memberId AND l.status IN ('DISBURSED', 'ACTIVE')")
    BigDecimal getTotalOutstandingByMember(@Param("memberId") UUID memberId);

    @Query("SELECT COALESCE(SUM(l.principalAmount), 0) FROM Loan l " +
           "WHERE l.financialYear.id = :yearId")
    BigDecimal getTotalDisbursedByYear(@Param("yearId") UUID financialYearId);

    @Query("SELECT COALESCE(SUM(l.totalInterestAccrued), 0) FROM Loan l " +
           "WHERE l.financialYear.id = :yearId")
    BigDecimal getTotalInterestEarnedByYear(@Param("yearId") UUID financialYearId);

    @Query("SELECT l FROM Loan l WHERE l.status IN ('DISBURSED', 'ACTIVE') " +
           "AND l.disbursementDate <= :date")
    List<Loan> findLoansForInterestAccrual(@Param("date") LocalDate date);

    @Query("SELECT l FROM Loan l WHERE l.expectedEndDate < :date " +
           "AND l.status IN ('DISBURSED', 'ACTIVE')")
    List<Loan> findOverdueLoans(@Param("date") LocalDate date);

    @Query("SELECT l FROM Loan l LEFT JOIN FETCH l.repayments WHERE l.id = :id")
    Optional<Loan> findByIdWithRepayments(@Param("id") UUID id);

    @Query("SELECT l FROM Loan l LEFT JOIN FETCH l.interestAccruals WHERE l.id = :id")
    Optional<Loan> findByIdWithAccruals(@Param("id") UUID id);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.member.id = :memberId " +
           "AND l.status IN ('DISBURSED', 'ACTIVE')")
    long countActiveLoansByMember(@Param("memberId") UUID memberId);

    List<Loan> findByLoanTypeAndStatus(LoanType loanType, LoanStatus status);

    @Query("SELECT l FROM Loan l WHERE l.sourceContribution.id = :contributionId")
    Optional<Loan> findBySourceContributionId(@Param("contributionId") UUID contributionId);

    @Query("SELECT l FROM Loan l " +
            "LEFT JOIN FETCH l.externalBorrower eb " +
            "LEFT JOIN FETCH l.guarantors g " +
            "LEFT JOIN FETCH g.member m " +
            "WHERE l.financialYear.group.id = :groupId " +
            "AND l.loanType = :loanType " +
            "ORDER BY l.createdAt DESC")
    List<Loan> findByGroupIdAndLoanType(@Param("groupId") UUID groupId, @Param("loanType") LoanType loanType);

    /**
     * Find loans by external borrower.
     */
    @Query("SELECT l FROM Loan l " +
            "LEFT JOIN FETCH l.externalBorrower eb " +
            "LEFT JOIN FETCH l.guarantors g " +
            "LEFT JOIN FETCH g.member m " +
            "WHERE l.externalBorrower.id = :borrowerId " +
            "ORDER BY l.createdAt DESC")
    List<Loan> findByExternalBorrowerId(@Param("borrowerId") UUID borrowerId);

    /**
     * Find active guaranteed loans.
     */
    @Query("SELECT l FROM Loan l " +
            "LEFT JOIN FETCH l.externalBorrower eb " +
            "LEFT JOIN FETCH l.guarantors g " +
            "WHERE l.loanType = 'GUARANTEED' " +
            "AND l.status IN ('ACTIVE', 'DISBURSED') " +
            "AND l.financialYear.group.id = :groupId")
    List<Loan> findActiveGuaranteedLoans(@Param("groupId") UUID groupId);
}
