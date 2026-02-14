package com.acm.auth.entity;

import java.time.LocalDateTime;
import java.util.Set;

import com.acm.auth.enums.UserStatus;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    Long id;

    @Column(name = "user_name", unique = true, length = 255)
    String username;

    @Column(name = "email", unique = true)
    String email;

    @Column(name = "phone", length = 30)
    String phone;

    @Column(name = "full_name")
    String fullName;

    @Column(name = "password_hash")
    String password;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    UserStatus status = UserStatus.ACTIVE;

    @Column(name = "locked_until")
    LocalDateTime lockedUntil;

    @Column(name = "google_id", unique = true)
    String googleId;

    @ManyToMany(cascade = { CascadeType.MERGE }, fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    Set<Role> roles;

    @Column(name = "joined_date")
    LocalDateTime joinedDate;
}
