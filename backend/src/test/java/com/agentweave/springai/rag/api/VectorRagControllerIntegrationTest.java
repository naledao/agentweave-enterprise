package com.agentweave.springai.rag.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
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

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class VectorRagControllerIntegrationTest {

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

    @MockitoBean
    private VectorStore vectorStore;

    private String ragToken;
    private String noPermissionToken;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        PermissionEntity ragPermission = ensurePermission(
                "knowledge:rag:search",
                "Search RAG knowledge base",
                PermissionType.API);

        RoleEntity ragRole = new RoleEntity(
                UUID.randomUUID(),
                "RAG_SEARCHER_" + suffix.toUpperCase(),
                "RAG Searcher",
                null);
        ragRole.replacePermissions(List.of(ragPermission));
        ragRole = roleRepository.save(ragRole);

        RoleEntity plainRole = new RoleEntity(
                UUID.randomUUID(),
                "RAG_READER_" + suffix.toUpperCase(),
                "RAG Reader",
                null);
        plainRole = roleRepository.save(plainRole);

        UserEntity searcher = new UserEntity(
                UUID.randomUUID(),
                "rag_searcher_" + suffix,
                "RAG Searcher",
                passwordEncoder.encode("password123"),
                "rag_searcher_" + suffix + "@example.com");
        searcher.replaceRoles(List.of(ragRole));
        userRepository.save(searcher);

        UserEntity plainUser = new UserEntity(
                UUID.randomUUID(),
                "rag_plain_" + suffix,
                "RAG Plain",
                passwordEncoder.encode("password123"),
                "rag_plain_" + suffix + "@example.com");
        plainUser.replaceRoles(List.of(plainRole));
        userRepository.save(plainUser);

        ragToken = login(searcher.getUsername(), "password123");
        noPermissionToken = login(plainUser.getUsername(), "password123");
    }

    @Test
    void searchReturnsCitationsAndBuildsMetadataFilter() throws Exception {
        String documentId = UUID.randomUUID().toString();
        String chunkId = UUID.randomUUID().toString();
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(Document.builder()
                        .id(chunkId)
                        .text("订单接口超时需要先检查 payment-api 和 retry 配置")
                        .metadata(Map.of(
                                "documentId", documentId,
                                "chunkId", chunkId,
                                "source", "runbook",
                                "businessDomain", "order",
                                "documentType", "RUNBOOK",
                                "permissionLevel", "INTERNAL"))
                        .score(0.87)
                        .build()));

        mockMvc.perform(post("/api/v1/rag/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "订单接口超时怎么排查",
                                  "businessDomain": "order",
                                  "documentType": "RUNBOOK",
                                  "permissionLevel": "INTERNAL",
                                  "topK": 3,
                                  "similarityThreshold": 0.25
                                }
                                """)
                        .header("Authorization", "Bearer " + ragToken)
                        .header("X-Trace-Id", "trace-rag-search-success"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "trace-rag-search-success"))
                .andExpect(jsonPath("$.query").value("订单接口超时怎么排查"))
                .andExpect(jsonPath("$.retrievalMode").value("VECTOR_ONLY"))
                .andExpect(jsonPath("$.topK").value(3))
                .andExpect(jsonPath("$.similarityThreshold").value(0.25))
                .andExpect(jsonPath("$.filter.businessDomain").value("order"))
                .andExpect(jsonPath("$.filter.documentType").value("RUNBOOK"))
                .andExpect(jsonPath("$.filter.permissionLevel").value("INTERNAL"))
                .andExpect(jsonPath("$.citations[0].documentId").value(documentId))
                .andExpect(jsonPath("$.citations[0].chunkId").value(chunkId))
                .andExpect(jsonPath("$.citations[0].source").value("runbook"))
                .andExpect(jsonPath("$.citations[0].businessDomain").value("order"))
                .andExpect(jsonPath("$.citations[0].documentType").value("RUNBOOK"))
                .andExpect(jsonPath("$.citations[0].permissionLevel").value("INTERNAL"))
                .andExpect(jsonPath("$.citations[0].score").value(0.87))
                .andExpect(jsonPath("$.citations[0].snippet").value("订单接口超时需要先检查 payment-api 和 retry 配置"));

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest searchRequest = captor.getValue();
        assertThat(searchRequest.getQuery()).isEqualTo("订单接口超时怎么排查");
        assertThat(searchRequest.getTopK()).isEqualTo(3);
        assertThat(searchRequest.getSimilarityThreshold()).isEqualTo(0.25);
        assertThat(searchRequest.hasFilterExpression()).isTrue();
        assertThat(searchRequest.getFilterExpression().toString())
                .contains("businessDomain")
                .contains("documentType")
                .contains("permissionLevel");
    }

    @Test
    void searchDefaultsTopKAndThresholdWhenOmitted() throws Exception {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/rag/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "日志查询失败"
                                }
                                """)
                        .header("Authorization", "Bearer " + ragToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topK").value(5))
                .andExpect(jsonPath("$.similarityThreshold").value(0.0))
                .andExpect(jsonPath("$.filter").isMap())
                .andExpect(jsonPath("$.citations").isArray());

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().getTopK()).isEqualTo(5);
        assertThat(captor.getValue().getSimilarityThreshold()).isEqualTo(0.0);
        assertThat(captor.getValue().hasFilterExpression()).isFalse();
    }

    @Test
    void searchRejectsBlankQuery() throws Exception {
        mockMvc.perform(post("/api/v1/rag/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "   "
                                }
                                """)
                        .header("Authorization", "Bearer " + ragToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message", containsString("query")));
    }

    @Test
    void searchRequiresPermission() throws Exception {
        mockMvc.perform(post("/api/v1/rag/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "订单接口超时怎么排查"
                                }
                                """)
                        .header("Authorization", "Bearer " + noPermissionToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));
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
