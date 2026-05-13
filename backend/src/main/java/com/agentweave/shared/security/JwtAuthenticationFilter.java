package com.agentweave.shared.security;

import com.agentweave.auth.domain.UserEntity;
import com.agentweave.auth.domain.UserStatus;
import com.agentweave.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.JwtException;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;
    private final AgentWeaveUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            UserRepository userRepository,
            AgentWeaveUserDetailsService userDetailsService) {
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveBearerToken(request);
        if (token == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        authenticateToken(request, token);
        filterChain.doFilter(request, response);
    }

    private void authenticateToken(HttpServletRequest request, String token) {
        if (!jwtTokenService.isTokenValid(token)) {
            return;
        }
        UUID userId;
        long tokenVersion;
        try {
            userId = jwtTokenService.parseUserId(token);
            tokenVersion = jwtTokenService.parseTokenVersion(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return;
        }
        Optional<UserEntity> userOptional = userRepository.findWithRolesById(userId);
        if (userOptional.isEmpty() || userOptional.get().getStatus() != UserStatus.ACTIVE) {
            return;
        }
        if (tokenVersion != userOptional.get().getTokenVersion()) {
            return;
        }

        AuthenticatedUser user = userDetailsService.toAuthenticatedUser(userOptional.get());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authHeader.substring(BEARER_PREFIX.length());
    }
}
