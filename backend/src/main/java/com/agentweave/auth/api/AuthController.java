package com.agentweave.auth.api;

import com.agentweave.auth.application.AuthService;
import com.agentweave.auth.dto.LoginRequest;
import com.agentweave.auth.dto.LoginResponse;
import com.agentweave.auth.dto.UserResponse;
import com.agentweave.shared.audit.AuditLogService;
import com.agentweave.shared.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public AuthController(
            AuthService authService,
            CurrentUserService currentUserService,
            AuditLogService auditLogService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me() {
        return authService.currentUser();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        currentUserService.getCurrentUser().ifPresent(auditLogService::recordLogout);
        return ResponseEntity.noContent().build();
    }
}
