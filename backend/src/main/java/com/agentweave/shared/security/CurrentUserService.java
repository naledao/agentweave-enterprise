package com.agentweave.shared.security;

import com.agentweave.shared.exception.AccessDeniedBusinessException;
import com.agentweave.shared.exception.UnauthorizedException;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public Optional<CurrentUser> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return Optional.of(authenticatedUser.currentUser());
        }
        return Optional.empty();
    }

    public CurrentUser requireCurrentUser() {
        return getCurrentUser().orElseThrow(UnauthorizedException::new);
    }

    public boolean hasPermission(String permission) {
        return getCurrentUser()
                .map(user -> user.hasPermission(permission) || user.hasRole("ADMIN"))
                .orElse(false);
    }

    public void requirePermission(String permission) {
        if (!hasPermission(permission)) {
            throw new AccessDeniedBusinessException();
        }
    }
}
