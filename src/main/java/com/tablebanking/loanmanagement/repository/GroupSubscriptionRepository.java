package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.GroupSubscription;
import com.tablebanking.loanmanagement.entity.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupSubscriptionRepository extends JpaRepository<GroupSubscription, UUID> {

    @Query("SELECT gs FROM GroupSubscription gs " +
           "LEFT JOIN FETCH gs.plan " +
           "LEFT JOIN FETCH gs.group " +
           "WHERE gs.group.id = :groupId AND gs.status = 'ACTIVE'")
    Optional<GroupSubscription> findActiveByGroupId(@Param("groupId") UUID groupId);

    @Query("SELECT gs FROM GroupSubscription gs " +
           "LEFT JOIN FETCH gs.plan " +
           "WHERE gs.group.id = :groupId " +
           "ORDER BY gs.createdAt DESC")
    List<GroupSubscription> findByGroupIdOrderByCreatedAtDesc(@Param("groupId") UUID groupId);

    @Query("SELECT gs FROM GroupSubscription gs " +
           "LEFT JOIN FETCH gs.plan " +
           "LEFT JOIN FETCH gs.group " +
           "WHERE gs.status = 'ACTIVE' AND gs.endDate IS NOT NULL AND gs.endDate <= :date")
    List<GroupSubscription> findExpiredSubscriptions(@Param("date") LocalDate date);

    @Query("SELECT gs FROM GroupSubscription gs " +
           "LEFT JOIN FETCH gs.plan " +
           "LEFT JOIN FETCH gs.group " +
           "WHERE gs.status = 'ACTIVE' AND gs.endDate IS NOT NULL " +
           "AND gs.endDate > :today AND gs.endDate <= :warningDate")
    List<GroupSubscription> findExpiringSoon(@Param("today") LocalDate today, @Param("warningDate") LocalDate warningDate);

    @Query("SELECT COUNT(gs) > 0 FROM GroupSubscription gs " +
           "WHERE gs.group.id = :groupId AND gs.status = :status")
    boolean existsByGroupIdAndStatus(@Param("groupId") UUID groupId, @Param("status") SubscriptionStatus status);

    @Query("SELECT gs FROM GroupSubscription gs " +
           "LEFT JOIN FETCH gs.plan " +
           "WHERE gs.group.id = :groupId AND gs.status = :status")
    Optional<GroupSubscription> findByGroupIdAndStatus(@Param("groupId") UUID groupId, @Param("status") SubscriptionStatus status);

    @Query("SELECT gs FROM GroupSubscription gs " +
           "LEFT JOIN FETCH gs.plan " +
           "LEFT JOIN FETCH gs.group g " +
           "LEFT JOIN FETCH g.members " +
           "WHERE gs.status = 'ACTIVE' AND gs.autoRenew = true AND gs.endDate = :date")
    List<GroupSubscription> findSubscriptionsForAutoRenewal(@Param("date") LocalDate date);
}
