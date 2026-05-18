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
import com.agentweave.conversation.application.ConversationAiResponse;
import com.agentweave.conversation.application.ModelCallLogService;
import com.agentweave.conversation.domain.ConversationEntity;
import com.agentweave.conversation.domain.ConversationMessageEntity;
import com.agentweave.conversation.domain.MessageRole;
import com.agentweave.conversation.domain.MessageStatus;
import com.agentweave.conversation.domain.ModelCallScenario;
import com.agentweave.conversation.domain.ModelCallStatus;
import com.agentweave.conversation.repository.ConversationRepository;
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
class ModelCallControllerIntegrationTest {

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
    private ConversationRepository conversationRepository;

    @Autowired
    private ModelCallLogService modelCallLogService;

    private String firstToken;
    private String adminToken;
    private String firstTraceId;
    private String secondTraceId;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String firstUsername = "model_call_a_" + suffix;
        String secondUsername = "model_call_b_" + suffix;
        String adminUsername = "model_call_admin_" + suffix;
        firstTraceId = "trace-model-first-" + suffix;
        secondTraceId = "trace-model-second-" + suffix;

        PermissionEntity observabilityRead =
                ensurePermission("observability:read", "Read observability", PermissionType.API);

        RoleEntity userRole = new RoleEntity(
                UUID.randomUUID(),
                "MODEL_CALL_USER_" + suffix.toUpperCase(),
                "Model Call User",
                null);
        roleRepository.save(userRole);

        RoleEntity adminRole = new RoleEntity(
                UUID.randomUUID(),
                "MODEL_CALL_ADMIN_" + suffix.toUpperCase(),
                "Model Call Admin",
                null);
        adminRole.replacePermissions(List.of(observabilityRead));
        roleRepository.save(adminRole);

        UserEntity firstUser = saveUser(firstUsername, List.of(userRole));
        UserEntity secondUser = saveUser(secondUsername, List.of(userRole));
        saveUser(adminUsername, List.of(adminRole));

        ConversationFixture firstConversation = saveConversationWithAssistantMessage(
                firstUser.getId(),
                "first model calls",
                firstTraceId);
        ConversationFixture secondConversation = saveConversationWithAssistantMessage(
                secondUser.getId(),
                "second model calls",
                secondTraceId);

        modelCallLogService.recordSuccess(
                firstConversation.conversationId(),
                firstConversation.messageId(),
                new ConversationAiResponse(
                        "answer token=plain-secret-value",
                        "openai",
                        "mimo-v2.5",
                        11,
                        7),
                "query token=plain-secret-value",
                "answer token=plain-secret-value",
                35,
                firstTraceId,
                ModelCallScenario.CHAT_SYNC);
        modelCallLogService.recordAgentCall(
                secondConversation.conversationId(),
                secondConversation.messageId(),
                "openai",
                "mimo-v2.5",
                null,
                null,
                5,
                3,
                21,
                ModelCallStatus.FAILED,
                "PLANNER_FAILED",
                "secret=plain-secret-value",
                secondTraceId,
                "PLANNER",
                null,
                null);

        firstToken = login(firstUsername, "password123");
        adminToken = login(adminUsername, "password123");
    }

    @Test
    void normalUserCanOnlyListOwnModelCalls() throws Exception {
        mockMvc.perform(get("/api/v1/observability/model-calls")
                        .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.traceId == '" + firstTraceId + "')].scenario")
                        .value(hasItem("CHAT_SYNC")))
                .andExpect(jsonPath("$.items[?(@.traceId == '" + secondTraceId + "')]").isEmpty());
    }

    @Test
    void observabilityReaderCanFilterAllModelCallsByTraceStatusAndScenario() throws Exception {
        mockMvc.perform(get("/api/v1/observability/model-calls")
                        .param("traceId", secondTraceId)
                        .param("status", "FAILED")
                        .param("scenario", "PLANNER")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].traceId").value(secondTraceId))
                .andExpect(jsonPath("$.items[0].modelName").value("mimo-v2.5"))
                .andExpect(jsonPath("$.items[0].scenario").value("PLANNER"))
                .andExpect(jsonPath("$.items[0].status").value("FAILED"))
                .andExpect(jsonPath("$.items[0].inputTokens").value(5))
                .andExpect(jsonPath("$.items[0].outputTokens").value(3))
                .andExpect(jsonPath("$.items[0].totalTokens").value(8))
                .andExpect(jsonPath("$.items[0].durationMs").value(21))
                .andExpect(jsonPath("$.items[0].errorMessage").value("secret=******"));
    }

    @Test
    void modelCallSummariesAreSanitized() throws Exception {
        mockMvc.perform(get("/api/v1/observability/model-calls")
                        .param("traceId", firstTraceId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].responseSummary").value("answer token=******"));
    }

    @Test
    void modelCallMetricsAreVisibleInActuator() throws Exception {
        mockMvc.perform(get("/actuator/metrics/agentweave.model.call.duration")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("agentweave.model.call.duration"));
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

    private ConversationFixture saveConversationWithAssistantMessage(UUID ownerUserId, String title, String traceId) {
        ConversationEntity conversation = new ConversationEntity(UUID.randomUUID(), ownerUserId, title);
        ConversationMessageEntity message = new ConversationMessageEntity(
                UUID.randomUUID(),
                ownerUserId,
                MessageRole.ASSISTANT,
                "answer",
                MessageStatus.SUCCEEDED,
                traceId);
        conversation.addMessage(message);
        ConversationEntity saved = conversationRepository.save(conversation);
        return new ConversationFixture(saved.getId(), message.getId());
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

    private record ConversationFixture(UUID conversationId, UUID messageId) {
    }
}
