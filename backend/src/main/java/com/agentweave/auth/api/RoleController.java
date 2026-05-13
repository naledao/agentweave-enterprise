package com.agentweave.auth.api;

import com.agentweave.auth.application.RoleManagementService;
import com.agentweave.auth.dto.CreateRoleRequest;
import com.agentweave.auth.dto.PermissionResponse;
import com.agentweave.auth.dto.RoleResponse;
import com.agentweave.auth.dto.UpdateRolePermissionsRequest;
import com.agentweave.auth.dto.UpdateRoleRequest;
import com.agentweave.auth.dto.UpdateRoleStatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleManagementService roleManagementService;

    public RoleController(RoleManagementService roleManagementService) {
        this.roleManagementService = roleManagementService;
    }

    @GetMapping(params = {"!page", "!size"})
    @PreAuthorize("hasAuthority('auth:role:read')")
    public List<RoleResponse> list() {
        return roleManagementService.listRoles();
    }

    @GetMapping(params = {"page", "size"})
    @PreAuthorize("hasAuthority('auth:role:read')")
    public Page<RoleResponse> page(Pageable pageable) {
        return roleManagementService.listRoles(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('auth:role:read')")
    public RoleResponse get(@PathVariable UUID id) {
        return roleManagementService.getRole(id);
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('auth:role:read')")
    public List<PermissionResponse> permissions() {
        return roleManagementService.listPermissions();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('auth:role:write')")
    public RoleResponse create(@Valid @RequestBody CreateRoleRequest request) {
        return roleManagementService.createRole(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('auth:role:write')")
    public RoleResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {
        return roleManagementService.updateRole(id, request);
    }

    @PatchMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('auth:role:write')")
    public RoleResponse updatePermissions(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRolePermissionsRequest request) {
        return roleManagementService.updatePermissions(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('auth:role:write')")
    public RoleResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateRoleStatusRequest request) {
        return roleManagementService.updateStatus(id, request);
    }
}
