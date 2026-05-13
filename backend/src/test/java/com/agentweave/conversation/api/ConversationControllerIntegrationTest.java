package com.agentweave.conversation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import com.agentweave.conversation.application.ConversationAiResponse;
import com.agentweave.conversation.application.ConversationPrompt;
import com.agentweave.conversation.domain.ModelCallLogEntity;
import com.agentweave.conversation.domain.ModelCallStatus;
import com.agentweave.conversation.domain.ConversationMessageEntity;
import com.agentweave.conversation.domain.MessageStatus;
import com.agentweave.conversation.application.ConversationStreamService;
import com.agentweave.conversation.repository.ConversationMessageRepository;
import com.agentweave.conversation.repository.ModelCallLogRepository;
import com.agentweave.graphrag.application.GraphRagRetrievalService;
import com.agentweave.graphrag.dto.GraphPathResponse;
import com.agentweave.graphrag.dto.GraphRagRetrievalResponse;
import com.agentweave.shared.security.AuthenticatedUser;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.tracing.CorrelationContext;
import com.agentweave.shared.tracing.TraceIdProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class ConversationControllerIntegrationTest {

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
    private ModelCallLogRepository modelCallLogRepository;

    @Autowired
    private ConversationMessageRepository conversationMessageRepository;

    @Autowired
    private ConversationStreamService conversationStreamService;

    @Value("${agentweave.chat.max-concurrent-streams-per-user}")
    private int maxConcurrentStreamsPerUser;

    @MockitoBean
    private ConversationAiClient conversationAiClient;

    @MockitoBean
    private VectorStore vectorStore;

    @MockitoBean
    private GraphRagRetrievalService graphRagRetrievalService;

    private String userToken;
    private String otherUserToken;
    private UUID userId;
    private String username;

    @BeforeEach
    void setUp() throws Exception {
        when(conversationAiClient.streamAnswer(any()))
                .thenReturn(Flux.just("MiMo-V2.5 test answer."));
        when(conversationAiClient.answer(any()))
                .thenReturn(new ConversationAiResponse(
                        "MiMo-V2.5 sync answer.",
                        "openai",
                        "mimo-v2.5",
                        12,
                        8));
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of());
        when(graphRagRetrievalService.retrieve(any(), any()))
                .thenReturn(GraphRagRetrievalResponse.empty());
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        PermissionEntity ticketTool = ensurePermission("tool:ticket:query", "Query tickets", PermissionType.TOOL);
        PermissionEntity ragSearch = ensurePermission("knowledge:rag:search", "Search RAG knowledge base", PermissionType.API);

        RoleEntity role = new RoleEntity(UUID.randomUUID(), "CHAT_USER_" + suffix.toUpperCase(), "Chat User", null);
        role.replacePermissions(List.of(ticketTool, ragSearch));
        role = roleRepository.save(role);

        UserEntity user = new UserEntity(
                UUID.randomUUID(),
                "chat_user_" + suffix,
                "Chat User",
                passwordEncoder.encode("password123"),
                "chat_user_" + suffix + "@example.com");
        user.replaceRoles(List.of(role));
        userRepository.save(user);
        userId = user.getId();
        username = user.getUsername();

        UserEntity otherUser = new UserEntity(
                UUID.randomUUID(),
                "chat_other_" + suffix,
                "Other User",
                passwordEncoder.encode("password123"),
                "chat_other_" + suffix + "@example.com");
        otherUser.replaceRoles(List.of(role));
        userRepository.save(otherUser);

        userToken = login(user.getUsername(), "password123");
        otherUserToken = login(otherUser.getUsername(), "password123");
    }

    @Test
    void createConversationReturnsTraceIdAndCreationTime() throws Exception {
        String traceId = "trace-create-001";

        mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-Trace-Id", traceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "排障对话"))))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", traceId))
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.title").value("排障对话"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.messageCount").value(0))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.traceId").value(traceId));
    }

    @Test
    void createConversationAcceptsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("新的对话"));
    }

    @Test
    void createConversationRejectsTooLongTitle() throws Exception {
        String traceId = "trace-create-002";
        String longTitle = "a".repeat(161);

        mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-Trace-Id", traceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", longTitle))))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Trace-Id", traceId))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.path").value("/api/v1/conversations"))
                .andExpect(jsonPath("$.traceId").value(traceId))
                .andExpect(jsonPath("$.message", containsString("title")));
    }

    @Test
    void createConversationRequiresAuthentication() throws Exception {
        String traceId = "trace-create-003";

        mockMvc.perform(post("/api/v1/conversations")
                        .header("X-Trace-Id", traceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "排障对话"))))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Trace-Id", traceId))
                .andExpect(jsonPath("$.code").value("AUTH_401"))
                .andExpect(jsonPath("$.path").value("/api/v1/conversations"))
                .andExpect(jsonPath("$.traceId").value(traceId));
    }

    @Test
    void userCanCreateConversationAndSendMessage() throws Exception {
        UUID conversationId = createConversation(userToken, "排障对话");

        mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "帮我分析接口超时"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(conversationId.toString()))
                .andExpect(jsonPath("$.userMessageId").isString())
                .andExpect(jsonPath("$.assistantMessageId").isString())
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        mockMvc.perform(get("/api/v1/conversations/{conversationId}", conversationId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("排障对话"))
                .andExpect(jsonPath("$.messagePage").value(0))
                .andExpect(jsonPath("$.messageSize").value(20))
                .andExpect(jsonPath("$.messageCount").value(2))
                .andExpect(jsonPath("$.messageTotal").value(2))
                .andExpect(jsonPath("$.messageTotalPages").value(1))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.messages[0].content").value("帮我分析接口超时"))
                .andExpect(jsonPath("$.messages[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.messages[0].citations").isArray())
                .andExpect(jsonPath("$.messages[0].toolCalls").isArray())
                .andExpect(jsonPath("$.messages[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.messages[1].content").value(""))
                .andExpect(jsonPath("$.messages[1].status").value("PENDING"));
    }

    @Test
    void syncMessageReturnsAnswerAndPersistsAssistantMessageAndModelCallLog() throws Exception {
        UUID conversationId = createConversation(userToken, "同步问答会话");
        String traceId = "trace-sync-success";
        String documentId = UUID.randomUUID().toString();
        String chunkId = UUID.randomUUID().toString();
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(Document.builder()
                        .id(chunkId)
                        .text("订单接口慢需要先检查 payment-api、连接池和 retry 配置。")
                        .metadata(Map.of(
                                "documentId", documentId,
                                "documentName", "订单排障手册",
                                "chunkId", chunkId,
                                "source", "runbook",
                                "businessDomain", "order",
                                "documentType", "RUNBOOK",
                                "permissionLevel", "INTERNAL"))
                        .score(0.91)
                        .build()));
        AtomicReference<String> modelTraceId = new AtomicReference<>();
        AtomicReference<String> modelConversationId = new AtomicReference<>();
        AtomicReference<String> modelMessageId = new AtomicReference<>();
        AtomicReference<ConversationPrompt> modelPrompt = new AtomicReference<>();
        when(conversationAiClient.answer(any()))
                .thenAnswer(invocation -> {
                    modelTraceId.set(MDC.get(TraceIdProvider.TRACE_ID_KEY));
                    modelConversationId.set(MDC.get(CorrelationContext.CONVERSATION_ID_KEY));
                    modelMessageId.set(MDC.get(CorrelationContext.MESSAGE_ID_KEY));
                    modelPrompt.set(invocation.getArgument(0));
                    return new ConversationAiResponse(
                            "MiMo-V2.5 sync answer.",
                            "openai",
                            "mimo-v2.5",
                            12,
                            8);
                });

        MvcResult result = mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-Trace-Id", traceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "同步分析订单接口慢",
                                "responseMode", "SYNC"))))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", traceId))
                .andExpect(jsonPath("$.conversationId").value(conversationId.toString()))
                .andExpect(jsonPath("$.userMessageId").isString())
                .andExpect(jsonPath("$.assistantMessageId").isString())
                .andExpect(jsonPath("$.answer").value("MiMo-V2.5 sync answer."))
                .andExpect(jsonPath("$.retrievalMode").value("VECTOR_ONLY"))
                .andExpect(jsonPath("$.citations[0].documentId").value(documentId))
                .andExpect(jsonPath("$.citations[0].documentName").value("订单排障手册"))
                .andExpect(jsonPath("$.citations[0].chunkId").value(chunkId))
                .andExpect(jsonPath("$.citations[0].source").value("runbook"))
                .andExpect(jsonPath("$.citations[0].snippet").value("订单接口慢需要先检查 payment-api、连接池和 retry 配置。"))
                .andExpect(jsonPath("$.graphPaths").isArray())
                .andExpect(jsonPath("$.traceId").value(traceId))
                .andReturn();

        UUID assistantMessageId = UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("assistantMessageId")
                .asText());
        assertThat(modelTraceId.get()).isEqualTo(traceId);
        assertThat(modelConversationId.get()).isEqualTo(conversationId.toString());
        assertThat(modelMessageId.get()).isEqualTo(assistantMessageId.toString());
        assertThat(modelPrompt.get().ragContext().promptContext())
                .contains(documentId)
                .contains(chunkId)
                .contains("订单接口慢需要先检查 payment-api、连接池和 retry 配置。");

        mockMvc.perform(get("/api/v1/conversations/{conversationId}", conversationId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageCount").value(2))
                .andExpect(jsonPath("$.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.messages[0].content").value("同步分析订单接口慢"))
                .andExpect(jsonPath("$.messages[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.messages[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.messages[1].id").value(assistantMessageId.toString()))
                .andExpect(jsonPath("$.messages[1].content").value("MiMo-V2.5 sync answer."))
                .andExpect(jsonPath("$.messages[1].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.messages[1].citations[0].documentId").value(documentId))
                .andExpect(jsonPath("$.messages[1].citations[0].chunkId").value(chunkId))
                .andExpect(jsonPath("$.messages[1].metadata", containsString("\"retrievalMode\":\"VECTOR_ONLY\"")))
                .andExpect(jsonPath("$.messages[1].metadata", containsString(documentId)));

        ModelCallLogEntity log = modelCallLogRepository
                .findFirstByConversationIdOrderByCreatedAtDesc(conversationId)
                .orElseThrow();
        assertThat(log.getMessageId()).isEqualTo(assistantMessageId);
        assertThat(log.getProvider()).isEqualTo("openai");
        assertThat(log.getModel()).isEqualTo("mimo-v2.5");
        assertThat(log.getPromptTokens()).isEqualTo(12);
        assertThat(log.getCompletionTokens()).isEqualTo(8);
        assertThat(log.getStatus()).isEqualTo(ModelCallStatus.SUCCEEDED);
        assertThat(log.getTraceId()).isEqualTo(traceId);

        ConversationMessageEntity assistantMessage = conversationMessageRepository
                .findByIdAndConversation_Id(assistantMessageId, conversationId)
                .orElseThrow();
        assertThat(assistantMessage.getUserId()).isNotNull();
        assertThat(assistantMessage.getStatus()).isEqualTo(MessageStatus.SUCCEEDED);
        assertThat(assistantMessage.getErrorCode()).isNull();
        assertThat(assistantMessage.getErrorMessage()).isNull();
        assertThat(assistantMessage.getMetadata()).contains(documentId);
        assertThat(assistantMessage.getMetadata()).contains(chunkId);
    }

    @Test
    void xRequestIdIsUsedAsTraceIdWhenTraceIdHeaderIsAbsent() throws Exception {
        String requestId = "request-correlation-001";

        mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "request id conversation"))))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", requestId))
                .andExpect(jsonPath("$.traceId").value(requestId));
    }

    @Test
    void serverGeneratesTraceIdWhenRequestHeadersAreAbsent() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "generated trace conversation"))))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andReturn();

        String headerTraceId = result.getResponse().getHeader("X-Trace-Id");
        String bodyTraceId = objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("traceId")
                .asText();
        assertThat(bodyTraceId).isEqualTo(headerTraceId);
    }

    @Test
    void syncMessageMarksAssistantFailedAndLogsModelFailureWhenAiClientFails() throws Exception {
        UUID conversationId = createConversation(userToken, "同步失败会话");
        String traceId = "trace-sync-failure";
        when(conversationAiClient.answer(any()))
                .thenThrow(new IllegalStateException("provider failed token=plain-secret-value"));

        MvcResult result = mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-Trace-Id", traceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "同步失败测试",
                                "responseMode", "SYNC"))))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("X-Trace-Id", traceId))
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.traceId").value(traceId))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("Internal server error");

        mockMvc.perform(get("/api/v1/conversations/{conversationId}", conversationId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageCount").value(2))
                .andExpect(jsonPath("$.messages[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.messages[1].content").value("AI answer generation failed"))
                .andExpect(jsonPath("$.messages[1].status").value("FAILED"));

        ModelCallLogEntity log = modelCallLogRepository
                .findFirstByConversationIdOrderByCreatedAtDesc(conversationId)
                .orElseThrow();
        assertThat(log.getStatus()).isEqualTo(ModelCallStatus.FAILED);
        assertThat(log.getErrorCode()).isEqualTo("MODEL_CALL_FAILED");
        assertThat(log.getTraceId()).isEqualTo(traceId);
        assertThat(log.getErrorMessage()).contains("token=******");
        assertThat(log.getErrorMessage()).doesNotContain("plain-secret-value");
    }

    @Test
    void sendMessageRejectsBlankAndTooLongContent() throws Exception {
        UUID conversationId = createConversation(userToken, "校验会话");

        mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));

        mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "a".repeat(8001)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    @Test
    void sendMessageRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "帮我分析接口超时"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void sendMessageReturnsNotFoundForMissingOrOtherUsersConversation() throws Exception {
        UUID conversationId = createConversation(userToken, "发送权限会话");

        mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", UUID.randomUUID())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "帮我分析接口超时"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_404"));

        mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "帮我分析接口超时"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_404"));
    }

    @Test
    void getConversationSupportsMessagePaginationAndAscendingOrder() throws Exception {
        UUID conversationId = createConversation(userToken, "分页会话");
        sendMessage(userToken, conversationId, "第一条消息");
        sendMessage(userToken, conversationId, "第二条消息");
        sendMessage(userToken, conversationId, "第三条消息");

        mockMvc.perform(get("/api/v1/conversations/{conversationId}", conversationId)
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messagePage").value(1))
                .andExpect(jsonPath("$.messageSize").value(2))
                .andExpect(jsonPath("$.messageTotal").value(6))
                .andExpect(jsonPath("$.messageTotalPages").value(3))
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].content").value("第二条消息"))
                .andExpect(jsonPath("$.messages[1].status").value("PENDING"));
    }

    @Test
    void getConversationReturnsEmptyMessagePageForEmptyConversation() throws Exception {
        UUID conversationId = createConversation(userToken, "空会话");

        mockMvc.perform(get("/api/v1/conversations/{conversationId}", conversationId)
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageCount").value(0))
                .andExpect(jsonPath("$.messagePage").value(0))
                .andExpect(jsonPath("$.messageSize").value(10))
                .andExpect(jsonPath("$.messageTotal").value(0))
                .andExpect(jsonPath("$.messageTotalPages").value(0))
                .andExpect(jsonPath("$.messages").isEmpty());
    }

    @Test
    void getConversationReturnsNotFoundWhenMissing() throws Exception {
        mockMvc.perform(get("/api/v1/conversations/{conversationId}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_404"));
    }

    @Test
    void getConversationRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/conversations/{conversationId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void userCanOnlyReadOwnConversations() throws Exception {
        UUID conversationId = createConversation(userToken, "私有会话");

        mockMvc.perform(get("/api/v1/conversations/{conversationId}", conversationId)
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_404"));
    }

    @Test
    void listReturnsCurrentUsersConversations() throws Exception {
        createConversation(userToken, "当前用户会话");
        createConversation(otherUserToken, "其他用户会话");

        mockMvc.perform(get("/api/v1/conversations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("当前用户会话"))
                .andExpect(jsonPath("$.items[0].messageCount").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listSupportsKeywordPaginationAndUpdatedAtDescendingOrder() throws Exception {
        UUID firstConversationId = createConversation(userToken, "订单接口超时");
        createConversation(userToken, "库存同步异常");
        UUID latestConversationId = createConversation(userToken, "订单状态查询");

        sendMessage(userToken, firstConversationId, "第一条订单排查消息");
        sendMessage(userToken, latestConversationId, "最近一条订单排查消息");

        mockMvc.perform(get("/api/v1/conversations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "1")
                        .param("keyword", "订单"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(latestConversationId.toString()))
                .andExpect(jsonPath("$.items[0].title").value("订单状态查询"))
                .andExpect(jsonPath("$.items[0].messageCount").value(2))
                .andExpect(jsonPath("$.items[0].lastMessagePreview").value("最近一条订单排查消息"))
                .andExpect(jsonPath("$.items[0].lastMessageAt").isNotEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/v1/conversations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "1")
                        .param("size", "1")
                        .param("keyword", "订单"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(firstConversationId.toString()))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/conversations")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void streamReturnsSseEventsAndPersistsAssistantMessage() throws Exception {
        String traceId = "trace-stream-success";
        String documentId = UUID.randomUUID().toString();
        String chunkId = UUID.randomUUID().toString();
        GraphPathResponse graphPath = new GraphPathResponse(
                "path-api-timeout",
                2,
                List.of("api-gateway", "order-service", "database"),
                List.of("CALLS", "DEPENDS_ON"),
                List.of(chunkId),
                0.78);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(Document.builder()
                        .id(chunkId)
                        .text("API timeout: inspect upstream latency and database slow queries first.")
                        .metadata(Map.of(
                                "documentId", documentId,
                                "documentName", "api-status-runbook",
                                "chunkId", chunkId,
                                "source", "runbook",
                                "businessDomain", "ops",
                                "documentType", "RUNBOOK",
                                "permissionLevel", "INTERNAL"))
                        .score(0.88)
                        .build()));
        when(graphRagRetrievalService.retrieve(any(), any()))
                .thenReturn(new GraphRagRetrievalResponse(
                        List.of(graphPath),
                        List.of("api-gateway", "order-service", "database"),
                        List.of(chunkId),
                        "count=1,max=0.78",
                        1,
                        0));
        AtomicReference<String> modelTraceId = new AtomicReference<>();
        AtomicReference<String> modelConversationId = new AtomicReference<>();
        AtomicReference<String> modelMessageId = new AtomicReference<>();
        AtomicReference<ConversationPrompt> modelPrompt = new AtomicReference<>();
        when(conversationAiClient.streamAnswer(any()))
                .thenAnswer(invocation -> Flux.defer(() -> {
                    modelTraceId.set(MDC.get(TraceIdProvider.TRACE_ID_KEY));
                    modelConversationId.set(MDC.get(CorrelationContext.CONVERSATION_ID_KEY));
                    modelMessageId.set(MDC.get(CorrelationContext.MESSAGE_ID_KEY));
                    modelPrompt.set(invocation.getArgument(0));
                    return Flux.just("MiMo-V2.5 test answer.");
                }));
        UUID conversationId = createConversation(userToken, "SSE session");
        sendMessage(userToken, conversationId, "Analyze API timeout");

        MvcResult streamResult = mockMvc.perform(get("/api/v1/conversations/{conversationId}/stream", conversationId)
                        .header("Authorization", "Bearer " + userToken)
                        .header("X-Trace-Id", traceId)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", traceId))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(streamResult))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:workflow_step")))
                .andExpect(content().string(containsString("\"eventId\"")))
                .andExpect(content().string(containsString("\"conversationId\":\"" + conversationId + "\"")))
                .andExpect(content().string(containsString("\"messageId\"")))
                .andExpect(content().string(containsString("\"traceId\":\"" + traceId + "\"")))
                .andExpect(content().string(containsString("\"timestamp\"")))
                .andExpect(content().string(containsString("\"createdAt\"")))
                .andExpect(content().string(containsString("event:citation")))
                .andExpect(content().string(containsString("\"documentId\":\"" + documentId + "\"")))
                .andExpect(content().string(containsString("\"documentName\":\"api-status-runbook\"")))
                .andExpect(content().string(containsString("\"chunkId\":\"" + chunkId + "\"")))
                .andExpect(content().string(containsString("\"source\":\"runbook\"")))
                .andExpect(content().string(containsString("event:graph_path")))
                .andExpect(content().string(containsString("\"graphPath\"")))
                .andExpect(content().string(containsString("\"pathId\":\"path-api-timeout\"")))
                .andExpect(content().string(containsString("\"depth\":2")))
                .andExpect(content().string(containsString("\"entities\":[\"api-gateway\",\"order-service\",\"database\"]")))
                .andExpect(content().string(containsString("\"relationships\":[\"CALLS\",\"DEPENDS_ON\"]")))
                .andExpect(content().string(containsString("\"sourceChunkIds\":[\"" + chunkId + "\"]")))
                .andExpect(content().string(containsString("\"confidence\":0.78")))
                .andExpect(content().string(containsString("event:message_delta")))
                .andExpect(content().string(containsString("\"delta\":\"MiMo-V2.5 test answer.\"")))
                .andExpect(content().string(containsString("MiMo-V2.5 test answer.")))
                .andExpect(content().string(containsString("event:done")))
                .andExpect(content().string(containsString("\"status\":\"SUCCEEDED\"")));

        mockMvc.perform(get("/api/v1/conversations/{conversationId}", conversationId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageCount").value(2))
                .andExpect(jsonPath("$.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.messages[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.messages[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.messages[1].content").value("MiMo-V2.5 test answer."))
                .andExpect(jsonPath("$.messages[1].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.messages[1].citations[0].documentId").value(documentId))
                .andExpect(jsonPath("$.messages[1].citations[0].chunkId").value(chunkId))
                .andExpect(jsonPath("$.messages[1].graphPaths[0].pathId").value("path-api-timeout"))
                .andExpect(jsonPath("$.messages[1].graphPaths[0].sourceChunkIds[0]").value(chunkId))
                .andExpect(jsonPath("$.messages[1].metadata", containsString(documentId)));

        ModelCallLogEntity log = modelCallLogRepository
                .findFirstByConversationIdOrderByCreatedAtDesc(conversationId)
                .orElseThrow();
        assertThat(log.getMessageId()).isNotNull();
        assertThat(log.getStatus()).isEqualTo(ModelCallStatus.SUCCEEDED);
        assertThat(log.getProvider()).isEqualTo("openai");
        assertThat(log.getModel()).isEqualTo("unknown");
        assertThat(log.getTraceId()).isEqualTo(traceId);
        assertThat(modelTraceId.get()).isEqualTo(traceId);
        assertThat(modelConversationId.get()).isEqualTo(conversationId.toString());
        assertThat(modelMessageId.get()).isNotBlank();
        assertThat(modelPrompt.get().ragContext().promptContext())
                .contains(documentId)
                .contains(chunkId)
                .contains("path-api-timeout")
                .contains("api-gateway -> order-service -> database");
    }

    @Test
    void cancellingStreamMarksAssistantMessageCancelled() throws Exception {
        Sinks.Many<String> streamSink = Sinks.many().multicast().onBackpressureBuffer();
        when(conversationAiClient.streamAnswer(any()))
                .thenReturn(streamSink.asFlux());
        UUID conversationId = createConversation(userToken, "SSE 取消会话");
        sendMessage(userToken, conversationId, "帮我分析接口超时");

        authenticateTestUser();
        Disposable subscription = conversationStreamService.stream(conversationId)
                .subscribe();
        subscription.dispose();
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/conversations/{conversationId}", conversationId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.messages[1].status").value("CANCELLED"))
                .andExpect(jsonPath("$.messages[1].errorCode").value("CHAT_ASSISTANT_CANCELLED"));
    }

    @Test
    void cancelEndpointCancelsActiveStreamAndReturnsMessageStatus() throws Exception {
        Sinks.Many<String> streamSink = Sinks.many().multicast().onBackpressureBuffer();
        when(conversationAiClient.streamAnswer(any()))
                .thenReturn(streamSink.asFlux());
        UUID conversationId = createConversation(userToken, "SSE ä¸»åŠ¨å–æ¶ˆä¼šè¯");
        MvcResult sendResult = mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "å¼€å§‹ä¸€æ¬¡å¯å–æ¶ˆç”Ÿæˆ",
                                "responseMode", "STREAM"))))
                .andExpect(status().isOk())
                .andReturn();
        UUID assistantMessageId = UUID.fromString(objectMapper
                .readTree(sendResult.getResponse().getContentAsString())
                .get("assistantMessageId")
                .asText());

        MvcResult streamResult = mockMvc.perform(get("/api/v1/conversations/{conversationId}/stream", conversationId)
                        .header("Authorization", "Bearer " + userToken)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages/{messageId}/cancel",
                        conversationId,
                        assistantMessageId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(conversationId.toString()))
                .andExpect(jsonPath("$.messageId").value(assistantMessageId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        mockMvc.perform(asyncDispatch(streamResult))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:workflow_step")));

        mockMvc.perform(get("/api/v1/conversations/{conversationId}", conversationId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[1].status").value("CANCELLED"))
                .andExpect(jsonPath("$.messages[1].errorCode").value("CHAT_ASSISTANT_CANCELLED"));
    }

    @Test
    void cancelEndpointDisposesUpstreamModelStream() throws Exception {
        CountDownLatch subscribed = new CountDownLatch(1);
        CountDownLatch cancelled = new CountDownLatch(1);
        AtomicBoolean cancelledBeforeExtraToken = new AtomicBoolean(false);
        Sinks.Many<String> modelStream = Sinks.many().multicast().onBackpressureBuffer();
        when(conversationAiClient.streamAnswer(any()))
                .thenAnswer(invocation -> modelStream.asFlux()
                        .doOnSubscribe(ignored -> subscribed.countDown())
                        .doOnCancel(() -> {
                            cancelledBeforeExtraToken.set(modelStream.currentSubscriberCount() > 0);
                            cancelled.countDown();
                        }));
        UUID conversationId = createConversation(userToken, "SSE upstream cancellation");
        MvcResult sendResult = mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "start a long model stream",
                                "responseMode", "STREAM"))))
                .andExpect(status().isOk())
                .andReturn();
        UUID assistantMessageId = UUID.fromString(objectMapper
                .readTree(sendResult.getResponse().getContentAsString())
                .get("assistantMessageId")
                .asText());

        MvcResult streamResult = mockMvc.perform(get("/api/v1/conversations/{conversationId}/stream", conversationId)
                        .header("Authorization", "Bearer " + userToken)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        assertThat(subscribed.await(2, TimeUnit.SECONDS)).isTrue();

        mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages/{messageId}/cancel",
                        conversationId,
                        assistantMessageId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(cancelled.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(modelStream.currentSubscriberCount()).isZero();
        assertThat(cancelledBeforeExtraToken).isTrue();
        modelStream.tryEmitNext("token emitted after cancellation");

        mockMvc.perform(asyncDispatch(streamResult))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:workflow_step")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("token emitted after cancellation"))));

        mockMvc.perform(get("/api/v1/conversations/{conversationId}", conversationId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[1].content").value(""))
                .andExpect(jsonPath("$.messages[1].status").value("CANCELLED"))
                .andExpect(jsonPath("$.messages[1].errorCode").value("CHAT_ASSISTANT_CANCELLED"));
    }

    @Test
    void streamRejectsWhenUserExceedsConcurrentLimit() throws Exception {
        assertThat(maxConcurrentStreamsPerUser).isEqualTo(1);
        Sinks.Many<String> streamSink = Sinks.many().multicast().onBackpressureBuffer();
        when(conversationAiClient.streamAnswer(any()))
                .thenReturn(streamSink.asFlux());
        UUID firstConversationId = createConversation(userToken, "SSE å¹¶å‘ä¸€");
        UUID secondConversationId = createConversation(userToken, "SSE å¹¶å‘äºŒ");
        sendMessage(userToken, firstConversationId, "ç¬¬ä¸€æ¡æµ");
        sendMessage(userToken, secondConversationId, "ç¬¬äºŒæ¡æµ");

        authenticateTestUser();
        Disposable firstSubscription = conversationStreamService.stream(firstConversationId)
                .subscribe();

        try {
            mockMvc.perform(get("/api/v1/conversations/{conversationId}/stream", secondConversationId)
                            .header("Authorization", "Bearer " + userToken)
                            .accept(MediaType.TEXT_EVENT_STREAM))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code").value("COMMON_429"));
        } finally {
            firstSubscription.dispose();
            SecurityContextHolder.clearContext();
        }
    }

    private UUID createConversation(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", title))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private void sendMessage(String token, UUID conversationId, String content) throws Exception {
        mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", content,
                                "responseMode", "STREAM"))))
                .andExpect(status().isOk());
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

    private void authenticateTestUser() {
        CurrentUser currentUser = new CurrentUser(
                userId,
                username,
                "Chat User",
                Set.of("CHAT_USER"),
                Set.of("tool:ticket:query", "knowledge:rag:search"));
        AuthenticatedUser principal = new AuthenticatedUser(
                currentUser,
                "password",
                List.of(),
                true,
                0);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private record LoginPayload(String username, String password) {
    }
}
