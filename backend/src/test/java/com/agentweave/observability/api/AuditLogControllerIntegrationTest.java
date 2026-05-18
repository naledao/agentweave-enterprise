package com.agentweave.observability.api;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.agentweave.shared.audit.AuditLogEntity;
import com.agentweave.shared.audit.AuditLogRepository;
import com.agentweave.shared.audit.AuditResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
class AuditLogControllerIntegrationTest {

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

    private UUID firstUserId;
    private UUID secondUserId;
    private String firstToken;
    private String adminToken;
    private String firstUsername;
    private String secondUsername;
    private String firstTraceId;
    private String secondTraceId;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        firstUsername = "audit_a_" + suffix;
        secondUsername = "audit_b_" + suffix;
        String adminUsername = "audit_admin_" + suffix;
        firstTraceId = "trace-audit-first-" + suffix;
        secondTraceId = "trace-audit-second-" + suffix;

        PermissionEntity observabilityRead =
                ensurePermission("observability:read", "Read observability", PermissionType.API);

        RoleEntity userRole = new RoleEntity(
                UUID.randomUUID(),
                "AUDIT_USER_" + suffix.toUpperCase(),
                "Audit User",
                null);
        roleRepository.save(userRole);

        RoleEntity adminRole = new RoleEntity(
                UUID.randomUUID(),
                "AUDIT_ADMIN_" + suffix.toUpperCase(),
                "Audit Admin",
                null);
        adminRole.replacePermissions(List.of(observabilityRead));
        roleRepository.save(adminRole);

        UserEntity firstUser = saveUser(firstUsername, List.of(userRole));
        UserEntity secondUser = saveUser(secondUsername, List.of(userRole));
        UserEntity admin = saveUser(adminUsername, List.of(adminRole));
        firstUserId = firstUser.getId();
        secondUserId = secondUser.getId();

        auditLogRepository.save(new AuditLogEntity(
                UUID.randomUUID(),
                AuditEventType.TOOL_INVOKE,
                firstUserId,
                firstUsername,
                "tool",
                "ticket.query",
                "invoke",
                AuditResult.SUCCESS,
                12L,
                "{\"ticketNo\":\"INC-1\"}",
                "{\"state\":\"OPEN\"}",
                null,
                "127.0.0.1",
                "test",
                firstTraceId));
        auditLogRepository.save(new AuditLogEntity(
                UUID.randomUUID(),
                AuditEventType.TOOL_INVOKE,
                secondUserId,
                secondUsername,
                "tool",
                "log.search",
                "invoke",
                AuditResult.SUCCESS,
                15L,
                "{\"keyword\":\"error\"}",
                "{\"matches\":1}",
                null,
                "127.0.0.1",
                "test",
                secondTraceId));

        firstToken = login(firstUsername, "password123");
        adminToken = login(adminUsername, "password123");
    }

    @Test
    void normalUserCanOnlyListOwnAuditLogs() throws Exception {
        mockMvc.perform(get("/api/v1/observability/audit-logs")
                        .param("eventType", "TOOL_INVOKE")
                        .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.traceId == '" + firstTraceId + "')].username")
                        .value(hasItem(firstUsername)))
                .andExpect(jsonPath("$.items[?(@.traceId == '" + secondTraceId + "')]").isEmpty());
    }

    @Test
    void observabilityReaderCanFilterAllAuditLogsByTraceId() throws Exception {
        mockMvc.perform(get("/api/v1/observability/audit-logs")
                        .param("traceId", secondTraceId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].username").value(secondUsername))
                .andExpect(jsonPath("$.items[0].resourceType").value("tool"))
                .andExpect(jsonPath("$.items[0].resourceId").value("log.search"))
                .andExpect(jsonPath("$.items[0].durationMs").value(15));
    }

    private PermissionEntity ensurePermission(String code, String name, PermissionType type) {
        return permissionRepository.findByCode(code)
                .orElseGet(() -> permissionRepository.save(
                        new PermissionEntity(UUID.randomUUID(), code, name, type, null)));
    }

    private UserEntity saveUser(String username, List<RoleEntity> roles) {
        UserEntity user = new UserEntity(
                UUID.randomUUID(),
                username,
                username,
                passwordEncoder.encode("password123"),
                username + "@example.com");
        user.replaceRoles(roles);
        return userRepository.save(user);
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
