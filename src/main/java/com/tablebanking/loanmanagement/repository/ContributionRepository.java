package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.Contribution;
import com.tablebanking.loanmanagement.entity.ContributionCycle;
import com.tablebanking.loanmanagement.entity.enums.ContributionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContributionRepository extends JpaRepository<Contribution, UUID> {

    List<Contribution> findByMemberId(UUID memberId);

    Page<Contribution> findByMemberId(UUID memberId, Pageable pageable);

    List<Contribution> findByCycleId(UUID cycleId);

    List<Contribution> findByCycleAndStatusIn(ContributionCycle cycle, List<ContributionStatus> statuses);


    Optional<Contribution> findByMemberIdAndCycleId(UUID memberId, UUID cycleId);

    List<Contribution> findByMemberIdAndStatus(UUID memberId, ContributionStatus status);

    List<Contribution> findByCycleIdAndStatus(UUID cycleId, ContributionStatus status);

    @Query("SELECT c FROM Contribution c WHERE c.cycle.id = :cycleId AND c.status IN ('PENDING', 'PARTIAL')")
    List<Contribution> findUnpaidContributions(@Param("cycleId") UUID cycleId);

    @Query("SELECT c FROM Contribution c WHERE c.cycle.id = :cycleId " +
           "AND c.status IN ('PENDING', 'PARTIAL') " +
           "AND c.convertedToLoan = false")
    List<Contribution> findDefaultedContributions(@Param("cycleId") UUID cycleId);

    @Query("SELECT COALESCE(SUM(c.paidAmount), 0) FROM Contribution c WHERE c.member.id = :memberId " +
           "AND c.cycle.financialYear.id = :yearId")
    BigDecimal getTotalContributionsByMemberAndYear(
            @Param("memberId") UUID memberId, 
            @Param("yearId") UUID financialYearId);

    @Query("SELECT COALESCE(SUM(c.paidAmount), 0) FROM Contribution c WHERE c.cycle.id = :cycleId")
    BigDecimal getTotalCollectedByCycle(@Param("cycleId") UUID cycleId);

    @Query("SELECT COUNT(c) FROM Contribution c WHERE c.cycle.id = :cycleId AND c.status = :status")
    long countByCycleIdAndStatus(@Param("cycleId") UUID cycleId, @Param("status") ContributionStatus status);

    @Query("SELECT c FROM Contribution c JOIN FETCH c.member WHERE c.cycle.id = :cycleId")
    List<Contribution> findByCycleIdWithMembers(@Param("cycleId") UUID cycleId);

    @Query("SELECT c FROM Contribution c " +
            "JOIN FETCH c.cycle cycle " +
            "WHERE c.member.id = :memberId " +
            "AND cycle.financialYear.id = :financialYearId " +
            "ORDER BY cycle.cycleMonth ASC")
    List<Contribution> findByMemberIdAndFinancialYearId(
            @Param("memberId") UUID memberId,
            @Param("financialYearId") UUID financialYearId);

    // Sum paid contributions by group and financial year
    @Query("SELECT COALESCE(SUM(c.paidAmount), 0) FROM Contribution c " +
            "WHERE c.member.group.id = :groupId " +
            "AND (:yearId IS NULL OR c.cycle.financialYear.id = :yearId)")
    BigDecimal sumPaidByGroupAndYear(@Param("groupId") UUID groupId, @Param("yearId") UUID yearId);

    // Sum paid contributions by member
    @Query("SELECT COALESCE(SUM(c.paidAmount), 0) FROM Contribution c " +
            "WHERE c.member.id = :memberId")
    BigDecimal sumPaidByMember(@Param("memberId") UUID memberId);

    // Sum expected contributions by cycle
    @Query("SELECT COALESCE(SUM(c.expectedAmount), 0) FROM Contribution c " +
            "WHERE c.cycle.id = :cycleId")
    BigDecimal sumExpectedByCycle(@Param("cycleId") UUID cycleId);

    // Sum paid contributions by cycle
    @Query("SELECT COALESCE(SUM(c.paidAmount), 0) FROM Contribution c " +
            "WHERE c.cycle.id = :cycleId")
    BigDecimal sumPaidByCycle(@Param("cycleId") UUID cycleId);

    // Sum paid contributions by group and month (using paymentDate)
    @Query("SELECT COALESCE(SUM(c.paidAmount), 0) FROM Contribution c " +
            "WHERE c.member.group.id = :groupId " +
            "AND EXTRACT(YEAR FROM c.paymentDate) = :year " +
            "AND EXTRACT(MONTH FROM c.paymentDate) = :month")
    BigDecimal sumPaidByGroupAndMonth(@Param("groupId") UUID groupId,
                                      @Param("year") int year,
                                      @Param("month") int month);

    // Find recent paid contributions by group
    @Query("SELECT c FROM Contribution c " +
            "WHERE c.member.group.id = :groupId " +
            "AND c.paidAmount > 0 " +
            "ORDER BY c.paymentDate DESC")
    List<Contribution> findRecentPaidByGroup(@Param("groupId") UUID groupId, Pageable pageable);
}
