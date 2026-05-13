package com.agentweave.auth.application;

import com.agentweave.auth.dto.PermissionResponse;
import com.agentweave.auth.dto.CreateRoleRequest;
import com.agentweave.auth.dto.RoleResponse;
import com.agentweave.auth.dto.UpdateRolePermissionsRequest;
import com.agentweave.auth.dto.UpdateRoleRequest;
import com.agentweave.auth.dto.UpdateRoleStatusRequest;
import com.agentweave.auth.domain.PermissionEntity;
import com.agentweave.auth.domain.RoleEntity;
import com.agentweave.auth.domain.RoleStatus;
import com.agentweave.auth.repository.PermissionRepository;
import com.agentweave.auth.repository.RoleRepository;
import com.agentweave.auth.repository.UserRepository;
import com.agentweave.shared.audit.AuditLogService;
import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import com.agentweave.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleManagementService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public RoleManagementService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream()
                .map(role -> roleRepository.findByCode(role.getCode()).orElse(role))
                .map(RoleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<RoleResponse> listRoles(Pageable pageable) {
        Page<RoleEntity> roles = roleRepository.findAll(pageable);
        Map<UUID, RoleEntity> rolesWithPermissions = roleRepository.findByIdIn(
                        roles.stream().map(RoleEntity::getId).toList())
                .stream()
                .collect(Collectors.toMap(RoleEntity::getId, Function.identity()));
        return roles
                .map(role -> rolesWithPermissions.getOrDefault(role.getId(), role))
                .map(RoleResponse::from);
    }

    @Transactional(readOnly = true)
    public RoleResponse getRole(UUID id) {
        return RoleResponse.from(findRole(id));
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll().stream()
                .map(PermissionResponse::from)
                .toList();
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        String code = normalizeCode(request.code());
        if (roleRepository.existsByCode(code)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "role code already exists");
        }
        RoleEntity role = new RoleEntity(UUID.randomUUID(), code, request.name(), request.description());
        role.replacePermissions(loadPermissions(request.permissionIds()));
        RoleEntity saved = roleRepository.save(role);
        auditLogService.recordRoleCreated(saved.getId(), saved.getCode());
        return RoleResponse.from(saved);
    }

    @Transactional
    public RoleResponse updateRole(UUID id, UpdateRoleRequest request) {
        RoleEntity role = findRole(id);
        role.updateProfile(request.name(), request.description());
        auditLogService.recordRoleUpdated(role.getId(), role.getCode());
        return RoleResponse.from(role);
    }

    @Transactional
    public RoleResponse updatePermissions(UUID id, UpdateRolePermissionsRequest request) {
        RoleEntity role = findRole(id);
        List<PermissionEntity> permissions = loadPermissions(request.permissionIds());
        role.replacePermissions(permissions);
        userRepository.incrementTokenVersionByRoleId(role.getId());
        auditLogService.recordRolePermissionsChanged(
                role.getId(),
                role.getCode(),
                permissions.stream().map(PermissionEntity::getCode).collect(Collectors.toUnmodifiableSet()));
        return RoleResponse.from(role);
    }

    @Transactional
    public RoleResponse updateStatus(UUID id, UpdateRoleStatusRequest request) {
        RoleEntity role = findRole(id);
        if (request.status() == RoleStatus.DISABLED && userRepository.existsByRoles_IdIn(Set.of(role.getId()))) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "role is assigned to users");
        }
        var previousStatus = role.getStatus();
        role.setStatus(request.status());
        userRepository.incrementTokenVersionByRoleId(role.getId());
        auditLogService.recordRoleStatusChanged(role.getId(), role.getCode(), previousStatus.name(), request.status().name());
        return RoleResponse.from(role);
    }

    private RoleEntity findRole(UUID id) {
        return roleRepository.findById(id)
                .flatMap(role -> roleRepository.findByCode(role.getCode()))
                .orElseThrow(() -> new ResourceNotFoundException("role not found"));
    }

    private List<PermissionEntity> loadPermissions(Set<UUID> permissionIds) {
        List<PermissionEntity> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new ResourceNotFoundException("permission not found");
        }
        return permissions;
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }
}
