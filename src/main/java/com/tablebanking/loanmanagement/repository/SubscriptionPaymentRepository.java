package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.SubscriptionPayment;
import com.tablebanking.loanmanagement.entity.enums.SubscriptionPaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, UUID> {

    @Query("SELECT sp FROM SubscriptionPayment sp " +
           "LEFT JOIN FETCH sp.plan " +
           "LEFT JOIN FETCH sp.paidBy " +
           "WHERE sp.group.id = :groupId " +
           "ORDER BY sp.createdAt DESC")
    Page<SubscriptionPayment> findByGroupIdOrderByCreatedAtDesc(@Param("groupId") UUID groupId, Pageable pageable);

    @Query("SELECT sp FROM SubscriptionPayment sp " +
           "LEFT JOIN FETCH sp.plan " +
           "LEFT JOIN FETCH sp.subscription " +
           "WHERE sp.id = :id")
    Optional<SubscriptionPayment> findByIdWithDetails(@Param("id") UUID id);

    Optional<SubscriptionPayment> findByPaymentReference(String paymentReference);

    Optional<SubscriptionPayment> findByPaymentNumber(String paymentNumber);

    @Query("SELECT sp FROM SubscriptionPayment sp " +
           "WHERE sp.subscription.id = :subscriptionId " +
           "ORDER BY sp.createdAt DESC")
    List<SubscriptionPayment> findBySubscriptionIdOrderByCreatedAtDesc(@Param("subscriptionId") UUID subscriptionId);

    @Query("SELECT sp FROM SubscriptionPayment sp " +
           "WHERE sp.status = 'PENDING' AND sp.createdAt < :cutoff")
    List<SubscriptionPayment> findStalePendingPayments(@Param("cutoff") Instant cutoff);

    @Query("SELECT COUNT(sp) FROM SubscriptionPayment sp " +
           "WHERE sp.group.id = :groupId AND sp.status = :status")
    long countByGroupIdAndStatus(@Param("groupId") UUID groupId, @Param("status") SubscriptionPaymentStatus status);

    @Query("SELECT sp FROM SubscriptionPayment sp " +
           "LEFT JOIN FETCH sp.plan " +
           "WHERE sp.group.id = :groupId AND sp.status = 'COMPLETED' " +
           "ORDER BY sp.paidAt DESC")
    List<SubscriptionPayment> findCompletedPaymentsByGroupId(@Param("groupId") UUID groupId);

    @Query("SELECT sp FROM SubscriptionPayment sp " +
           "WHERE sp.paymentMethod = 'MPESA' AND sp.status = 'PENDING' " +
           "AND sp.paymentReference = :checkoutRequestId")
    Optional<SubscriptionPayment> findPendingMpesaPayment(@Param("checkoutRequestId") String checkoutRequestId);
}
