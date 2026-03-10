package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.GroupSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupSettingsRepository extends JpaRepository<GroupSettings, UUID> {

    /**
     * Find settings by group ID.
     */
    Optional<GroupSettings> findByGroupId(UUID groupId);

    /**
     * Find settings by group ID with the group eagerly loaded.
     */
    @Query("SELECT gs FROM GroupSettings gs JOIN FETCH gs.group WHERE gs.group.id = :groupId")
    Optional<GroupSettings> findByGroupIdWithGroup(UUID groupId);

    /**
     * Check if settings exist for a group.
     */
    boolean existsByGroupId(UUID groupId);

    /**
     * Delete settings by group ID.
     */
    void deleteByGroupId(UUID groupId);
}
