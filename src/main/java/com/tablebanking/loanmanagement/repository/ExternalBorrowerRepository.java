package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.ExternalBorrower;
import com.tablebanking.loanmanagement.entity.enums.ExternalBorrowerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalBorrowerRepository extends JpaRepository<ExternalBorrower, UUID> {

    List<ExternalBorrower> findByGroupId(UUID groupId);

    List<ExternalBorrower> findByGroupIdAndStatus(UUID groupId, ExternalBorrowerStatus status);

    Optional<ExternalBorrower> findByGroupIdAndPhoneNumber(UUID groupId, String phoneNumber);

    Optional<ExternalBorrower> findByGroupIdAndNationalId(UUID groupId, String nationalId);

    @Query("SELECT eb FROM ExternalBorrower eb WHERE eb.group.id = :groupId " +
            "AND eb.status = 'ACTIVE' ORDER BY eb.lastName, eb.firstName")
    List<ExternalBorrower> findActiveBorrowers(@Param("groupId") UUID groupId);

    @Query("SELECT eb FROM ExternalBorrower eb " +
            "LEFT JOIN FETCH eb.loans l " +
            "WHERE eb.id = :id")
    Optional<ExternalBorrower> findByIdWithLoans(@Param("id") UUID id);

    boolean existsByGroupIdAndPhoneNumber(UUID groupId, String phoneNumber);

    boolean existsByGroupIdAndNationalId(UUID groupId, String nationalId);
}