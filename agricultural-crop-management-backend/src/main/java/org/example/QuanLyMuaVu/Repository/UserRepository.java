package org.example.QuanLyMuaVu.Repository;

import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
        boolean existsByUsername(String username);

        Optional<User> findByUsername(String username);

        @Query("select u from User u left join fetch u.roles where u.username = :username")
        Optional<User> findByUsernameWithRoles(@Param("username") String username);

        List<User> findAllByRoles_Code(String roleCode);

        Page<User> findAllByRoles_Code(String roleCode, Pageable pageable);

        long countByStatus(UserStatus status);

        Page<User> findAllByRoles_CodeAndUsernameContainingIgnoreCase(String roleCode, String keyword,
                        Pageable pageable);

        Page<User> findAllByRoles_CodeAndStatus(String roleCode, UserStatus status, Pageable pageable);

        Page<User> findAllByRoles_CodeAndStatusAndUsernameContainingIgnoreCase(
                        String roleCode,
                        UserStatus status,
                        String keyword,
                        Pageable pageable);

        /**
         * Find user by email (case-insensitive) with roles eagerly fetched.
         * Used for authentication to avoid N+1 query on roles.
         */
        @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE LOWER(u.email) = LOWER(:email)")
        Optional<User> findByEmailIgnoreCaseWithRoles(@Param("email") String email);

        /**
         * Find user by identifier (either username or email, case-insensitive) with
         * roles eagerly fetched.
         * Used for login that accepts either username or email.
         *
         * @param identifier the username or email to search for
         * @return Optional containing the user with roles loaded
         */
        @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE LOWER(u.email) = LOWER(:identifier) OR LOWER(u.username) = LOWER(:identifier)")
        Optional<User> findByIdentifierWithRoles(@Param("identifier") String identifier);

        boolean existsByEmailIgnoreCase(String email);

        // ═══════════════════════════════════════════════════════════════
        // ADMIN QUERY METHODS
        // ═══════════════════════════════════════════════════════════════

        /**
         * Count users who joined after a specific date/time.
         * Used for admin dashboard "new users this month".
         */
        @Query("SELECT COUNT(u) FROM User u WHERE u.joinedDate >= :joinDateTime")
        long countByJoinedDateAfter(@Param("joinDateTime") java.time.LocalDateTime joinDateTime);

        /**
         * Search users by keyword (username, email, or fullName).
         * Used for admin user search.
         */
        @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
