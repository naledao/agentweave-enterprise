package com.agentweave.graphrag.infrastructure;

import com.agentweave.graphrag.domain.KnowledgeGraphChunkAssociation;
import com.agentweave.graphrag.domain.KnowledgeGraphEntity;
import com.agentweave.graphrag.domain.KnowledgeGraphRelationship;
import com.agentweave.graphrag.repository.Neo4jGraphRepository;
import com.agentweave.knowledge.domain.DocumentChunkEntity;
import com.agentweave.knowledge.domain.DocumentEntity;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@ConditionalOnProperty(prefix = "agentweave.graphrag.neo4j", name = "enabled", havingValue = "true")
public class HttpNeo4jGraphRepository implements Neo4jGraphRepository {

    private static final Logger log = LoggerFactory.getLogger(HttpNeo4jGraphRepository.class);

    private final GraphRagNeo4jProperties properties;
    private final RestClient restClient;

    public HttpNeo4jGraphRepository(GraphRagNeo4jProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeaders(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                    if (properties.username() != null && !properties.username().isBlank()) {
                        String token = Base64.getEncoder().encodeToString(
                                (properties.username() + ":" + safePassword()).getBytes(StandardCharsets.UTF_8));
                        headers.set("Authorization", "Basic " + token);
                    }
                })
                .build();
    }

    @Override
    public void deleteByDocumentId(UUID documentId) {
        executeTransaction(List.of(statement(
                """
                        MATCH (n)
                        WHERE n.sourceDocumentId = $documentId OR n.documentId = $documentId
                        DETACH DELETE n
                        """,
                Map.of("documentId", documentId.toString()))));
    }

    @Override
    public void upsertGraph(
            DocumentEntity document,
            List<DocumentChunkEntity> chunks,
            List<KnowledgeGraphEntity> entities,
            List<KnowledgeGraphRelationship> relationships,
            List<KnowledgeGraphChunkAssociation> chunkAssociations) {
        deleteByDocumentId(document.getId());

        List<Map<String, Object>> statements = new ArrayList<>();
        statements.add(statement(
                """
                        MERGE (d:Document {documentId: $documentId})
                        SET d.filename = $filename,
                            d.businessDomain = $businessDomain,
                            d.documentType = $documentType,
                            d.permissionLevel = $permissionLevel,
                            d.traceId = $traceId,
                            d.sourceDocumentId = $documentId
                        """,
                documentParameters(document)));

        statements.add(statement(
                """
                        UNWIND $chunks AS chunk
                        MERGE (c:Chunk {documentId: $documentId, chunkId: chunk.chunkId})
                        SET c.chunkIndex = chunk.chunkIndex,
                            c.contentLength = chunk.contentLength,
                            c.sourceDocumentId = $documentId,
                            c.businessDomain = chunk.businessDomain,
                            c.permissionLevel = chunk.permissionLevel
                        WITH c
                        MATCH (d:Document {documentId: $documentId})
                        MERGE (d)-[:HAS_CHUNK]->(c)
                        """,
                Map.of(
                        "documentId", document.getId().toString(),
                        "chunks", chunks.stream()
                                .map(this::chunkParameters)
                                .toList())));

        statements.add(statement(
                """
                        UNWIND $entities AS entity
                        MERGE (e:Entity {sourceDocumentId: $documentId, entityId: entity.entityId})
                        SET e.name = entity.name,
                            e.normalizedName = entity.normalizedName,
                            e.type = entity.type,
                            e.description = entity.description,
                            e.aliases = entity.aliases,
                            e.businessDomain = entity.businessDomain,
                            e.permissionLevel = entity.permissionLevel,
                            e.sourceChunkIds = entity.sourceChunkIds
                        """,
                Map.of(
                        "documentId", document.getId().toString(),
                        "entities", entities.stream()
                                .map(this::entityParameters)
                                .toList())));

        statements.add(statement(
                """
                        UNWIND $chunkAssociations AS association
                        MATCH (c:Chunk {documentId: $documentId, chunkId: association.chunkId})
                        MATCH (e:Entity {sourceDocumentId: $documentId, entityId: association.entityId})
                        MERGE (c)-[r:MENTIONS]->(e)
                        SET r.mentionCount = association.mentionCount,
                            r.sourceDocumentId = $documentId,
                            r.chunkId = association.chunkId
                        """,
                Map.of(
                        "documentId", document.getId().toString(),
                        "chunkAssociations", chunkAssociations.stream()
                                .map(this::chunkAssociationParameters)
                                .toList())));

        for (KnowledgeGraphRelationship relationship : relationships) {
            statements.add(statement(
                    """
                            MATCH (source:Entity {sourceDocumentId: $documentId, entityId: $sourceEntityId})
                            MATCH (target:Entity {sourceDocumentId: $documentId, entityId: $targetEntityId})
                            MERGE (source)-[r:%s {relationshipId: $relationshipId}]->(target)
                            SET r.sourceDocumentId = $documentId,
                                r.sourceChunkId = $sourceChunkId,
                                r.description = $description,
                                r.confidence = $confidence,
                                r.type = $type
                            """.formatted(relationship.getType().name()),
                    relationshipParameters(document.getId(), relationship)));
        }

        executeTransaction(statements);
    }

    private Map<String, Object> documentParameters(DocumentEntity document) {
        return Map.of(
                "documentId", document.getId().toString(),
                "filename", document.getFilename(),
                "businessDomain", document.getBusinessDomain(),
                "documentType", document.getDocumentType(),
                "permissionLevel", document.getPermissionLevel(),
                "traceId", document.getTraceId() == null ? "" : document.getTraceId());
    }

    private Map<String, Object> chunkParameters(DocumentChunkEntity chunk) {
        return Map.of(
                "chunkId", chunk.getId().toString(),
                "chunkIndex", chunk.getChunkIndex(),
                "contentLength", chunk.getContentLength(),
                "businessDomain", chunk.getMetadata().getOrDefault("businessDomain", ""),
                "permissionLevel", chunk.getMetadata().getOrDefault("permissionLevel", ""));
    }

    private Map<String, Object> entityParameters(KnowledgeGraphEntity entity) {
        return Map.of(
                "entityId", entity.getId().toString(),
                "name", entity.getName(),
                "normalizedName", entity.getNormalizedName(),
                "type", entity.getType().name(),
                "description", entity.getDescription() == null ? "" : entity.getDescription(),
                "aliases", entity.getAliases(),
                "businessDomain", entity.getBusinessDomain(),
                "permissionLevel", entity.getPermissionLevel(),
                "sourceChunkIds", entity.getSourceChunkIds().stream()
                        .map(UUID::toString)
                        .toList());
    }

    private Map<String, Object> chunkAssociationParameters(KnowledgeGraphChunkAssociation association) {
        return Map.of(
                "chunkId", association.getChunkId().toString(),
                "entityId", association.getEntityId().toString(),
                "mentionCount", association.getMentionCount());
    }

    private Map<String, Object> relationshipParameters(UUID documentId, KnowledgeGraphRelationship relationship) {
        return Map.of(
                "documentId", documentId.toString(),
                "relationshipId", relationship.getId().toString(),
                "sourceEntityId", relationship.getSourceEntityId().toString(),
                "targetEntityId", relationship.getTargetEntityId().toString(),
                "sourceChunkId", relationship.getSourceChunkId().toString(),
                "description", relationship.getDescription() == null ? "" : relationship.getDescription(),
                "confidence", relationship.getConfidence(),
                "type", relationship.getType().name());
    }

    private Map<String, Object> statement(String cypher, Map<String, Object> parameters) {
        return Map.of("statement", cypher, "parameters", parameters);
    }

    private void executeTransaction(List<Map<String, Object>> statements) {
        Map<String, Object> request = Map.of("statements", statements);
        Neo4jCommitResponse response = restClient.post()
                .uri("/db/{database}/tx/commit", properties.database())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Neo4jCommitResponse.class);
        if (response == null) {
            throw new IllegalStateException("neo4j response was empty");
        }
        if (response.errors() != null && !response.errors().isEmpty()) {
            throw new IllegalStateException("neo4j commit failed: " + response.errors().get(0).message());
        }
    }

    private String safePassword() {
        return properties.password() == null ? "" : properties.password();
    }

    private record Neo4jCommitResponse(List<Neo4jError> errors) {
    }

    private record Neo4jError(String code, String message) {
    }
}
