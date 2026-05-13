package com.agentweave.graphrag.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import com.agentweave.graphrag.application.KnowledgeGraphExtractionAgent;
import com.agentweave.graphrag.domain.KnowledgeGraphChunkAssociation;
import com.agentweave.graphrag.domain.KnowledgeGraphEntity;
import com.agentweave.graphrag.domain.KnowledgeGraphEntityAlias;
import com.agentweave.graphrag.domain.KnowledgeGraphRelationship;
import com.agentweave.graphrag.dto.GraphRagEntityCandidate;
import com.agentweave.graphrag.dto.GraphRagExtractionResult;
import com.agentweave.graphrag.dto.GraphRagRelationshipCandidate;
import com.agentweave.graphrag.repository.GraphRagIndexLogRepository;
import com.agentweave.graphrag.repository.KnowledgeGraphChunkAssociationRepository;
import com.agentweave.graphrag.repository.KnowledgeGraphEntityAliasRepository;
import com.agentweave.graphrag.repository.KnowledgeGraphEntityRepository;
import com.agentweave.graphrag.repository.KnowledgeGraphRelationshipRepository;
import com.agentweave.graphrag.repository.Neo4jGraphRepository;
import com.agentweave.knowledge.application.DocumentApplicationService;
import com.agentweave.knowledge.application.DocumentStorageService;
import com.agentweave.knowledge.application.StoredDocumentObject;
import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.repository.DocumentChunkRepository;
import com.agentweave.knowledge.repository.DocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class GraphRagIndexingIntegrationTest {

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
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private DocumentApplicationService documentApplicationService;

    @Autowired
    private GraphRagIndexLogRepository graphRagIndexLogRepository;

    @Autowired
    private KnowledgeGraphEntityRepository knowledgeGraphEntityRepository;

    @Autowired
    private KnowledgeGraphEntityAliasRepository knowledgeGraphEntityAliasRepository;

    @Autowired
    private KnowledgeGraphChunkAssociationRepository knowledgeGraphChunkAssociationRepository;

    @Autowired
    private KnowledgeGraphRelationshipRepository knowledgeGraphRelationshipRepository;

    @MockitoBean
    private DocumentStorageService documentStorageService;

    @MockitoBean
    private com.agentweave.knowledge.application.PgVectorIndexService pgVectorIndexService;

    @MockitoBean
    private KnowledgeGraphExtractionAgent knowledgeGraphExtractionAgent;

    @MockitoBean
    private Neo4jGraphRepository neo4jGraphRepository;

    private String uploadToken;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        PermissionEntity uploadPermission = ensurePermission(
                "knowledge:document:upload",
                "Upload knowledge documents",
                PermissionType.API);
        PermissionEntity parsePermission = ensurePermission(
                "knowledge:document:parse",
                "Parse knowledge documents",
                PermissionType.API);
        PermissionEntity indexPermission = ensurePermission(
                "knowledge:document:index",
                "Index knowledge documents",
                PermissionType.API);

        RoleEntity uploaderRole = new RoleEntity(
                UUID.randomUUID(),
                "GRAPH_UPLOADER_" + suffix.toUpperCase(),
                "Graph Uploader",
                null);
        uploaderRole.replacePermissions(List.of(uploadPermission, parsePermission, indexPermission));
        uploaderRole = roleRepository.save(uploaderRole);

        UserEntity uploader = new UserEntity(
                UUID.randomUUID(),
                "graph_uploader_" + suffix,
                "Graph Uploader",
                passwordEncoder.encode("password123"),
                "graph_uploader_" + suffix + "@example.com");
        uploader.replaceRoles(List.of(uploaderRole));
        userRepository.save(uploader);

        uploadToken = login(uploader.getUsername(), "password123");

        when(documentStorageService.store(any(), any(InputStream.class), anyLong(), any(), any()))
                .thenAnswer(invocation -> new StoredDocumentObject(
                        "agentweave-documents",
                        invocation.getArgument(0, String.class),
                        invocation.getArgument(4, String.class),
                        invocation.getArgument(2, Long.class)));
    }

    @Test
    void indexingCreatesGraphSummaryAndPersistsGraphData() throws Exception {
        String traceId = "trace-graphrag-index-success";
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("graph.txt", "order service calls payment api"))
                        .param("source", "runbook")
                        .param("businessDomain", "order")
                        .param("documentType", "RUNBOOK")
                        .param("permissionLevel", "INTERNAL")
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isOk())
                .andReturn();

        UUID documentId = UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("documentId")
                .asText());

        when(documentStorageService.read(eq("agentweave-documents"), any()))
                .thenReturn(stream("order service calls payment api"));
        when(knowledgeGraphExtractionAgent.extract(any()))
                .thenReturn(new GraphRagExtractionResult(
                        List.of(new GraphRagEntityCandidate(
                                "Order Service",
                                "SERVICE",
                                "Handles order orchestration",
                                List.of("Order Service", "OrderService"),
                                0.91)),
                        List.of(new GraphRagRelationshipCandidate(
                                "Order Service",
                                "SERVICE",
                                "Payment API",
                                "API",
                                "CALLS",
                                "Order service calls payment api",
                                0.88))));

        mockMvc.perform(post("/api/v1/documents/{documentId}/parse", documentId)
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isOk());
        documentApplicationService.chunkDocument(documentId, "order service calls payment api");

        mockMvc.perform(post("/api/v1/documents/{documentId}/index", documentId)
                        .header("Authorization", "Bearer " + uploadToken)
                        .header("X-Trace-Id", traceId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", traceId))
                .andExpect(jsonPath("$.status").value("indexed"));

        mockMvc.perform(get("/api/v1/documents/{documentId}", documentId)
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.graphRag.status").value("indexed"))
                .andExpect(jsonPath("$.graphRag.entityCount").value(2))
                .andExpect(jsonPath("$.graphRag.relationshipCount").value(1))
                .andExpect(jsonPath("$.graphRag.chunkCount").value(1))
                .andExpect(jsonPath("$.graphRag.traceId").value(traceId))
                .andExpect(jsonPath("$.graphRag.errorMessage").doesNotExist());

        DocumentEntity document = documentRepository.findById(documentId).orElseThrow();
        assertThat(document.getStatus().name()).isEqualTo("INDEXED");
        assertThat(graphRagIndexLogRepository.findFirstByDocumentIdOrderByCreatedAtDesc(documentId))
                .isPresent();
        assertThat(knowledgeGraphEntityRepository.findBySourceDocumentIdOrderByNormalizedNameAsc(documentId))
                .hasSize(2);
        assertThat(knowledgeGraphEntityAliasRepository.findBySourceDocumentIdOrderByAliasAsc(documentId))
                .hasSize(3);
        assertThat(knowledgeGraphChunkAssociationRepository.findBySourceDocumentIdOrderByChunkIdAsc(documentId))
                .hasSize(2);
        assertThat(knowledgeGraphRelationshipRepository.findBySourceDocumentIdOrderByTypeAsc(documentId))
                .hasSize(1);
        verify(neo4jGraphRepository).deleteByDocumentId(documentId);
        verify(neo4jGraphRepository).upsertGraph(
                any(),
                any(),
                any(),
                any(),
                any());
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

    private MockMultipartFile textFile(String filename, String content) {
        return file(filename, "text/plain", content);
    }

    private MockMultipartFile file(String filename, String contentType, String content) {
        return new MockMultipartFile(
                "file",
                filename,
                contentType,
                content.getBytes(StandardCharsets.UTF_8));
    }

    private ByteArrayInputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private record LoginPayload(String username, String password) {
    }
}
