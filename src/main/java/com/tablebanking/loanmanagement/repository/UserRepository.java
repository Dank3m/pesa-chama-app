package com.tablebanking.loanmanagement.repository;

import com.tablebanking.loanmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
}
