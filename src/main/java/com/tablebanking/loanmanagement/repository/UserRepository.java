package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.User;
import com.tablebanking.loanmanagement.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByMemberId(UUID memberId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.member WHERE u.username = :username")
    Optional<User> findByUsernameWithMember(String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.member m LEFT JOIN FETCH m.group WHERE u.id = :id")
    Optional<User> findByIdWithMemberAndGroup(UUID id);

    @Query("SELECT u FROM User u JOIN u.member m WHERE m.group.id = :groupId AND u.role IN :roles AND u.isEnabled = true")
    List<User> findByMemberGroupIdAndRoleIn(@Param("groupId") UUID groupId, @Param("roles") List<UserRole> roles);
}
