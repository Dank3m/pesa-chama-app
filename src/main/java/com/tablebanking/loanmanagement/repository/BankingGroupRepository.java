package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.BankingGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankingGroupRepository extends JpaRepository<BankingGroup, UUID> {

    Optional<BankingGroup> findByNameIgnoreCase(String name);

    List<BankingGroup> findByIsActiveTrue();

    @Query("SELECT bg FROM BankingGroup bg LEFT JOIN FETCH bg.members WHERE bg.id = :id")
    Optional<BankingGroup> findByIdWithMembers(UUID id);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.group.id = :groupId AND m.status = 'ACTIVE'")
    int countActiveMembers(UUID groupId);

    boolean existsByNameIgnoreCase(String name);
}
