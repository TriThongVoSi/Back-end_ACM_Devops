package com.acm.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    Long id;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    String code;

    @Column(name = "name", nullable = false, length = 100)
    String name;

    @Column(name = "description")
    String description;

    /**
     * Priority for determining primary role (higher = more important).
     * When a user has multiple roles, the role with highest priority becomes
     * primary.
     */
    @Builder.Default
    @Column(name = "priority", nullable = false)
    Integer priority = 0;

    /**
     * Redirect path after successful login for users with this role as primary.
     * Example: "/admin", "/dashboard", "/home"
     */
    @Builder.Default
    @Column(name = "redirect_path", length = 100)
    String redirectPath = "/dashboard";
}
