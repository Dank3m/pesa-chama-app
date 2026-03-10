package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.Investment;
import com.tablebanking.loanmanagement.entity.enums.InvestmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, UUID> {

    // Find by group (non-deleted)
    @Query("SELECT i FROM Investment i WHERE i.group.id = :groupId AND i.isDeleted = false ORDER BY i.investmentDate DESC")
    List<Investment> findByGroupId(@Param("groupId") UUID groupId);

    // Find by group with pagination
    @Query("SELECT i FROM Investment i WHERE i.group.id = :groupId AND i.isDeleted = false ORDER BY i.investmentDate DESC")
    Page<Investment> findByGroupId(@Param("groupId") UUID groupId, Pageable pageable);

    // Find by member (non-deleted)
    @Query("SELECT i FROM Investment i WHERE i.member.id = :memberId AND i.isDeleted = false ORDER BY i.investmentDate DESC")
    List<Investment> findByMemberId(@Param("memberId") UUID memberId);

    // Find by group and status
    @Query("SELECT i FROM Investment i WHERE i.group.id = :groupId AND i.status = :status AND i.isDeleted = false ORDER BY i.investmentDate DESC")
    List<Investment> findByGroupIdAndStatus(@Param("groupId") UUID groupId, @Param("status") InvestmentStatus status);

    // Sum total invested amount by group
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Investment i WHERE i.group.id = :groupId AND i.isDeleted = false")
    BigDecimal sumTotalAmountByGroup(@Param("groupId") UUID groupId);

    // Sum current value by group
    @Query("SELECT COALESCE(SUM(i.currentValue), 0) FROM Investment i WHERE i.group.id = :groupId AND i.isDeleted = false")
    BigDecimal sumCurrentValueByGroup(@Param("groupId") UUID groupId);

    // Count by group and status
    @Query("SELECT COUNT(i) FROM Investment i WHERE i.group.id = :groupId AND i.status = :status AND i.isDeleted = false")
    int countByGroupAndStatus(@Param("groupId") UUID groupId, @Param("status") InvestmentStatus status);

    // Count all non-deleted by group
    @Query("SELECT COUNT(i) FROM Investment i WHERE i.group.id = :groupId AND i.isDeleted = false")
    int countByGroup(@Param("groupId") UUID groupId);
}
