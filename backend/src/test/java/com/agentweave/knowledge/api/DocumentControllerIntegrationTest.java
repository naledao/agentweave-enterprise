package com.agentweave.knowledge.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.agentweave.conversation.application.MessageMetadataService;
import com.agentweave.conversation.application.RagPromptContext;
import com.agentweave.conversation.domain.ConversationEntity;
import com.agentweave.conversation.domain.ConversationMessageEntity;
import com.agentweave.conversation.domain.MessageRole;
import com.agentweave.conversation.domain.MessageStatus;
import com.agentweave.conversation.dto.CitationEventResponse;
import com.agentweave.conversation.repository.ConversationRepository;
import com.agentweave.knowledge.application.DocumentStorageException;
import com.agentweave.knowledge.application.DocumentApplicationService;
import com.agentweave.knowledge.application.DocumentStorageService;
import com.agentweave.knowledge.application.GraphRagIndexCleanupService;
import com.agentweave.knowledge.application.GraphRagIndexingScheduler;
import com.agentweave.knowledge.application.PgVectorIndexService;
import com.agentweave.knowledge.application.StoredDocumentObject;
import com.agentweave.knowledge.domain.DocumentChunkEntity;
import com.agentweave.knowledge.domain.DocumentChunkStatus;
import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.domain.DocumentStatus;
import com.agentweave.knowledge.messaging.publisher.DocumentReindexRequestedEventPublisher;
import com.agentweave.knowledge.repository.DocumentChunkRepository;
import com.agentweave.knowledge.repository.DocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
class DocumentControllerIntegrationTest {

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
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageMetadataService messageMetadataService;

    @MockitoBean
    private DocumentStorageService documentStorageService;

    @MockitoBean
    private PgVectorIndexService pgVectorIndexService;

    @MockitoBean
    private GraphRagIndexCleanupService graphRagIndexCleanupService;

    @MockitoBean
    private GraphRagIndexingScheduler graphRagIndexingScheduler;

    @MockitoBean
    private DocumentReindexRequestedEventPublisher documentReindexRequestedEventPublisher;

    private String uploadToken;
    private String noPermissionToken;
    private UserEntity uploader;
    private UserEntity plainUser;

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
        PermissionEntity deletePermission = ensurePermission(
                "knowledge:document:delete",
                "Delete knowledge documents",
                PermissionType.API);

        RoleEntity uploaderRole = new RoleEntity(
                UUID.randomUUID(),
                "DOC_UPLOADER_" + suffix.toUpperCase(),
                "Document Uploader",
                null);
        uploaderRole.replacePermissions(List.of(uploadPermission, parsePermission, indexPermission, deletePermission));
        uploaderRole = roleRepository.save(uploaderRole);

        RoleEntity plainRole = new RoleEntity(
                UUID.randomUUID(),
                "DOC_READER_" + suffix.toUpperCase(),
                "Document Reader",
                null);
        plainRole = roleRepository.save(plainRole);

        uploader = new UserEntity(
                UUID.randomUUID(),
                "doc_uploader_" + suffix,
                "Document Uploader",
                passwordEncoder.encode("password123"),
                "doc_uploader_" + suffix + "@example.com");
        uploader.replaceRoles(List.of(uploaderRole));
        userRepository.save(uploader);

        plainUser = new UserEntity(
                UUID.randomUUID(),
                "doc_plain_" + suffix,
                "Document Plain",
                passwordEncoder.encode("password123"),
                "doc_plain_" + suffix + "@example.com");
        plainUser.replaceRoles(List.of(plainRole));
        userRepository.save(plainUser);

        uploadToken = login(uploader.getUsername(), "password123");
        noPermissionToken = login(plainUser.getUsername(), "password123");

