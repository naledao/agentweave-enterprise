package com.agentweave.auth.api;

import com.agentweave.auth.application.UserManagementService;
import com.agentweave.auth.domain.UserStatus;
import com.agentweave.auth.dto.CreateUserRequest;
import com.agentweave.auth.dto.ResetUserPasswordRequest;
import com.agentweave.auth.dto.UpdateUserProfileRequest;
import com.agentweave.auth.dto.UpdateUserRolesRequest;
import com.agentweave.auth.dto.UpdateUserStatusRequest;
import com.agentweave.auth.dto.UserResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserManagementService userManagementService;

    public UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('auth:user:read')")
    public Page<UserResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            Pageable pageable) {
        return userManagementService.list(keyword, status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('auth:user:read')")
    public UserResponse get(@PathVariable UUID id) {
        return userManagementService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('auth:user:write')")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userManagementService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('auth:user:write')")
    public UserResponse updateProfile(@PathVariable UUID id, @Valid @RequestBody UpdateUserProfileRequest request) {
        return userManagementService.updateProfile(id, request);
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("hasAuthority('auth:user:write')")
    public UserResponse resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetUserPasswordRequest request) {
        return userManagementService.resetPassword(id, request);
    }

    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('auth:user:write')")
    public UserResponse updateRoles(@PathVariable UUID id, @Valid @RequestBody UpdateUserRolesRequest request) {
        return userManagementService.updateRoles(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('auth:user:write')")
    public UserResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateUserStatusRequest request) {
        return userManagementService.updateStatus(id, request);
    }
}
