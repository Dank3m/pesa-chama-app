package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.FinancialYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinancialYearRepository extends JpaRepository<FinancialYear, UUID> {

    List<FinancialYear> findByGroupId(UUID groupId);

    List<FinancialYear> findByGroupIdOrderByStartDateDesc(UUID groupId);

    Optional<FinancialYear> findByGroupIdAndIsCurrent(UUID groupId, Boolean isCurrent);

    @Query("SELECT fy FROM FinancialYear fy WHERE fy.group.id = :groupId AND fy.isCurrent = true")
    Optional<FinancialYear> findCurrentByGroupId(@Param("groupId") UUID groupId);

    Optional<FinancialYear> findByGroupIdAndYearName(UUID groupId, String yearName);

    @Query("SELECT fy FROM FinancialYear fy WHERE fy.group.id = :groupId " +
           "AND :date BETWEEN fy.startDate AND fy.endDate")
    Optional<FinancialYear> findByGroupIdAndDate(@Param("groupId") UUID groupId, @Param("date") LocalDate date);

    @Query("SELECT fy FROM FinancialYear fy LEFT JOIN FETCH fy.contributionCycles WHERE fy.id = :id")
    Optional<FinancialYear> findByIdWithCycles(@Param("id") UUID id);

    @Query("SELECT fy FROM FinancialYear fy WHERE fy.group.id = :groupId AND fy.isClosed = false")
    List<FinancialYear> findOpenYearsByGroupId(@Param("groupId") UUID groupId);

    boolean existsByGroupIdAndYearName(UUID groupId, String yearName);
}
