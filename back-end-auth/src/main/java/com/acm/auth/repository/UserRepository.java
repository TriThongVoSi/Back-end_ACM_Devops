package com.acm.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.acm.auth.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    /**
     * Find user by identifier (email OR username) with roles eagerly loaded.
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :identifier OR u.username = :identifier")
    Optional<User> findByIdentifierWithRoles(@Param("identifier") String identifier);

    /**
     * Find user by Google ID with roles eagerly loaded.
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.googleId = :googleId")
    Optional<User> findByGoogleIdWithRoles(@Param("googleId") String googleId);
}
