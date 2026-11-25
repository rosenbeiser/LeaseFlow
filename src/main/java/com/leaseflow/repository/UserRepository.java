package com.leaseflow.repository;

import com.leaseflow.entity.User;
import com.leaseflow.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    /** Used by landlords to search for tenants when creating a lease. */
    Optional<User> findByEmailAndRole(String email, Role role);
}

