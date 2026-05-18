package com.agentweave.tool.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.agentweave.shared.audit.AuditResult;
import com.agentweave.shared.audit.AuditLogRepository;
import com.agentweave.tool.domain.ToolDefinitionEntity;
import com.agentweave.tool.domain.ToolInvocationEntity;
import com.agentweave.tool.domain.ToolInvocationStatus;
import com.agentweave.tool.domain.ToolRiskLevel;
import com.agentweave.tool.repository.ToolDefinitionRepository;
import com.agentweave.tool.repository.ToolInvocationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(properties = {
        "agentweave.tool.security.max-invocations-per-minute=2",
        "agentweave.tool.security.execution-timeout=100ms"
})
class ToolDefinitionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ToolDefinitionRepository toolDefinitionRepository;

    @Autowired
    private ToolInvocationRepository toolInvocationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    private String adminToken;
    private String userToken;
    private String userUsername;
    private ToolDefinitionEntity disabledTool;
    private ToolDefinitionEntity highRiskTool;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String adminUsername = "tool_admin_" + suffix;
        userUsername = "tool_user_" + suffix;

        PermissionEntity ticketPermission = ensurePermission("tool:ticket:query", "Query tickets", PermissionType.TOOL);
        PermissionEntity apiStatusPermission =
                ensurePermission("tool:api-status:query", "Query API status", PermissionType.TOOL);
        PermissionEntity disabledPermission =
                ensurePermission("tool:disabled-test:query", "Disabled tool", PermissionType.TOOL);

        ensureToolDefinition(
                "ticket.query",
                "工单查询",
                "tool:ticket:query",
                ToolRiskLevel.LOW,
                true);
        highRiskTool = ensureToolDefinition(
                "endpoint.status",
                "接口状态查询",
                "tool:api-status:query",
                ToolRiskLevel.LOW,
                true);
        disabledTool = ensureToolDefinition(
                "disabled.test.tool",
                "停用工具",
                disabledPermission.getCode(),
                ToolRiskLevel.HIGH,
                false);

        RoleEntity adminRole = new RoleEntity(
                UUID.randomUUID(),
                "TOOL_ADMIN_" + suffix.toUpperCase(),
                "Tool Admin",
                null);
        adminRole.replacePermissions(List.of(ticketPermission, apiStatusPermission, disabledPermission));
        roleRepository.save(adminRole);

        RoleEntity userRole = new RoleEntity(
                UUID.randomUUID(),
                "TOOL_USER_" + suffix.toUpperCase(),
                "Tool User",
                null);
        userRole.replacePermissions(List.of(ticketPermission, disabledPermission));
        roleRepository.save(userRole);

        UserEntity admin = new UserEntity(
                UUID.randomUUID(),
                adminUsername,
                "Tool Admin",
                passwordEncoder.encode("admin123"),
                adminUsername + "@example.com");
        admin.replaceRoles(List.of(adminRole));
        userRepository.save(admin);

        UserEntity user = new UserEntity(
                UUID.randomUUID(),
                userUsername,
                "Tool User",
                passwordEncoder.encode("password123"),
                userUsername + "@example.com");
        user.replaceRoles(List.of(userRole));
        userRepository.save(user);

        adminToken = login(adminUsername, "admin123");
        userToken = login(userUsername, "password123");
    }

    @Test
    void listToolsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/tools"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void authenticatedUserCanListToolDefinitionsWithAvailability() throws Exception {
        mockMvc.perform(get("/api/v1/tools").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'ticket.query')].permissionCode")
                        .value(org.hamcrest.Matchers.hasItem("tool:ticket:query")))
                .andExpect(jsonPath("$[?(@.code == 'ticket.query')].riskLevel")
                        .value(org.hamcrest.Matchers.hasItem("LOW")))
                .andExpect(jsonPath("$[?(@.code == 'ticket.query')].available")
                        .value(org.hamcrest.Matchers.hasItem(true)))
                .andExpect(jsonPath("$[?(@.code == 'endpoint.status')].available")
                        .value(org.hamcrest.Matchers.hasItem(false)))
                .andExpect(jsonPath("$[?(@.code == '" + disabledTool.getCode() + "')].enabled")
                        .value(org.hamcrest.Matchers.hasItem(false)))
                .andExpect(jsonPath("$[?(@.code == '" + disabledTool.getCode() + "')].available")
                        .value(org.hamcrest.Matchers.hasItem(false)));
    }

    @Test
    void adminCanListAllEnabledToolsAsAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/tools").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'endpoint.status')].available")
                        .value(org.hamcrest.Matchers.hasItem(true)))
                .andExpect(jsonPath("$[?(@.code == '" + disabledTool.getCode() + "')].available")
                        .value(org.hamcrest.Matchers.hasItem(false)));
    }

    @Test
    void toolCodeIsUnique() {
        ToolDefinitionEntity duplicate = new ToolDefinitionEntity(
                UUID.randomUUID(),
                disabledTool.getCode(),
                "Duplicate tool",
                null,
                "tool:ticket:query",
                ToolRiskLevel.LOW,
                true,
                null,
                null);

        assertThatThrownBy(() -> {
            toolDefinitionRepository.saveAndFlush(duplicate);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void disabledToolCannotBeInvoked() throws Exception {
        ToolDefinitionEntity ticketTool = toolDefinitionRepository.findByCode("ticket.query")
                .orElseThrow();
        setEnabled(ticketTool, false);
        try {
            mockMvc.perform(get("/api/v1/tools/demo/tickets")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_403"));
        } finally {
            setEnabled(ticketTool, true);
        }

        assertThat(auditLogRepository.countByEventTypeAndUsername(
                AuditEventType.TOOL_PERMISSION_DENIED, userUsername)).isEqualTo(1);
    }

    @Test
    void userWithPermissionCanInvokeTool() throws Exception {
        mockMvc.perform(get("/api/v1/tools/demo/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-Trace-Id", "trace-tool-allowed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolName").value("ticket-query"))
                .andExpect(jsonPath("$.result.ticketNo").value("INC-10001"));

        ToolInvocationEntity invocation = latestInvocation("trace-tool-allowed");
        assertThat(invocation.getToolCode()).isEqualTo("ticket.query");
        assertThat(invocation.getToolName()).isEqualTo("工单查询");
        assertThat(invocation.getRiskLevel()).isEqualTo(ToolRiskLevel.LOW);
        assertThat(invocation.getUsername()).isEqualTo(userUsername);
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocationStatus.SUCCESS);
        assertThat(invocation.getDurationMs()).isNotNull();
        assertThat(invocation.getResultSummary()).contains("ticket-query");
        assertThat(invocation.getErrorMessage()).isNull();
        assertThat(auditLogRepository.findByEventTypeAndUsernameOrderByCreatedAtDesc(
                        AuditEventType.TOOL_INVOKE,
                        userUsername))
                .anySatisfy(log -> {
                    assertThat(log.getResourceType()).isEqualTo("tool");
                    assertThat(log.getResourceId()).isEqualTo("ticket.query");
                    assertThat(log.getResult()).isEqualTo(AuditResult.SUCCESS);
                    assertThat(log.getRequestSummary()).contains("queryTicket");
                    assertThat(log.getResponseSummary()).contains("ticket-query");
                });
        assertThat(meterRegistry.find("agentweave.tool.call.duration")
                        .tag("toolCode", "ticket.query")
                        .tag("riskLevel", "LOW")
                        .tag("status", "SUCCESS")
                        .timer())
                .isNotNull();
    }

    @Test
    void userWithoutPermissionIsDeniedAndAudited() throws Exception {
        mockMvc.perform(get("/api/v1/tools/demo/api-status")
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-Trace-Id", "trace-tool-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"))
                .andExpect(jsonPath("$.message").value("Missing tool permission"))
                .andExpect(jsonPath("$.traceId").value("trace-tool-denied"));

        assertThat(auditLogRepository.countByEventTypeAndUsername(
                AuditEventType.TOOL_PERMISSION_DENIED, userUsername)).isEqualTo(1);

        ToolInvocationEntity invocation = latestInvocation("trace-tool-denied");
        assertThat(invocation.getToolCode()).isEqualTo("endpoint.status");
        assertThat(invocation.getToolName()).isEqualTo("接口状态查询");
        assertThat(invocation.getUsername()).isEqualTo(userUsername);
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocationStatus.DENIED);
        assertThat(invocation.getErrorMessage()).isEqualTo("Missing tool permission");
        assertThat(auditLogRepository.findByEventTypeAndUsernameOrderByCreatedAtDesc(
                        AuditEventType.TOOL_DENIED,
                        userUsername))
                .anySatisfy(log -> {
                    assertThat(log.getResourceType()).isEqualTo("tool");
                    assertThat(log.getResourceId()).isEqualTo("endpoint.status");
                    assertThat(log.getResult()).isEqualTo(AuditResult.DENIED);
                    assertThat(log.getErrorMessage()).isEqualTo("Missing tool permission");
                });
        assertThat(meterRegistry.find("agentweave.tool.call.denied")
                        .tag("toolCode", "endpoint.status")
                        .tag("status", "DENIED")
                        .counter())
                .isNotNull();
    }

    @Test
    void highRiskToolRequiresAdminRole() throws Exception {
        setRiskLevel(highRiskTool, ToolRiskLevel.HIGH);
        try {
            mockMvc.perform(get("/api/v1/tools/demo/api-status")
                            .header("Authorization", "Bearer " + userToken)
                            .header("X-Trace-Id", "trace-tool-high-risk"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_403"))
                    .andExpect(jsonPath("$.message").value("High risk tool requires administrator role"))
                    .andExpect(jsonPath("$.traceId").value("trace-tool-high-risk"));
        } finally {
            setRiskLevel(highRiskTool, ToolRiskLevel.LOW);
        }

        assertThat(auditLogRepository.countByEventTypeAndUsername(
                AuditEventType.TOOL_PERMISSION_DENIED, userUsername)).isEqualTo(1);
    }

    @Test
    void invalidToolArgumentIsDeniedBeforeBusinessExecution() throws Exception {
        mockMvc.perform(get("/api/v1/tools/demo/tickets")
                        .param("ticketNo", " ")
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-Trace-Id", "trace-tool-invalid-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.traceId").value("trace-tool-invalid-argument"));

        assertThat(auditLogRepository.countByEventTypeAndUsername(
                AuditEventType.TOOL_PERMISSION_DENIED, userUsername)).isEqualTo(1);
    }

    @Test
    void rateLimitedToolInvocationReturnsControlledError() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String rateLimitedUsername = "tool_rate_" + suffix;
        String rateLimitedToken = createUserWithPermissions(
                rateLimitedUsername,
                "password123",
                "TOOL_RATE_" + suffix.toUpperCase(),
                List.of(ensurePermission("tool:ticket:query", "Query tickets", PermissionType.TOOL)));

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/api/v1/tools/demo/tickets")
                            .header("Authorization", "Bearer " + rateLimitedToken))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/tools/demo/tickets")
                        .header("Authorization", "Bearer " + rateLimitedToken)
                        .header("X-Trace-Id", "trace-tool-rate-limit"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("COMMON_429"))
                .andExpect(jsonPath("$.traceId").value("trace-tool-rate-limit"));

        assertThat(auditLogRepository.countByEventTypeAndUsername(
                AuditEventType.TOOL_PERMISSION_DENIED, rateLimitedUsername)).isEqualTo(1);
    }

    @Test
    void timedOutToolInvocationReturnsControlledError() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String timeoutUsername = "tool_timeout_" + suffix;
        String timeoutToken = createUserWithPermissions(
                timeoutUsername,
                "password123",
                "TOOL_TIMEOUT_" + suffix.toUpperCase(),
                List.of(ensurePermission("tool:ticket:query", "Query tickets", PermissionType.TOOL)));

        mockMvc.perform(get("/api/v1/tools/demo/slow-ticket")
                        .param("delayMs", "300")
                        .header("Authorization", "Bearer " + timeoutToken)
                        .header("X-Trace-Id", "trace-tool-timeout"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("TOOL_408"))
                .andExpect(jsonPath("$.traceId").value("trace-tool-timeout"));

        ToolInvocationEntity invocation = latestInvocation("trace-tool-timeout");
        assertThat(invocation.getToolCode()).isEqualTo("ticket.query");
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocationStatus.TIMEOUT);
        assertThat(invocation.getErrorMessage()).contains("tool execution timeout");
        assertThat(invocation.getFinishedAt()).isNotNull();
        assertThat(meterRegistry.find("agentweave.tool.call.timeout")
                        .tag("toolCode", "ticket.query")
                        .tag("status", "TIMEOUT")
                        .counter())
                .isNotNull();
    }

    @Test
    void failedToolInvocationWritesErrorSummary() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String failureUsername = "tool_failure_" + suffix;
        String failureToken = createUserWithPermissions(
                failureUsername,
                "password123",
                "TOOL_FAILURE_" + suffix.toUpperCase(),
                List.of(ensurePermission("tool:ticket:query", "Query tickets", PermissionType.TOOL)));

        mockMvc.perform(get("/api/v1/tools/demo/failing-ticket")
                        .param("ticketNo", "INC-FAILED")
                        .header("Authorization", "Bearer " + failureToken)
                        .header("X-Trace-Id", "trace-tool-failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.traceId").value("trace-tool-failure"));

        ToolInvocationEntity invocation = latestInvocation("trace-tool-failure");
        assertThat(invocation.getToolCode()).isEqualTo("ticket.query");
        assertThat(invocation.getUsername()).isEqualTo(failureUsername);
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocationStatus.FAILED);
        assertThat(invocation.getErrorMessage()).contains("demo ticket tool failure");
        assertThat(invocation.getFinishedAt()).isNotNull();
        assertThat(meterRegistry.find("agentweave.tool.call.failures")
                        .tag("toolCode", "ticket.query")
                        .tag("status", "FAILED")
                        .counter())
                .isNotNull();
    }

    @Test
    void listToolInvocationsReturnsOnlyCurrentUsersRecords() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String firstUsername = "tool_list_a_" + suffix;
        String secondUsername = "tool_list_b_" + suffix;
        PermissionEntity ticketPermission = ensurePermission("tool:ticket:query", "Query tickets", PermissionType.TOOL);
        String firstToken = createUserWithPermissions(
                firstUsername,
                "password123",
                "TOOL_LIST_A_" + suffix.toUpperCase(),
                List.of(ticketPermission));
        String secondToken = createUserWithPermissions(
                secondUsername,
                "password123",
                "TOOL_LIST_B_" + suffix.toUpperCase(),
                List.of(ticketPermission));

        mockMvc.perform(get("/api/v1/tools/demo/tickets")
                        .header("Authorization", "Bearer " + firstToken)
                        .header("X-Trace-Id", "trace-tool-list-first"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/tools/demo/tickets")
                        .header("Authorization", "Bearer " + secondToken)
                        .header("X-Trace-Id", "trace-tool-list-second"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/tools/invocations")
                        .param("toolCode", "ticket.query")
                        .param("status", "success")
                        .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.traceId == 'trace-tool-list-first')].username")
                        .value(org.hamcrest.Matchers.hasItem(firstUsername)))
                .andExpect(jsonPath("$.items[?(@.traceId == 'trace-tool-list-second')]").isEmpty());
    }

    @Test
    void getToolInvocationRequiresOwnerOrAdmin() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String ownerUsername = "tool_detail_owner_" + suffix;
        String otherUsername = "tool_detail_other_" + suffix;
        String platformAdminUsername = "tool_detail_admin_" + suffix;
        PermissionEntity ticketPermission = ensurePermission("tool:ticket:query", "Query tickets", PermissionType.TOOL);
        String ownerToken = createUserWithPermissions(
                ownerUsername,
                "password123",
                "TOOL_DETAIL_OWNER_" + suffix.toUpperCase(),
                List.of(ticketPermission));
        String otherToken = createUserWithPermissions(
                otherUsername,
                "password123",
                "TOOL_DETAIL_OTHER_" + suffix.toUpperCase(),
                List.of(ticketPermission));
        String platformAdminToken = createUserWithRoles(
                platformAdminUsername,
                "password123",
                List.of(roleRepository.findByCode("ADMIN").orElseThrow()));

        mockMvc.perform(get("/api/v1/tools/demo/tickets")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Trace-Id", "trace-tool-detail-owner"))
                .andExpect(status().isOk());
        UUID invocationId = latestInvocation("trace-tool-detail-owner").getId();

        mockMvc.perform(get("/api/v1/tools/invocations/{invocationId}", invocationId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invocationId.toString()))
                .andExpect(jsonPath("$.toolCode").value("ticket.query"))
                .andExpect(jsonPath("$.toolName").value("工单查询"))
                .andExpect(jsonPath("$.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.username").value(ownerUsername))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.traceId").value("trace-tool-detail-owner"));

        mockMvc.perform(get("/api/v1/tools/invocations/{invocationId}", invocationId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_404"));

        mockMvc.perform(get("/api/v1/tools/invocations/{invocationId}", invocationId)
                        .header("Authorization", "Bearer " + platformAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invocationId.toString()));
    }

    private PermissionEntity ensurePermission(String code, String name, PermissionType type) {
        return permissionRepository.findByCode(code)
                .orElseGet(() -> permissionRepository.save(
                        new PermissionEntity(UUID.randomUUID(), code, name, type, null)));
    }

    private ToolDefinitionEntity ensureToolDefinition(
            String code,
            String name,
            String permissionCode,
            ToolRiskLevel riskLevel,
            boolean enabled) {
        return toolDefinitionRepository.findByCode(code)
                .map(definition -> setEnabledAndRiskLevel(definition, enabled, riskLevel))
                .orElseGet(() -> toolDefinitionRepository.save(new ToolDefinitionEntity(
                        UUID.randomUUID(),
                        code,
                        name,
                        "Integration test tool",
                        permissionCode,
                        riskLevel,
                        enabled,
                        "{}",
                        "{}")));
    }

    private ToolDefinitionEntity setEnabled(ToolDefinitionEntity definition, boolean enabled) {
        definition.setEnabled(enabled);
        return toolDefinitionRepository.save(definition);
    }

    private ToolDefinitionEntity setEnabledAndRiskLevel(
            ToolDefinitionEntity definition,
            boolean enabled,
            ToolRiskLevel riskLevel) {
        definition.setEnabled(enabled);
        definition.setRiskLevel(riskLevel);
        return toolDefinitionRepository.save(definition);
    }

    private ToolDefinitionEntity setRiskLevel(ToolDefinitionEntity definition, ToolRiskLevel riskLevel) {
        definition.setRiskLevel(riskLevel);
        return toolDefinitionRepository.save(definition);
    }

    private String createUserWithPermissions(
            String username,
            String password,
            String roleCode,
            List<PermissionEntity> permissions) throws Exception {
        RoleEntity role = new RoleEntity(
                UUID.randomUUID(),
                roleCode,
                "Tool Security Test Role",
                null);
        role.replacePermissions(permissions);
        roleRepository.save(role);

        UserEntity user = new UserEntity(
                UUID.randomUUID(),
                username,
                username,
                passwordEncoder.encode(password),
                username + "@example.com");
        user.replaceRoles(List.of(role));
        userRepository.save(user);
        return login(username, password);
    }

    private String createUserWithRoles(
            String username,
            String password,
            List<RoleEntity> roles) throws Exception {
        UserEntity user = new UserEntity(
                UUID.randomUUID(),
                username,
                username,
                passwordEncoder.encode(password),
                username + "@example.com");
        user.replaceRoles(roles);
        userRepository.save(user);
        return login(username, password);
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

    private ToolInvocationEntity latestInvocation(String traceId) {
        return toolInvocationRepository.findFirstByTraceIdOrderByCreatedAtDesc(traceId)
                .orElseThrow();
    }

    private record LoginPayload(String username, String password) {
    }
}
