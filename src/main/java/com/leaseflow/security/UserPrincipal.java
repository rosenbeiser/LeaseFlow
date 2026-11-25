package com.leaseflow.security;

import com.leaseflow.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Immutable principal adapter that wraps a {@link User} entity for Spring Security.
 * Exposes id, email, and role as typed fields for use in service-layer data isolation.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String fullName;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserPrincipal(User user) {
        this.id          = user.getId();
        this.email       = user.getEmail();
        this.password    = user.getPassword();
        this.fullName    = user.getFullName();
        this.role        = user.getRole().name();
        this.authorities = List.of(new SimpleGrantedAuthority(user.getRole().name()));
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user);
    }

    // ── UserDetails ───────────────────────────────────────────────────────────

    @Override public String getUsername()              { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}

