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
import com.agentweave.conversation.domain.ConversationEntity;
import com.agentweave.conversation.domain.ConversationMessageEntity;
import com.agentweave.conversation.domain.MessageRole;
import com.agentweave.conversation.domain.MessageStatus;
import com.agentweave.conversation.repository.ConversationRepository;
import com.agentweave.graphrag.domain.GraphRagIndexLog;
import com.agentweave.graphrag.domain.GraphRagRetrievalLog;
import com.agentweave.graphrag.repository.GraphRagIndexLogRepository;
import com.agentweave.graphrag.repository.GraphRagRetrievalLogRepository;
import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.repository.DocumentRepository;
import com.agentweave.observability.application.AgentWeaveMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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
class GraphRagObservabilityControllerIntegrationTest {

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
    private DocumentRepository documentRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private GraphRagIndexLogRepository graphRagIndexLogRepository;

    @Autowired
    private GraphRagRetrievalLogRepository graphRagRetrievalLogRepository;

    @Autowired
    private AgentWeaveMetrics agentWeaveMetrics;

    private String firstToken;
    private String adminToken;
    private String firstIndexTraceId;
    private String secondIndexTraceId;
    private String firstRetrievalTraceId;
    private String secondRetrievalTraceId;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        firstIndexTraceId = "trace-graphrag-index-first-" + suffix;
        secondIndexTraceId = "trace-graphrag-index-second-" + suffix;
        firstRetrievalTraceId = "trace-graphrag-retrieval-first-" + suffix;
        secondRetrievalTraceId = "trace-graphrag-retrieval-second-" + suffix;

        PermissionEntity observabilityRead =
                ensurePermission("observability:read", "Read observability", PermissionType.API);

        RoleEntity userRole = new RoleEntity(
                UUID.randomUUID(),
                "GRAPHRAG_OBS_USER_" + suffix.toUpperCase(),
                "GraphRAG Observer User",
                null);
        roleRepository.save(userRole);

        RoleEntity adminRole = new RoleEntity(
                UUID.randomUUID(),
                "GRAPHRAG_OBS_ADMIN_" + suffix.toUpperCase(),
                "GraphRAG Observer Admin",
                null);
        adminRole.replacePermissions(List.of(observabilityRead));
        roleRepository.save(adminRole);

        UserEntity firstUser = saveUser("graphrag_obs_a_" + suffix, List.of(userRole));
        UserEntity secondUser = saveUser("graphrag_obs_b_" + suffix, List.of(userRole));
        UserEntity admin = saveUser("graphrag_obs_admin_" + suffix, List.of(adminRole));

        DocumentEntity firstDocument = saveDocument(firstUser.getId(), "first-" + suffix + ".txt");
        DocumentEntity secondDocument = saveDocument(secondUser.getId(), "second-" + suffix + ".txt");
        ConversationFixture firstConversation = saveConversation(firstUser.getId(), "first graph retrieval", firstRetrievalTraceId);
        ConversationFixture secondConversation = saveConversation(secondUser.getId(), "second graph retrieval", secondRetrievalTraceId);

        saveIndexLog(firstDocument.getId(), firstIndexTraceId, 2, 1, 3);
        saveIndexLog(secondDocument.getId(), secondIndexTraceId, 4, 2, 6);
        saveRetrievalLog(
                firstConversation.conversationId(),
                firstConversation.messageId(),
                firstDocument.getId(),
                firstRetrievalTraceId,
                "HYBRID",
                "order",
                3,
                2);
        saveRetrievalLog(
                secondConversation.conversationId(),
                secondConversation.messageId(),
                secondDocument.getId(),
                secondRetrievalTraceId,
                "GRAPH_ONLY",
                "payment",
                2,
                1);

