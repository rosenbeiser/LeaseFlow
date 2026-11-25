package com.leaseflow.entity;

import com.leaseflow.enums.Role;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a system user. Roles determine access rights:
 * <ul>
 *   <li>{@code ROLE_LANDLORD} – owns properties, manages leases and readings.</li>
 *   <li>{@code ROLE_TENANT}   – views own leases, invoices, and makes payments.</li>
 *   <li>{@code ROLE_ADMIN}    – system administrator; seeded, not self-registrable.</li>
 * </ul>
 * Passwords are stored as BCrypt hashes (cost factor 12).
 */
@Entity
@Table(name = "users",
        indexes = @Index(name = "idx_users_email", columnList = "email", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;
}

