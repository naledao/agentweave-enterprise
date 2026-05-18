package com.agentweave.observability.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import com.agentweave.auth.domain.PermissionEntity;
import com.agentweave.auth.domain.PermissionType;
import com.agentweave.auth.domain.RoleEntity;
import com.agentweave.auth.domain.UserEntity;
import com.agentweave.auth.repository.PermissionRepository;
import com.agentweave.auth.repository.RoleRepository;
import com.agentweave.auth.repository.UserRepository;
import com.agentweave.observability.application.AgentWeaveMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureObservability
@AutoConfigureMockMvc
@SpringBootTest
class ActuatorMicrometerIntegrationTest {

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
    private AgentWeaveMetrics agentWeaveMetrics;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        PermissionEntity observabilityRead =
                ensurePermission("observability:read", "Read observability", PermissionType.API);
        RoleEntity role = new RoleEntity(
                UUID.randomUUID(),
                "ACTUATOR_OBSERVER_" + suffix.toUpperCase(),
                "Actuator Observer",
                null);
        role.replacePermissions(List.of(observabilityRead));
        roleRepository.save(role);
        UserEntity user = saveUser("actuator_observer_" + suffix, List.of(role));
        token = login(user.getUsername(), "password123");
        agentWeaveMetrics.recordVectorSearch("VECTOR_ONLY", "order", "INTERNAL", "SUCCESS", 12, 1, 1);
    }

    @Test
    void healthEndpointsArePublicButMetricsEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(200, 503))
                .andExpect(jsonPath("$.status").isString());

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").isString());

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanReadMetricsAndPrometheusOutput() throws Exception {
        mockMvc.perform(get("/actuator/metrics/agentweave.vector.search.duration")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("agentweave.vector.search.duration"));

        mockMvc.perform(get("/actuator/prometheus")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("agentweave_vector_search_duration")));
    }

    @Test
    void observabilitySummaryRequiresAuthenticationAndReturnsCoreSections() throws Exception {
        mockMvc.perform(get("/api/v1/observability/summary"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/observability/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelCallSummary.total").isNumber())
                .andExpect(jsonPath("$.ragSummary.total").isNumber())
                .andExpect(jsonPath("$.graphRagSummary.indexLogCount").isNumber())
                .andExpect(jsonPath("$.toolSummary.total").isNumber())
                .andExpect(jsonPath("$.workflowSummary.total").isNumber())
                .andExpect(jsonPath("$.sseSummary.activeConnections").isNumber())
                .andExpect(jsonPath("$.healthSummary.status").isString());
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
