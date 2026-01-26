package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.Member;
import com.tablebanking.loanmanagement.entity.enums.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {

    List<Member> findByGroupId(UUID groupId);

    List<Member> findByGroupIdAndStatus(UUID groupId, MemberStatus status);

    Page<Member> findByGroupId(UUID groupId, Pageable pageable);

    Optional<Member> findByGroupIdAndMemberNumber(UUID groupId, String memberNumber);

    Optional<Member> findByGroupIdAndPhoneNumber(UUID groupId, String phoneNumber);

    Optional<Member> findByGroupIdAndNationalId(UUID groupId, String nationalId);

    Optional<Member> findByGroupIdAndEmail(UUID groupId, String email);

    /**
     * @deprecated Use findByGroupIdAndPhoneNumber instead to prevent cross-group account mixing
     */
    @Deprecated
    Optional<Member> findByPhoneNumber(String phoneNumber);

    /**
     * @deprecated Use findByGroupIdAndNationalId instead to prevent cross-group account mixing
     */
    @Deprecated
    Optional<Member> findByNationalId(String nationalId);

    /**
     * @deprecated Use findByGroupIdAndMemberNumber instead to prevent cross-group account mixing
     */
    @Deprecated
    Optional<Member> findByMemberNumber(String memberNumber);

    @Query("SELECT m FROM Member m WHERE m.group.id = :groupId AND m.status = 'ACTIVE'")
    List<Member> findActiveMembers(@Param("groupId") UUID groupId);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.group.id = :groupId AND m.status = :status")
    long countByGroupIdAndStatus(@Param("groupId") UUID groupId, @Param("status") MemberStatus status);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.contributions WHERE m.id = :id")
    Optional<Member> findByIdWithContributions(@Param("id") UUID id);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.loans WHERE m.id = :id")
    Optional<Member> findByIdWithLoans(@Param("id") UUID id);

    boolean existsByGroupIdAndPhoneNumber(UUID groupId, String phoneNumber);

    boolean existsByGroupIdAndMemberNumber(UUID groupId, String memberNumber);

    boolean existsByGroupIdAndEmail(UUID groupId, String email);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(m.memberNumber, 6) AS integer)), 0) FROM Member m WHERE m.group.id = :groupId")
    int getMaxMemberNumber(@Param("groupId") UUID groupId);

    // Count active members by group
    @Query("SELECT COUNT(m) FROM Member m " +
            "WHERE m.group.id = :groupId " +
            "AND m.status = 'ACTIVE'")
    int countActiveByGroup(@Param("groupId") UUID groupId);

    // Count all members by group
    @Query("SELECT COUNT(m) FROM Member m WHERE m.group.id = :groupId")
    int countByGroupId(@Param("groupId") UUID groupId);

    // Find all members with same email across all groups (for multi-group support)
    @Query("SELECT m FROM Member m JOIN FETCH m.group WHERE m.email = :email AND m.status = 'ACTIVE'")
    List<Member> findAllByEmailWithGroup(@Param("email") String email);

    // Check if email exists in multiple groups
    @Query("SELECT COUNT(DISTINCT m.group.id) FROM Member m WHERE m.email = :email AND m.status = 'ACTIVE'")
    int countGroupsByEmail(@Param("email") String email);
}
