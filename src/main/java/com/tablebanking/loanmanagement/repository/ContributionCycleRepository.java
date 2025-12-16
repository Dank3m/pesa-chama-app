package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.ContributionCycle;
import com.tablebanking.loanmanagement.entity.enums.CycleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContributionCycleRepository extends JpaRepository<ContributionCycle, UUID> {

    List<ContributionCycle> findByFinancialYearId(UUID financialYearId);

    List<ContributionCycle> findByFinancialYearIdOrderByCycleMonthAsc(UUID financialYearId);

    Optional<ContributionCycle> findByFinancialYearIdAndCycleMonth(UUID financialYearId, LocalDate cycleMonth);

    List<ContributionCycle> findByStatus(CycleStatus status);

    List<ContributionCycle> findByDueDateBetweenAndStatus(LocalDate start, LocalDate end, CycleStatus status);
    List<ContributionCycle> findByDueDateBeforeAndStatus(LocalDate date, CycleStatus status);

    @Query("SELECT cc FROM ContributionCycle cc WHERE cc.financialYear.id = :yearId AND cc.status = :status")
    List<ContributionCycle> findByFinancialYearIdAndStatus(
            @Param("yearId") UUID financialYearId, 
            @Param("status") CycleStatus status);

    @Query("SELECT cc FROM ContributionCycle cc WHERE cc.dueDate < :date AND cc.status = 'OPEN' AND cc.isProcessed = false")
    List<ContributionCycle> findOverdueCycles(@Param("date") LocalDate date);

    @Query("SELECT cc FROM ContributionCycle cc WHERE cc.financialYear.group.id = :groupId " +
           "AND :date BETWEEN cc.cycleMonth AND cc.dueDate")
    Optional<ContributionCycle> findCurrentCycleByGroupId(@Param("groupId") UUID groupId, @Param("date") LocalDate date);

    @Query("SELECT cc FROM ContributionCycle cc LEFT JOIN FETCH cc.contributions WHERE cc.id = :id")
    Optional<ContributionCycle> findByIdWithContributions(@Param("id") UUID id);

    @Query("SELECT cc FROM ContributionCycle cc WHERE cc.financialYear.group.id = :groupId " +
            "AND cc.status = 'OPEN' \n" +
            "AND YEAR(cc.cycleMonth) = YEAR(CURRENT_DATE) " +
            "AND MONTH(cc.cycleMonth) = MONTH(CURRENT_DATE) " +
            "ORDER BY cc.cycleMonth DESC")
    Optional<ContributionCycle> findLatestOpenCycleByGroupId(@Param("groupId") UUID groupId);
}
