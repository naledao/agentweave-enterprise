package com.agentweave.auth.application;

import com.agentweave.auth.domain.UserEntity;
import com.agentweave.auth.domain.UserStatus;
import com.agentweave.auth.dto.LoginRequest;
import com.agentweave.auth.dto.LoginResponse;
import com.agentweave.auth.dto.UserResponse;
import com.agentweave.auth.repository.UserRepository;
import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import com.agentweave.shared.audit.AuditLogService;
import com.agentweave.shared.security.AgentWeaveUserDetailsService;
import com.agentweave.shared.security.AuthenticatedUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.shared.security.JwtTokenService;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AgentWeaveUserDetailsService userDetailsService;
    private final JwtTokenService jwtTokenService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AgentWeaveUserDetailsService userDetailsService,
            JwtTokenService jwtTokenService,
            CurrentUserService currentUserService,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.jwtTokenService = jwtTokenService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findWithRolesByUsername(request.username())
                .orElseThrow(() -> badCredentials(request.username()));
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Disabled user attempted login: {}", request.username());
            auditLogService.recordLoginFailure(request.username(), "User is disabled");
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw badCredentials(request.username());
        }

        user.markLoggedIn(Instant.now());
        auditLogService.recordLoginSuccess(user.getId(), user.getUsername());
        AuthenticatedUser authenticatedUser = userDetailsService.toAuthenticatedUser(user);
        String token = jwtTokenService.generateAccessToken(authenticatedUser);
        log.info("User login succeeded: {}", request.username());
        return new LoginResponse(
                token,
                "Bearer",
                jwtTokenService.accessTokenExpiresInSeconds(),
                UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser() {
        return UserResponse.from(currentUserService.requireCurrentUser());
    }

    private BusinessException badCredentials(String username) {
        log.warn("User login failed: {}", username);
        auditLogService.recordLoginFailure(username, ErrorCode.BAD_CREDENTIALS.defaultMessage());
        return new BusinessException(ErrorCode.BAD_CREDENTIALS);
    }
}