        when(documentStorageService.store(any(), any(InputStream.class), anyLong(), any(), any()))
                .thenAnswer(invocation -> new StoredDocumentObject(
                        "agentweave-documents",
                        invocation.getArgument(0, String.class),
                        invocation.getArgument(4, String.class),
                        invocation.getArgument(2, Long.class)));
    }

    @Test
    void uploadDocumentStoresObjectAndCreatesUploadedRecord() throws Exception {
        String traceId = "trace-document-upload-success";

        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("runbook.txt", "hello, world!"))
                        .param("source", "runbook")
                        .param("businessDomain", "order")
                        .param("documentType", "RUNBOOK")
                        .param("permissionLevel", "INTERNAL")
                        .param("tags", "api", "timeout")
                        .header("Authorization", "Bearer " + uploadToken)
                        .header("X-Trace-Id", traceId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", traceId))
                .andExpect(jsonPath("$.documentId").isString())
                .andExpect(jsonPath("$.filename").value("runbook.txt"))
                .andExpect(jsonPath("$.contentType").value("text/plain"))
                .andExpect(jsonPath("$.fileSize").value(13))
                .andExpect(jsonPath("$.status").value("uploaded"))
                .andExpect(jsonPath("$.storage.bucket").value("agentweave-documents"))
                .andExpect(jsonPath("$.storage.objectKey", containsString("runbook.txt")))
                .andExpect(jsonPath("$.storage.checksum").isString())
                .andExpect(jsonPath("$.metadata.source").value("runbook"))
                .andExpect(jsonPath("$.metadata.businessDomain").value("order"))
                .andExpect(jsonPath("$.metadata.documentType").value("RUNBOOK"))
                .andExpect(jsonPath("$.metadata.permissionLevel").value("INTERNAL"))
                .andExpect(jsonPath("$.metadata.tags[0]").value("api"))
                .andExpect(jsonPath("$.metadata.tags[1]").value("timeout"))
                .andExpect(jsonPath("$.traceId").value(traceId))
                .andReturn();

        UUID documentId = UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("documentId")
                .asText());
        DocumentEntity document = documentRepository.findById(documentId).orElseThrow();
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(document.getStorageBucket()).isEqualTo("agentweave-documents");
        assertThat(document.getStorageObjectKey()).contains(documentId.toString());
        assertThat(document.getChecksum()).hasSize(64);
        assertThat(document.getTags()).isEqualTo("api,timeout");

        verify(documentStorageService).store(any(), any(InputStream.class), eq(13L), eq("text/plain"), any());
    }

    @Test
    void parseUploadedTextDocumentStoresCleanedTextAndMovesToCleaning() throws Exception {
        String traceId = "trace-document-parse-success";
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("runbook.txt", "  parsed   runbook   text  \r\n\r\n  and   more  "))
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
                .thenReturn(stream("  parsed   runbook   text  \r\n\r\n  and   more  "));

        mockMvc.perform(post("/api/v1/documents/{documentId}/parse", documentId)
                        .header("Authorization", "Bearer " + uploadToken)
                        .header("X-Trace-Id", traceId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", traceId))
                .andExpect(jsonPath("$.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.status").value("cleaning"))
                .andExpect(jsonPath("$.errorMessage").doesNotExist())
                .andExpect(jsonPath("$.traceId").value(traceId));

        DocumentEntity document = documentRepository.findById(documentId).orElseThrow();
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.CLEANING);
        assertThat(document.getCleanedText()).isEqualTo("parsed runbook text\n\nand more");
        assertThat(document.getTextLength()).isEqualTo(29);
        assertThat(document.getErrorMessage()).isNull();
        assertThat(document.getTraceId()).isEqualTo(traceId);
        verify(documentStorageService).read(eq("agentweave-documents"), eq(document.getStorageObjectKey()));
    }

    @Test
    void parseMarksFailedWhenCleaningRemovesAllContent() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("control-chars.txt", "\u200B\u0000\t"))
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
                .thenReturn(stream("\u200B\u0000\t"));

        mockMvc.perform(post("/api/v1/documents/{documentId}/parse", documentId)
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("cleaned text must not be blank"));

        DocumentEntity document = documentRepository.findById(documentId).orElseThrow();
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(document.getCleanedText()).isNull();
        assertThat(document.getTextLength()).isNull();
        assertThat(document.getErrorMessage()).isEqualTo("cleaned text must not be blank");
    }

    @Test
    void parseRejectsUnsupportedDocumentTypeAndRecordsFailure() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(file("guide.pdf", "application/pdf", "%PDF demo"))
                        .param("source", "manual")
                        .param("businessDomain", "order")
                        .param("documentType", "MANUAL")
                        .param("permissionLevel", "INTERNAL")
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isOk())
                .andReturn();

        UUID documentId = UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("documentId")
                .asText());
        when(documentStorageService.read(eq("agentweave-documents"), any()))
                .thenReturn(stream("%PDF demo"));

        mockMvc.perform(post("/api/v1/documents/{documentId}/parse", documentId)
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("unsupported document file type for parsing"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        DocumentEntity document = documentRepository.findById(documentId).orElseThrow();
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(document.getCleanedText()).isNull();
        assertThat(document.getErrorMessage()).isEqualTo("unsupported document file type for parsing");
        assertThat(document.getTraceId()).isNotBlank();
    }

    @Test
    void parseMarksFailedWhenOriginalObjectIsMissing() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("missing-object.txt", "hello, world!"))
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
                .thenThrow(new IOException("object not found"));

        mockMvc.perform(post("/api/v1/documents/{documentId}/parse", documentId)
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.message").value("object not found"));

        DocumentEntity document = documentRepository.findById(documentId).orElseThrow();
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(document.getCleanedText()).isNull();
        assertThat(document.getErrorMessage()).isEqualTo("object not found");
        assertThat(document.getTraceId()).isNotBlank();
    }

    @Test
    void parseReturnsCleanedDocumentWhenAlreadyCleaned() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("already-parsed.txt", "hello, world!"))
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
                .thenReturn(stream("parsed once"));

        mockMvc.perform(post("/api/v1/documents/{documentId}/parse", documentId)
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cleaning"));

        mockMvc.perform(post("/api/v1/documents/{documentId}/parse", documentId)
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cleaning"));

        DocumentEntity document = documentRepository.findById(documentId).orElseThrow();
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.CLEANING);
        assertThat(document.getCleanedText()).isEqualTo("parsed once");
    }

    @Test
    void parseRequiresDocumentPermission() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("parse-permission.txt", "hello, world!"))
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

        mockMvc.perform(post("/api/v1/documents/{documentId}/parse", documentId)
                        .header("Authorization", "Bearer " + noPermissionToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));
    }

    @Test
    void replaceChunksWritesCompleteMetadataForRagFilters() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("runbook.txt", "hello, world!"))
                        .param("source", "runbook")
                        .param("businessDomain", "order")
                        .param("documentType", "RUNBOOK")
                        .param("permissionLevel", "INTERNAL")
                        .param("tags", "api", "timeout")
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isOk())
                .andReturn();

        UUID documentId = UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("documentId")
                .asText());

        List<DocumentChunkEntity> savedChunks = documentApplicationService.replaceChunks(
                documentId,
                List.of("first searchable chunk", "second searchable chunk"));

        assertThat(savedChunks).hasSize(2);
        assertThat(documentChunkRepository.countByDocumentId(documentId)).isEqualTo(2);
        assertThat(savedChunks)
                .extracting(DocumentChunkEntity::getChunkIndex)
                .containsExactly(0, 1);

        DocumentChunkEntity firstChunk = savedChunks.get(0);
        assertThat(firstChunk.getStatus()).isEqualTo(DocumentChunkStatus.PENDING_EMBEDDING);
        assertThat(firstChunk.getContent()).isEqualTo("first searchable chunk");
        assertThat(firstChunk.getContentLength()).isEqualTo("first searchable chunk".length());
        assertThat(firstChunk.getMetadata())
                .containsEntry("documentId", documentId.toString())
                .containsEntry("chunkId", firstChunk.getId().toString())
                .containsEntry("source", "runbook")
                .containsEntry("businessDomain", "order")
                .containsEntry("documentType", "RUNBOOK")
                .containsEntry("permissionLevel", "INTERNAL");
        assertThat(firstChunk.getMetadata().get("createdAt")).isInstanceOf(String.class);
        assertThat(firstChunk.getMetadata().get("tags")).isEqualTo(List.of("api", "timeout"));
    }

    @Test
    void chunkDocumentSplitsTextAndMovesDocumentToEmbedding() throws Exception {
        String traceId = "trace-document-chunking-success";
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("runbook.txt", "hello, world!"))
                        .param("source", "runbook")
                        .param("businessDomain", "order")
                        .param("documentType", "RUNBOOK")
                        .param("permissionLevel", "INTERNAL")
                        .param("tags", "api", "timeout")
                        .header("Authorization", "Bearer " + uploadToken)
                        .header("X-Trace-Id", traceId))
                .andExpect(status().isOk())
                .andReturn();

        UUID documentId = UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("documentId")
                .asText());

        List<DocumentChunkEntity> savedChunks = documentApplicationService.chunkDocument(
                documentId,
                "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda");

        DocumentEntity document = documentRepository.findById(documentId).orElseThrow();
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.EMBEDDING);
        assertThat(document.getTraceId()).isNotBlank();
        assertThat(document.getErrorMessage()).isNull();
        assertThat(savedChunks).hasSize(1);
        assertThat(savedChunks.get(0).getMetadata())
                .containsEntry("documentId", documentId.toString())
                .containsEntry("businessDomain", "order")
                .containsEntry("permissionLevel", "INTERNAL");
    }

    @Test
    void chunkDocumentMarksDocumentFailedWhenCleanedTextIsBlank() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("runbook.txt", "hello, world!"))
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

        assertThatThrownBy(() -> documentApplicationService.chunkDocument(documentId, "  "))
                .hasMessage("cleaned text must not be blank");

        DocumentEntity document = documentRepository.findById(documentId).orElseThrow();
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(document.getErrorMessage()).isEqualTo("cleaned text must not be blank");
        assertThat(document.getTraceId()).isNotBlank();
        assertThat(documentChunkRepository.countByDocumentId(documentId)).isZero();
    }

    @Test
    void indexDocumentWritesChunksToVectorStoreAndMarksIndexed() throws Exception {
        String traceId = "trace-document-index-success";
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("runbook-index.txt", "hello, world!"))
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
        List<DocumentChunkEntity> chunks = documentApplicationService.chunkDocument(
                documentId,
                "first searchable chunk\n\nsecond searchable chunk");

        mockMvc.perform(post("/api/v1/documents/{documentId}/index", documentId)
                        .header("Authorization", "Bearer " + uploadToken)
                        .header("X-Trace-Id", traceId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", traceId))
                .andExpect(jsonPath("$.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.status").value("indexed"))
                .andExpect(jsonPath("$.traceId").value(traceId))
                .andExpect(jsonPath("$.chunkCount").value(chunks.size()))
                .andExpect(jsonPath("$.indexedAt").isString());

        DocumentEntity document = documentRepository.findById(documentId).orElseThrow();
        List<DocumentChunkEntity> indexedChunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.INDEXED);
        assertThat(document.getIndexedAt()).isNotNull();
        assertThat(indexedChunks)
                .extracting(DocumentChunkEntity::getStatus)
                .containsOnly(DocumentChunkStatus.INDEXED);
        assertThat(indexedChunks)
                .extracting(DocumentChunkEntity::getVectorId)
                .containsExactlyElementsOf(indexedChunks.stream().map(DocumentChunkEntity::getId).toList());
        assertThat(indexedChunks)
                .allSatisfy(chunk -> {
                    assertThat(chunk.getEmbeddedAt()).isNotNull();
                    assertThat(chunk.getTraceId()).isEqualTo(traceId);
                    assertThat(chunk.getErrorMessage()).isNull();
                });
        verify(pgVectorIndexService).index(any());
        verify(graphRagIndexingScheduler).enqueue(documentId, traceId);
    }

    @Test
    void indexDocumentMarksFailedWhenVectorStoreFails() throws Exception {
        String traceId = "trace-document-index-failure";
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("runbook-index-fail.txt", "hello, world!"))
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
        documentApplicationService.chunkDocument(documentId, "searchable chunk");
        doThrow(new IllegalStateException("embedding service unavailable"))
                .when(pgVectorIndexService)
                .index(any());

        mockMvc.perform(post("/api/v1/documents/{documentId}/index", documentId)
                        .header("Authorization", "Bearer " + uploadToken)
                        .header("X-Trace-Id", traceId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.message").value("embedding service unavailable"))
                .andExpect(jsonPath("$.traceId").value(traceId));

        DocumentEntity document = documentRepository.findById(documentId).orElseThrow();
        List<DocumentChunkEntity> chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(document.getErrorMessage()).isEqualTo("embedding service unavailable");
        assertThat(document.getTraceId()).isEqualTo(traceId);
        assertThat(chunks)
                .extracting(DocumentChunkEntity::getStatus)
                .containsOnly(DocumentChunkStatus.FAILED);
        assertThat(chunks)
                .allSatisfy(chunk -> {
                    assertThat(chunk.getErrorMessage()).isEqualTo("embedding service unavailable");
                    assertThat(chunk.getTraceId()).isEqualTo(traceId);
                });
    }

    @Test
    void indexDocumentRequiresDocumentPermission() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("index-permission.txt", "hello, world!"))
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

        mockMvc.perform(post("/api/v1/documents/{documentId}/index", documentId)
                        .header("Authorization", "Bearer " + noPermissionToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));
    }

    @Test
    void reindexDocumentPublishesAsyncTaskAndKeepsCurrentState() throws Exception {
        String initialTraceId = "trace-document-reindex-initial";
        String reindexTraceId = "trace-document-reindex-success";
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("runbook-reindex.txt", "hello, world!"))
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
                .thenReturn(stream("first searchable chunk\n\nsecond searchable chunk"));
        mockMvc.perform(post("/api/v1/documents/{documentId}/parse", documentId)
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isOk());
        documentApplicationService.chunkDocument(
                documentId,
                "first searchable chunk\n\nsecond searchable chunk");
        mockMvc.perform(post("/api/v1/documents/{documentId}/index", documentId)
                        .header("Authorization", "Bearer " + uploadToken)
                        .header("X-Trace-Id", initialTraceId))
                .andExpect(status().isOk());
        List<UUID> oldChunkIds = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId).stream()
                .map(DocumentChunkEntity::getId)
                .toList();

        mockMvc.perform(post("/api/v1/documents/{documentId}/reindex", documentId)
                        .header("Authorization", "Bearer " + uploadToken)
                        .header("X-Trace-Id", reindexTraceId))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Trace-Id", reindexTraceId))
                .andExpect(jsonPath("$.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.status").value("indexed"))
                .andExpect(jsonPath("$.traceId").value(initialTraceId))
                .andExpect(jsonPath("$.reindexCount").value(0))
                .andExpect(jsonPath("$.chunkCount").value(2))
                .andExpect(jsonPath("$.indexedAt").isString());

        DocumentEntity document = documentRepository.findById(documentId).orElseThrow();
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.INDEXED);
        assertThat(document.getReindexCount()).isZero();
        assertThat(document.getTraceId()).isEqualTo(initialTraceId);
        assertThat(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId))
                .extracting(DocumentChunkEntity::getStatus)
                .containsOnly(DocumentChunkStatus.INDEXED);
        assertThat(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId))
                .extracting(DocumentChunkEntity::getId)
                .containsExactlyElementsOf(oldChunkIds);
        verify(documentReindexRequestedEventPublisher)
                .publish(any(DocumentEntity.class), eq(reindexTraceId), any(UUID.class));
    }

    @Test
    void reindexDocumentCanQueueUnparsedDocumentForBackgroundValidation() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("reindex-unparsed.txt", "hello, world!"))
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

        mockMvc.perform(post("/api/v1/documents/{documentId}/reindex", documentId)
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.status").value("uploaded"));

        DocumentEntity document = documentRepository.findById(documentId).orElseThrow();
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(document.getErrorMessage()).isNull();
        assertThat(document.getReindexCount()).isZero();
        verify(documentReindexRequestedEventPublisher)
                .publish(any(DocumentEntity.class), any(), any(UUID.class));
    }

    @Test
    void reindexDocumentReturnsBusinessErrorWhenTaskPublishFails() throws Exception {
        String traceId = "trace-document-reindex-publish-failure";
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("reindex-publish-fail.txt", "hello, world!"))
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
        DocumentEntity document = documentRepository.findById(documentId).orElseThrow();
        doThrow(new IllegalStateException("rabbitmq unavailable"))
                .when(documentReindexRequestedEventPublisher)
                .publish(any(DocumentEntity.class), eq(traceId), any(UUID.class));

        mockMvc.perform(post("/api/v1/documents/{documentId}/reindex", documentId)
                        .header("Authorization", "Bearer " + uploadToken)
                        .header("X-Trace-Id", traceId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.message").value("rabbitmq unavailable"))
                .andExpect(jsonPath("$.traceId").value(traceId));

        DocumentEntity persistedDocument = documentRepository.findById(documentId).orElseThrow();
        assertThat(persistedDocument.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(persistedDocument.getErrorMessage()).isNull();
    }

    @Test
    void reindexDocumentRequiresDocumentPermission() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("reindex-permission.txt", "hello, world!"))
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

        mockMvc.perform(post("/api/v1/documents/{documentId}/reindex", documentId)
                        .header("Authorization", "Bearer " + noPermissionToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));
    }

    @Test
    void deleteDocumentRemovesDocumentAndChunks() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("delete-me.txt", "hello, world!"))
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
        documentApplicationService.replaceChunks(documentId, List.of("chunk one", "chunk two"));

        mockMvc.perform(delete("/api/v1/documents/{documentId}", documentId)
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isNoContent());

        assertThat(documentRepository.findById(documentId)).isEmpty();
        assertThat(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId)).isEmpty();
    }

    @Test
    void listDocumentsReturnsStatusFailureTraceAndChunkCount() throws Exception {
        String filename = "status-list-" + UUID.randomUUID() + ".txt";
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile(filename, "hello, world!"))
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
        assertThatThrownBy(() -> documentApplicationService.chunkDocument(documentId, "  "))
                .hasMessage("cleaned text must not be blank");

        mockMvc.perform(get("/api/v1/documents")
                        .param("keyword", filename)
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.items[0].filename").value(filename))
                .andExpect(jsonPath("$.items[0].uploadedBy").isString())
                .andExpect(jsonPath("$.items[0].status").value("failed"))
                .andExpect(jsonPath("$.items[0].errorMessage").value("cleaned text must not be blank"))
                .andExpect(jsonPath("$.items[0].traceId").isNotEmpty())
                .andExpect(jsonPath("$.items[0].chunkCount").value(0))
                .andExpect(jsonPath("$.items[0].indexedAt").doesNotExist())
                .andExpect(jsonPath("$.items[0].updatedAt").isString())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getDocumentReturnsChunksWithProcessingStatus() throws Exception {
        String filename = "status-detail-" + UUID.randomUUID() + ".txt";
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile(filename, "hello, world!"))
                        .param("source", "runbook")
                        .param("businessDomain", "order")
                        .param("documentType", "RUNBOOK")
                        .param("permissionLevel", "INTERNAL")
                        .param("tags", "api", "timeout")
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isOk())
                .andReturn();

        UUID documentId = UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("documentId")
                .asText());
        List<DocumentChunkEntity> chunks = documentApplicationService.replaceChunks(
                documentId,
                List.of("first searchable chunk", "second searchable chunk"));

        mockMvc.perform(get("/api/v1/documents/{documentId}", documentId)
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.filename").value(filename))
                .andExpect(jsonPath("$.status").value("uploaded"))
                .andExpect(jsonPath("$.errorMessage").doesNotExist())
                .andExpect(jsonPath("$.chunkCount").value(2))
                .andExpect(jsonPath("$.metadata.tags[0]").value("api"))
                .andExpect(jsonPath("$.metadata.tags[1]").value("timeout"))
                .andExpect(jsonPath("$.chunks[0].chunkId").value(chunks.get(0).getId().toString()))
                .andExpect(jsonPath("$.chunks[0].chunkIndex").value(0))
                .andExpect(jsonPath("$.chunks[0].content").value("first searchable chunk"))
                .andExpect(jsonPath("$.chunks[0].contentLength").value("first searchable chunk".length()))
                .andExpect(jsonPath("$.chunks[0].status").value("pending_embedding"))
                .andExpect(jsonPath("$.chunks[0].errorMessage").doesNotExist())
                .andExpect(jsonPath("$.chunks[1].chunkId").value(chunks.get(1).getId().toString()))
                .andExpect(jsonPath("$.chunks[1].chunkIndex").value(1))
                .andExpect(jsonPath("$.chunks[1].status").value("pending_embedding"));
    }

    @Test
    void getDocumentReturnsOwnedCitationRecords() throws Exception {
        String filename = "citation-record-" + UUID.randomUUID() + ".txt";
        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile(filename, "hello, world!"))
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
        createAssistantCitationMessage(
                uploader.getId(),
                documentId,
                "Owned answer with citation",
                "trace-owned-citation");
        createAssistantCitationMessage(
                plainUser.getId(),
                documentId,
                "Other user citation should stay hidden",
                "trace-hidden-citation");

        mockMvc.perform(get("/api/v1/documents/{documentId}", documentId)
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citationRecords.length()").value(1))
                .andExpect(jsonPath("$.citationRecords[0].messagePreview").value("Owned answer with citation"))
                .andExpect(jsonPath("$.citationRecords[0].traceId").value("trace-owned-citation"));
    }

    @Test
    void getDocumentReturnsNotFoundForMissingDocument() throws Exception {
        mockMvc.perform(get("/api/v1/documents/{documentId}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_404"))
                .andExpect(jsonPath("$.message").value("document not found"));
    }

    @Test
    void listDocumentsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void uploadRequiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("runbook.txt", "hello, world!"))
                        .param("source", "runbook")
                        .param("businessDomain", "order")
                        .param("documentType", "RUNBOOK")
                        .param("permissionLevel", "INTERNAL"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void uploadRequiresDocumentPermission() throws Exception {
        mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("runbook.txt", "hello, world!"))
                        .param("source", "runbook")
                        .param("businessDomain", "order")
                        .param("documentType", "RUNBOOK")
                        .param("permissionLevel", "INTERNAL")
                        .header("Authorization", "Bearer " + noPermissionToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403"));
    }

    @Test
    void uploadRejectsUnsupportedContentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                "application/x-msdownload",
                "hello".getBytes());

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("source", "runbook")
                        .param("businessDomain", "order")
                        .param("documentType", "RUNBOOK")
                        .param("permissionLevel", "INTERNAL")
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("unsupported file content type"));
    }

    @Test
    void uploadRejectsFileOverSizeLimit() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                new byte[(50 * 1024 * 1024) + 1]);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("source", "runbook")
                        .param("businessDomain", "order")
                        .param("documentType", "RUNBOOK")
                        .param("permissionLevel", "INTERNAL")
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("file size exceeds 50MB limit"));
    }

    @Test
    void uploadReturnsUnifiedErrorWhenStorageFails() throws Exception {
        doThrow(new DocumentStorageException("minio unavailable", new IllegalStateException("offline")))
                .when(documentStorageService)
                .store(any(), any(InputStream.class), eq(13L), eq("text/plain"), any());

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(textFile("runbook.txt", "hello, world!"))
                        .param("source", "runbook")
                        .param("businessDomain", "order")
                        .param("documentType", "RUNBOOK")
                        .param("permissionLevel", "INTERNAL")
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.message").value("failed to store document object"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
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

    private void createAssistantCitationMessage(
            UUID ownerUserId,
            UUID documentId,
            String content,
            String traceId) {
        ConversationEntity conversation = new ConversationEntity(
                UUID.randomUUID(),
                ownerUserId,
                "RAG citation");
        ConversationMessageEntity message = new ConversationMessageEntity(
                UUID.randomUUID(),
                ownerUserId,
                MessageRole.ASSISTANT,
                content,
                MessageStatus.SUCCEEDED,
                traceId);
        message.replaceMetadata(messageMetadataService.assistantRagMetadata(new RagPromptContext(
                "VECTOR_ONLY",
                "prompt",
                List.of(new CitationEventResponse(
                        documentId.toString(),
                        filenameFor(documentId),
                        "chunk-1",
                        filenameFor(documentId),
                        "runbook",
                        "citation snippet",
                        0.91d)),
                List.of())));
        conversation.addMessage(message);
        conversationRepository.saveAndFlush(conversation);
    }

    private String filenameFor(UUID documentId) {
        return "document-" + documentId;
    }

    private record LoginPayload(String username, String password) {
    }
}
