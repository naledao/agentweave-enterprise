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
import com.agentweave.shared.audit.AuditLogRepository;
import com.agentweave.tool.domain.ToolDefinitionEntity;
import com.agentweave.tool.domain.ToolRiskLevel;
import com.agentweave.tool.repository.ToolDefinitionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private String adminToken;
    private String userToken;
    private String userUsername;
    private ToolDefinitionEntity disabledTool;

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
        ensureToolDefinition(
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
                AuditEventType.TOOL_PERMISSION_DENIED, userUsername)).isZero();
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
                .map(definition -> setEnabled(definition, enabled))
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
