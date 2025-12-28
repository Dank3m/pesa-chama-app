package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.LoanGuarantor;
import com.tablebanking.loanmanagement.entity.enums.GuarantorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanGuarantorRepository extends JpaRepository<LoanGuarantor, UUID> {

    List<LoanGuarantor> findByLoanId(UUID loanId);

    List<LoanGuarantor> findByMemberId(UUID memberId);

    List<LoanGuarantor> findByMemberIdAndStatus(UUID memberId, GuarantorStatus status);

    Optional<LoanGuarantor> findByLoanIdAndMemberId(UUID loanId, UUID memberId);

    @Query("SELECT lg FROM LoanGuarantor lg " +
            "JOIN FETCH lg.loan l " +
            "JOIN FETCH lg.member m " +
            "WHERE lg.member.id = :memberId AND lg.status = 'ACTIVE'")
    List<LoanGuarantor> findActiveGuaranteesByMember(@Param("memberId") UUID memberId);

    @Query("SELECT lg FROM LoanGuarantor lg " +
            "JOIN FETCH lg.loan l " +
            "JOIN FETCH lg.member m " +
            "WHERE lg.loan.id = :loanId AND lg.status = 'ACTIVE'")
    List<LoanGuarantor> findActiveGuarantorsByLoan(@Param("loanId") UUID loanId);

    @Query("SELECT COUNT(lg) FROM LoanGuarantor lg " +
            "WHERE lg.member.id = :memberId AND lg.status = 'ACTIVE'")
    long countActiveGuaranteesByMember(@Param("memberId") UUID memberId);

    /**
     * Calculate total guaranteed exposure for a member.
     */
    @Query("SELECT COALESCE(SUM(" +
            "  CASE " +
            "    WHEN lg.guaranteedAmount IS NOT NULL THEN lg.guaranteedAmount " +
            "    ELSE l.outstandingBalance * lg.guaranteePercentage / 100 " +
            "  END" +
            "), 0) " +
            "FROM LoanGuarantor lg " +
            "JOIN lg.loan l " +
            "WHERE lg.member.id = :memberId " +
            "AND lg.status = 'ACTIVE' " +
            "AND l.status IN ('ACTIVE', 'DISBURSED')")
    BigDecimal calculateMemberExposure(@Param("memberId") UUID memberId);

    /**
     * Get total amount a member has paid on behalf of defaulting borrowers.
     */
    @Query("SELECT COALESCE(SUM(lg.amountPaidOnBehalf), 0) " +
            "FROM LoanGuarantor lg " +
            "WHERE lg.member.id = :memberId")
    BigDecimal calculateTotalPaidOnBehalf(@Param("memberId") UUID memberId);
}