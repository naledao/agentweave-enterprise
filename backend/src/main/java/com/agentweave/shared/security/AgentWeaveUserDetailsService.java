package com.agentweave.shared.security;

import com.agentweave.auth.domain.PermissionEntity;
import com.agentweave.auth.domain.RoleEntity;
import com.agentweave.auth.domain.UserEntity;
import com.agentweave.auth.domain.UserStatus;
import com.agentweave.auth.repository.UserRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentWeaveUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AgentWeaveUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        UserEntity user = userRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toAuthenticatedUser(user);
    }

    public AuthenticatedUser toAuthenticatedUser(UserEntity user) {
        Set<String> roles = new LinkedHashSet<>();
        Set<String> permissions = new LinkedHashSet<>();
        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();

        for (RoleEntity role : user.getRoles()) {
            roles.add(role.getCode());
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
            for (PermissionEntity permission : role.getPermissions()) {
                permissions.add(permission.getCode());
                authorities.add(new SimpleGrantedAuthority(permission.getCode()));
            }
        }

        CurrentUser currentUser = new CurrentUser(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                Set.copyOf(roles),
                Set.copyOf(permissions));
        return new AuthenticatedUser(
                currentUser,
                user.getPasswordHash(),
                Set.copyOf(authorities),
                user.getStatus() == UserStatus.ACTIVE,
                user.getTokenVersion());
    }
}
