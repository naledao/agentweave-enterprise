package com.agentweave.auth.application;

import com.agentweave.auth.domain.RoleEntity;
import com.agentweave.auth.domain.UserEntity;
import com.agentweave.auth.domain.UserStatus;
import com.agentweave.auth.dto.CreateUserRequest;
import com.agentweave.auth.dto.ResetUserPasswordRequest;
import com.agentweave.auth.dto.UpdateUserProfileRequest;
import com.agentweave.auth.dto.UpdateUserRolesRequest;
import com.agentweave.auth.dto.UpdateUserStatusRequest;
import com.agentweave.auth.dto.UserResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(String keyword, UserStatus status, Pageable pageable) {
        String normalizedKeyword = normalizeKeyword(keyword);
        Page<UserEntity> users = normalizedKeyword == null
                ? userRepository.findByStatusFilter(status, pageable)
                : userRepository.searchByKeywordAndStatus(normalizedKeyword, status, pageable);
        Map<UUID, UserEntity> usersWithRoles = userRepository.findByIdIn(
                        users.stream().map(UserEntity::getId).toList())
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        return users
                .map(user -> usersWithRoles.getOrDefault(user.getId(), user))
                .map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return UserResponse.from(findUser(id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "username already exists");
        }
        UserEntity user = new UserEntity(
                UUID.randomUUID(),
                request.username(),
                request.displayName(),
                passwordEncoder.encode(request.password()),
                request.email());
        user.replaceRoles(loadRoles(request.roleIds()));
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateProfile(UUID id, UpdateUserProfileRequest request) {
        UserEntity user = findUser(id);
        user.updateProfile(request.displayName(), request.email());
        auditLogService.recordUserProfileUpdated(user.getId());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse resetPassword(UUID id, ResetUserPasswordRequest request) {
        UserEntity user = findUser(id);
        user.resetPasswordHash(passwordEncoder.encode(request.password()));
        user.invalidateTokens();
        auditLogService.recordUserPasswordReset(user.getId());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateRoles(UUID id, UpdateUserRolesRequest request) {
        UserEntity user = findUser(id);
        List<RoleEntity> roles = loadRoles(request.roleIds());
        user.replaceRoles(roles);
        user.invalidateTokens();
        auditLogService.recordUserRoleChanged(
                user.getId(),
                roles.stream().map(RoleEntity::getCode).collect(Collectors.toUnmodifiableSet()));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateStatus(UUID id, UpdateUserStatusRequest request) {
        UserEntity user = findUser(id);
        var previousStatus = user.getStatus();
        user.setStatus(request.status());
        user.invalidateTokens();
        auditLogService.recordUserStatusChanged(user.getId(), previousStatus.name(), request.status().name());
        return UserResponse.from(user);
    }

    private UserEntity findUser(UUID id) {
        return userRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("user not found"));
    }

    private List<RoleEntity> loadRoles(Set<UUID> roleIds) {
        List<RoleEntity> roles = roleRepository.findByIdIn(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new ResourceNotFoundException("role not found");
        }
        return roles;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }
}
