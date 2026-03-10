package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.SubscriptionPlan;
import com.tablebanking.loanmanagement.entity.enums.SubscriptionPlanType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

    Optional<SubscriptionPlan> findByName(SubscriptionPlanType name);

    @Query("SELECT p FROM SubscriptionPlan p WHERE p.isActive = true ORDER BY p.sortOrder ASC")
    List<SubscriptionPlan> findAllActivePlans();

    @Query("SELECT p FROM SubscriptionPlan p ORDER BY p.sortOrder ASC")
    List<SubscriptionPlan> findAllOrderBySortOrder();

    boolean existsByName(SubscriptionPlanType name);
}
