package com.agentweave.conversation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agentweave.auth.domain.PermissionEntity;
import com.agentweave.auth.domain.PermissionType;
import com.agentweave.auth.domain.RoleEntity;
import com.agentweave.auth.domain.UserEntity;
import com.agentweave.auth.repository.PermissionRepository;
import com.agentweave.auth.repository.RoleRepository;
import com.agentweave.auth.repository.UserRepository;
import com.agentweave.conversation.application.ConversationAiClient;
import com.agentweave.conversation.domain.ConversationMessageEntity;
import com.agentweave.conversation.domain.MessageStatus;
import com.agentweave.conversation.domain.ModelCallLogEntity;
import com.agentweave.conversation.domain.ModelCallStatus;
import com.agentweave.conversation.repository.ConversationMessageRepository;
import com.agentweave.conversation.repository.ModelCallLogRepository;
import com.agentweave.graphrag.application.GraphRagRetrievalService;
import com.agentweave.graphrag.dto.GraphRagRetrievalResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "agentweave.chat.stream-timeout=100ms",
        "agentweave.chat.max-concurrent-streams-per-user=1"
})
class ConversationStreamTimeoutIntegrationTest {

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
    private ConversationMessageRepository conversationMessageRepository;

    @Autowired
    private ModelCallLogRepository modelCallLogRepository;

    @MockitoBean
    private ConversationAiClient conversationAiClient;

    @MockitoBean
    private VectorStore vectorStore;

    @MockitoBean
    private GraphRagRetrievalService graphRagRetrievalService;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        when(conversationAiClient.streamAnswer(any()))
                .thenReturn(Flux.never());
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of());
        when(graphRagRetrievalService.retrieve(any(), any()))
                .thenReturn(GraphRagRetrievalResponse.empty());

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        PermissionEntity ticketTool = ensurePermission("tool:ticket:query", "Query tickets", PermissionType.TOOL);
        PermissionEntity ragSearch = ensurePermission("knowledge:rag:search", "Search RAG knowledge base", PermissionType.API);
        RoleEntity role = new RoleEntity(UUID.randomUUID(), "TIMEOUT_CHAT_USER_" + suffix.toUpperCase(),
                "Timeout Chat User", null);
        role.replacePermissions(List.of(ticketTool, ragSearch));
        role = roleRepository.save(role);

        UserEntity user = new UserEntity(
                UUID.randomUUID(),
                "timeout_chat_user_" + suffix,
                "Timeout Chat User",
                passwordEncoder.encode("password123"),
                "timeout_chat_user_" + suffix + "@example.com");
        user.replaceRoles(List.of(role));
        userRepository.save(user);

        userToken = login(user.getUsername(), "password123");
    }

    @Test
    void streamTimeoutSendsErrorMarksAssistantFailedAndReleasesTask() throws Exception {
        String traceId = "trace-stream-timeout";
        UUID conversationId = createConversation("SSE timeout conversation");
        UUID assistantMessageId = sendStreamMessage(conversationId, "start a stream that times out");

        MvcResult timeoutResult = mockMvc.perform(get("/api/v1/conversations/{conversationId}/stream", conversationId)
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-Trace-Id", traceId)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        timeoutResult.getAsyncResult(3_000);

        mockMvc.perform(asyncDispatch(timeoutResult))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:workflow_step")))
                .andExpect(content().string(containsString("event:error")))
                .andExpect(content().string(containsString("\"code\":\"CHAT_STREAM_TIMEOUT\"")))
                .andExpect(content().string(containsString("\"message\":\"SSE stream timed out\"")))
                .andExpect(content().string(containsString("\"traceId\":\"" + traceId + "\"")));

        ConversationMessageEntity assistantMessage = conversationMessageRepository
                .findByIdAndConversation_Id(assistantMessageId, conversationId)
                .orElseThrow();
        assertThat(assistantMessage.getStatus()).isEqualTo(MessageStatus.FAILED);
        assertThat(assistantMessage.getErrorCode()).isEqualTo("CHAT_STREAM_TIMEOUT");
        assertThat(assistantMessage.getErrorMessage()).isEqualTo("SSE stream timed out");

        ModelCallLogEntity timeoutLog = modelCallLogRepository
                .findFirstByConversationIdOrderByCreatedAtDesc(conversationId)
                .orElseThrow();
        assertThat(timeoutLog.getStatus()).isEqualTo(ModelCallStatus.FAILED);
        assertThat(timeoutLog.getTraceId()).isEqualTo(traceId);

        when(conversationAiClient.streamAnswer(any()))
                .thenReturn(Flux.just("stream after timeout"));
        UUID secondConversationId = createConversation("SSE after timeout cleanup");
        sendStreamMessage(secondConversationId, "start after timeout");

        MvcResult secondStreamResult = mockMvc.perform(get("/api/v1/conversations/{conversationId}/stream",
                        secondConversationId)
                        .header("Authorization", "Bearer " + userToken)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        secondStreamResult.getAsyncResult(3_000);

        mockMvc.perform(asyncDispatch(secondStreamResult))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:workflow_step")))
                .andExpect(content().string(containsString("stream after timeout")));
    }

    private UUID createConversation(String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", title))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID sendStreamMessage(UUID conversationId, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", content,
                                "responseMode", "STREAM"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("assistantMessageId").asText());
    }

    private PermissionEntity ensurePermission(String code, String name, PermissionType type) {
        return permissionRepository.findByCode(code)
                .orElseGet(() -> permissionRepository.save(
                        new PermissionEntity(UUID.randomUUID(), code, name, type, null)));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private record LoginPayload(String username, String password) {
    }
}
