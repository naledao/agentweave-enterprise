package com.agentweave.shared.security;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthenticatedUser implements UserDetails {

    private final CurrentUser currentUser;
    private final String passwordHash;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;
    private final long tokenVersion;

    public AuthenticatedUser(
            CurrentUser currentUser,
            String passwordHash,
            Collection<? extends GrantedAuthority> authorities,
            boolean enabled,
            long tokenVersion) {
        this.currentUser = currentUser;
        this.passwordHash = passwordHash;
        this.authorities = authorities;
        this.enabled = enabled;
        this.tokenVersion = tokenVersion;
    }

    public UUID id() {
        return currentUser.id();
    }

    public String displayName() {
        return currentUser.displayName();
    }

    public Set<String> roles() {
        return currentUser.roles();
    }

    public Set<String> permissions() {
        return currentUser.permissions();
    }

    public CurrentUser currentUser() {
        return currentUser;
    }

    public long tokenVersion() {
        return tokenVersion;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return currentUser.username();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