        firstToken = login(firstUser.getUsername(), "password123");
        adminToken = login(admin.getUsername(), "password123");
    }

    @Test
    void normalUserCanOnlyListOwnGraphRagLogs() throws Exception {
        mockMvc.perform(get("/api/v1/observability/graphrag/index-logs")
                        .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.traceId == '" + firstIndexTraceId + "')].entityCount")
                        .value(hasItem(2)))
                .andExpect(jsonPath("$.items[?(@.traceId == '" + secondIndexTraceId + "')]").isEmpty());

        mockMvc.perform(get("/api/v1/observability/graphrag/retrieval-logs")
                        .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.traceId == '" + firstRetrievalTraceId + "')].filteredPathCount")
                        .value(hasItem(2)))
                .andExpect(jsonPath("$.items[?(@.traceId == '" + secondRetrievalTraceId + "')]").isEmpty());
    }

    @Test
    void observabilityReaderCanFilterGraphRagLogs() throws Exception {
        mockMvc.perform(get("/api/v1/observability/graphrag/index-logs")
                        .param("traceId", secondIndexTraceId)
                        .param("status", "INDEXED")
                        .param("neo4jEnabled", "false")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].traceId").value(secondIndexTraceId))
                .andExpect(jsonPath("$.items[0].chunkEntityCount").value(6))
                .andExpect(jsonPath("$.items[0].durationMs").isNumber());

        mockMvc.perform(get("/api/v1/observability/graphrag/retrieval-logs")
                        .param("traceId", firstRetrievalTraceId)
                        .param("retrievalMode", "HYBRID")
                        .param("businessDomain", "order")
                        .param("status", "SUCCESS")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].traceId").value(firstRetrievalTraceId))
                .andExpect(jsonPath("$.items[0].sourceChunkIds[0]").value("chunk-1"))
                .andExpect(jsonPath("$.items[0].durationMs").isNumber());
    }

    @Test
    void graphRagSummaryReturnsLatestReadableLogCounts() throws Exception {
        mockMvc.perform(get("/api/v1/observability/graphrag")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexLogCount").isNumber())
                .andExpect(jsonPath("$.retrievalLogCount").isNumber())
                .andExpect(jsonPath("$.latestIndexLog.traceId").isString())
                .andExpect(jsonPath("$.latestRetrievalLog.traceId").isString());
    }

    @Test
    void graphRagMetricsAreVisibleInActuator() throws Exception {
        agentWeaveMetrics.recordGraphRagIndex("order", "INTERNAL", "INDEXED", false, 15);
        agentWeaveMetrics.recordGraphRagPathSearch("HYBRID", "order", "INTERNAL", "SUCCESS", 9, 2);

        mockMvc.perform(get("/actuator/metrics/agentweave.graphrag.index.duration")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("agentweave.graphrag.index.duration"));

        mockMvc.perform(get("/actuator/metrics/agentweave.graphrag.path.search.duration")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("agentweave.graphrag.path.search.duration"));
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

    private DocumentEntity saveDocument(UUID uploadedBy, String filename) {
        DocumentEntity document = new DocumentEntity(
                UUID.randomUUID(),
                filename,
                "text/plain",
                128,
                "agentweave-documents",
                "observability/" + filename,
                UUID.randomUUID().toString(),
                uploadedBy,
                "runbook",
                "order",
                "RUNBOOK",
                "INTERNAL",
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                "graph");
        document.markIndexed("trace-doc-" + filename);
        return documentRepository.save(document);
    }

    private ConversationFixture saveConversation(UUID ownerUserId, String title, String traceId) {
        ConversationEntity conversation = new ConversationEntity(UUID.randomUUID(), ownerUserId, title);
        ConversationMessageEntity message = new ConversationMessageEntity(
                UUID.randomUUID(),
                ownerUserId,
                MessageRole.USER,
                "why is order timeout",
                MessageStatus.SUCCEEDED,
                traceId);
        conversation.addMessage(message);
        ConversationEntity saved = conversationRepository.save(conversation);
        return new ConversationFixture(saved.getId(), message.getId());
    }

    private void saveIndexLog(UUID documentId, String traceId, int entityCount, int relationshipCount, int chunkEntityCount) {
        GraphRagIndexLog log = new GraphRagIndexLog(UUID.randomUUID(), documentId, traceId, 2, false);
        log.markCompleted(entityCount, relationshipCount, 2, chunkEntityCount);
        graphRagIndexLogRepository.saveAndFlush(log);
    }

    private void saveRetrievalLog(
            UUID conversationId,
            UUID messageId,
            UUID documentId,
            String traceId,
            String retrievalMode,
            String businessDomain,
            int matchedPathCount,
            int filteredPathCount) {
        GraphRagRetrievalLog log = new GraphRagRetrievalLog(
                UUID.randomUUID(),
                conversationId,
                messageId,
                null,
                null,
                traceId,
                "why is order timeout",
                retrievalMode,
                businessDomain,
                "INTERNAL",
                documentId,
                2,
                5,
                List.of("Order Service"));
        log.markCompleted(
                matchedPathCount,
                filteredPathCount,
                List.of("Order Service"),
                List.of("chunk-1", "chunk-2"),
                "count=" + filteredPathCount);
        graphRagRetrievalLogRepository.saveAndFlush(log);
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

    private record ConversationFixture(UUID conversationId, UUID messageId) {
    }
}
