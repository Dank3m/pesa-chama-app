package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.MemberBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberBalanceRepository extends JpaRepository<MemberBalance, UUID> {

    List<MemberBalance> findByMemberId(UUID memberId);

    Optional<MemberBalance> findByMemberIdAndFinancialYearId(UUID memberId, UUID financialYearId);

    List<MemberBalance> findByFinancialYearId(UUID financialYearId);

    @Query("SELECT COALESCE(SUM(mb.totalContributions), 0) FROM MemberBalance mb " +
           "WHERE mb.financialYear.id = :yearId")
    BigDecimal getTotalContributionsByYear(@Param("yearId") UUID financialYearId);

    @Query("SELECT COALESCE(SUM(mb.outstandingLoanBalance), 0) FROM MemberBalance mb " +
           "WHERE mb.financialYear.id = :yearId")
    BigDecimal getTotalOutstandingLoansByYear(@Param("yearId") UUID financialYearId);

    @Query("SELECT mb FROM MemberBalance mb WHERE mb.financialYear.id = :yearId " +
           "ORDER BY mb.totalContributions DESC")
    List<MemberBalance> findByYearOrderByContributionsDesc(@Param("yearId") UUID financialYearId);

    @Query("SELECT mb FROM MemberBalance mb JOIN FETCH mb.member WHERE mb.financialYear.id = :yearId")
    List<MemberBalance> findByYearWithMembers(@Param("yearId") UUID financialYearId);
}
