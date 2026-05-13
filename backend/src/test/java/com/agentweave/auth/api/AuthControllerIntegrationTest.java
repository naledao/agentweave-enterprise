package com.agentweave.auth.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agentweave.auth.domain.PermissionEntity;
import com.agentweave.auth.domain.PermissionType;
import com.agentweave.auth.domain.RoleEntity;
import com.agentweave.auth.domain.UserEntity;
import com.agentweave.auth.repository.PermissionRepository;
import com.agentweave.auth.repository.RoleRepository;
import com.agentweave.auth.repository.UserRepository;
import com.agentweave.shared.audit.AuditEventType;
import com.agentweave.shared.audit.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private String adminToken;
    private String adminUsername;
    private String userUsername;
    private String roleCode;
    private UUID aliceId;
    private UUID userRoleId;
    private UUID userReadPermissionId;
    private UUID roleReadPermissionId;
    private UUID roleWritePermissionId;
    private UUID ticketToolPermissionId;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        adminUsername = "it_admin_" + suffix;
        userUsername = "it_alice_" + suffix;
        roleCode = "IT_USER_" + suffix.toUpperCase();

        PermissionEntity userRead = ensurePermission("auth:user:read", "Read users", PermissionType.API);
        PermissionEntity userWrite = ensurePermission("auth:user:write", "Write users", PermissionType.API);
        PermissionEntity roleRead = ensurePermission("auth:role:read", "Read roles", PermissionType.API);
        PermissionEntity roleWrite = ensurePermission("auth:role:write", "Write roles", PermissionType.API);
        PermissionEntity ticketTool = ensurePermission("tool:ticket:query", "Query tickets", PermissionType.TOOL);
        PermissionEntity apiStatusTool = ensurePermission("tool:api-status:query", "Query API status", PermissionType.TOOL);

        userReadPermissionId = userRead.getId();
        roleReadPermissionId = roleRead.getId();
        roleWritePermissionId = roleWrite.getId();
        ticketToolPermissionId = ticketTool.getId();

        RoleEntity adminRole = new RoleEntity(UUID.randomUUID(), "IT_ADMIN_" + suffix.toUpperCase(), "Integration Admin", null);
        adminRole.replacePermissions(List.of(userRead, userWrite, roleRead, roleWrite, ticketTool, apiStatusTool));
        roleRepository.save(adminRole);

        RoleEntity userRole = new RoleEntity(UUID.randomUUID(), roleCode, "Integration User", null);
        userRole.replacePermissions(List.of(ticketTool));
        userRole = roleRepository.save(userRole);
        userRoleId = userRole.getId();

        UserEntity admin = new UserEntity(
                UUID.randomUUID(),
                adminUsername,
                "Integration Admin",
                passwordEncoder.encode("admin123"),
                adminUsername + "@example.com");
        admin.replaceRoles(List.of(adminRole));
        userRepository.save(admin);

        UserEntity user = new UserEntity(
                UUID.randomUUID(),
                userUsername,
                "Alice",
                passwordEncoder.encode("password123"),
                userUsername + "@example.com");
        user.replaceRoles(List.of(userRole));
        user = userRepository.save(user);
        aliceId = user.getId();

        adminToken = login(adminUsername, "admin123");
    }

    @Test
    void loginReturnsAccessTokenAndCurrentUser() throws Exception {
        String token = login(userUsername, "password123");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(userUsername))
                .andExpect(jsonPath("$.roles").isArray());
        org.assertj.core.api.Assertions.assertThat(auditLogRepository.countByEventTypeAndUsername(
                AuditEventType.LOGIN_SUCCESS, userUsername)).isEqualTo(1);
    }

    @Test
    void protectedEndpointRequiresToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void methodSecurityDeniesMissingAuthority() throws Exception {
        String userToken = login(userUsername, "password123");

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));
        org.assertj.core.api.Assertions.assertThat(auditLogRepository.countByEventTypeAndUsername(
                AuditEventType.PERMISSION_DENIED, userUsername)).isEqualTo(1);
    }

    @Test
    void adminCanListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void adminCanFilterUsersByKeywordAndStatus() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .param("keyword", userUsername)
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value(userUsername))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));

        String statusBody = objectMapper.writeValueAsString(Map.of("status", "DISABLED"));
        mockMvc.perform(patch("/api/v1/users/{id}/status", aliceId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        mockMvc.perform(get("/api/v1/users")
                        .param("keyword", userUsername.substring(3))
                        .param("status", "DISABLED")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value(userUsername))
                .andExpect(jsonPath("$.content[0].status").value("DISABLED"));

        mockMvc.perform(get("/api/v1/users")
                        .param("keyword", "missing-user-" + UUID.randomUUID())
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void adminCanUpdateUserProfileAndResetPassword() throws Exception {
        String previousUserToken = login(userUsername, "password123");
        String nextEmail = "updated_" + userUsername + "@example.com";
        String profileBody = objectMapper.writeValueAsString(Map.of(
                "displayName", "Alice Updated",
                "email", nextEmail));
        mockMvc.perform(put("/api/v1/users/{id}", aliceId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alice Updated"))
                .andExpect(jsonPath("$.email").value(nextEmail));

        String passwordBody = objectMapper.writeValueAsString(Map.of("password", "newPassword123"));
        mockMvc.perform(patch("/api/v1/users/{id}/password", aliceId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(userUsername));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + previousUserToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));

        login(userUsername, "newPassword123");

        org.assertj.core.api.Assertions.assertThat(auditLogRepository.countByEventTypeAndUsername(
                AuditEventType.USER_PROFILE_UPDATED, adminUsername)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(auditLogRepository.countByEventTypeAndUsername(
                AuditEventType.USER_PASSWORD_RESET, adminUsername)).isEqualTo(1);
    }

    @Test
    void userCannotUpdateProfileOrResetPassword() throws Exception {
        String userToken = login(userUsername, "password123");
        String profileBody = objectMapper.writeValueAsString(Map.of(
                "displayName", "Denied Update",
                "email", "denied_" + userUsername + "@example.com"));
        mockMvc.perform(put("/api/v1/users/{id}", aliceId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));

        String passwordBody = objectMapper.writeValueAsString(Map.of("password", "newPassword123"));
        mockMvc.perform(patch("/api/v1/users/{id}/password", aliceId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));
    }

    @Test
    void toolPermissionAspectAllowsAndDeniesToolAccess() throws Exception {
        String userToken = login(userUsername, "password123");

        mockMvc.perform(get("/api/v1/tools/demo/tickets").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolName").value("ticket-query"));

        mockMvc.perform(get("/api/v1/tools/demo/api-status").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        org.assertj.core.api.Assertions.assertThat(auditLogRepository.countByEventTypeAndUsername(
                AuditEventType.TOOL_PERMISSION_DENIED, userUsername)).isEqualTo(1);
    }

    @Test
    void failedLoginWritesAuditLog() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginPayload(userUsername, "wrong-password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        org.assertj.core.api.Assertions.assertThat(auditLogRepository.countByEventTypeAndUsername(
                AuditEventType.LOGIN_FAILED, userUsername)).isEqualTo(1);
    }

    @Test
    void userStatusAndRoleChangesWriteAuditLogs() throws Exception {
        String previousUserToken = login(userUsername, "password123");
        String rolesBody = objectMapper.writeValueAsString(Map.of("roleIds", List.of(userRoleId)));
        mockMvc.perform(patch("/api/v1/users/{id}/roles", aliceId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rolesBody))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + previousUserToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));

        String tokenAfterRoleChange = login(userUsername, "password123");
        String statusBody = objectMapper.writeValueAsString(Map.of("status", "DISABLED"));
        mockMvc.perform(patch("/api/v1/users/{id}/status", aliceId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokenAfterRoleChange))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));

        org.assertj.core.api.Assertions.assertThat(auditLogRepository.countByEventTypeAndUsername(
                AuditEventType.USER_STATUS_CHANGED, adminUsername)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(auditLogRepository.countByEventTypeAndUsername(
                AuditEventType.USER_ROLE_CHANGED, adminUsername)).isEqualTo(1);
    }

    @Test
    void adminCanCreateUpdateAndDisableUnassignedRole() throws Exception {
        String roleCode = "IT_WRITER_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String createBody = objectMapper.writeValueAsString(Map.of(
                "code", roleCode,
                "name", "Integration Writer",
                "description", "Created by integration test",
                "permissionIds", List.of(userReadPermissionId, roleReadPermissionId)));

        MvcResult createResult = mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(roleCode))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        UUID roleId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText());

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "name", "Integration Writer Updated",
                "description", "Updated by integration test"));
        mockMvc.perform(put("/api/v1/roles/{id}", roleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Integration Writer Updated"));

        String permissionsBody = objectMapper.writeValueAsString(Map.of(
                "permissionIds", List.of(userReadPermissionId, roleReadPermissionId, roleWritePermissionId)));
        mockMvc.perform(patch("/api/v1/roles/{id}/permissions", roleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(permissionsBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.length()").value(3));

        String statusBody = objectMapper.writeValueAsString(Map.of("status", "DISABLED"));
        mockMvc.perform(patch("/api/v1/roles/{id}/status", roleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        org.assertj.core.api.Assertions.assertThat(auditLogRepository.countByEventTypeAndUsername(
                AuditEventType.ROLE_PERMISSION_CHANGED, adminUsername)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(auditLogRepository.countByEventTypeAndUsername(
                AuditEventType.ROLE_STATUS_CHANGED, adminUsername)).isEqualTo(1);
    }

    @Test
    void rolePermissionChangeInvalidatesAssignedUserToken() throws Exception {
        String userToken = login(userUsername, "password123");

        String permissionsBody = objectMapper.writeValueAsString(Map.of(
                "permissionIds", List.of(ticketToolPermissionId, roleReadPermissionId)));
        mockMvc.perform(patch("/api/v1/roles/{id}/permissions", userRoleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(permissionsBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.length()").value(2));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));

        String nextUserToken = login(userUsername, "password123");
        mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", "Bearer " + nextUserToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanPageRoles() throws Exception {
        mockMvc.perform(get("/api/v1/roles")
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].permissions").isArray())
                .andExpect(jsonPath("$.page.size").value(1));
    }

    @Test
    void userCannotCreateRoleAndAssignedRoleCannotBeDisabled() throws Exception {
        String userToken = login(userUsername, "password123");
        String createBody = objectMapper.writeValueAsString(Map.of(
                "code", "IT_DENIED_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "name", "Denied Role",
                "permissionIds", List.of(ticketToolPermissionId)));

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));

        String statusBody = objectMapper.writeValueAsString(Map.of("status", "DISABLED"));
        mockMvc.perform(patch("/api/v1/roles/{id}/status", userRoleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    private PermissionEntity ensurePermission(String code, String name, PermissionType type) {
        return permissionRepository.findByCode(code)
                .orElseGet(() -> permissionRepository.save(
                        new PermissionEntity(UUID.randomUUID(), code, name, type, null)));
    }

    private String login(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginPayload(username, password));
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private record LoginPayload(String username, String password) {
    }
}
