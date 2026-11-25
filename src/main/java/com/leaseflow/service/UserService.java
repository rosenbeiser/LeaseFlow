package com.leaseflow.service;

import com.leaseflow.entity.User;
import com.leaseflow.enums.Role;
import com.leaseflow.exception.ResourceNotFoundException;
import com.leaseflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for user account management.
 *
 * <p>Handles profile retrieval, profile updates, and password changes.
 * Password operations use BCrypt via the injected {@link PasswordEncoder}.
 *
 * @author Student 2207082, Student 2207084
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public List<User> findTenants() {
        return userRepository.findByRole(Role.ROLE_TENANT);
    }

    public User findTenantByEmail(String email) {
        return userRepository.findByEmailAndRole(email, Role.ROLE_TENANT)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No tenant found with email: " + email));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Update the full name and/or phone of an existing user.
     *
     * @param userId   the id of the user to update
     * @param fullName new full name (must not be blank)
     * @param phone    new phone number (may be null to clear)
     * @return the saved user entity
     */
    @Transactional
    public User updateProfile(Long userId, String fullName, String phone) {
        User user = findById(userId);
        user.setFullName(fullName);
        user.setPhone(phone);
        return userRepository.save(user);
    }

    /**
     * Change the password for a user after verifying the current password.
     *
     * @param userId          the id of the user changing their password
     * @param currentPassword the user's current plain-text password (verified against stored hash)
     * @param newPassword     the new plain-text password (will be hashed with BCrypt)
     * @throws IllegalArgumentException if {@code currentPassword} does not match the stored hash
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}

