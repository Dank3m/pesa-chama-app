package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID> {

    Optional<UserSettings> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
