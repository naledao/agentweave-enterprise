package com.agentweave.graphrag.application;

import com.agentweave.graphrag.domain.KnowledgeGraphEntity;
import com.agentweave.graphrag.domain.KnowledgeGraphEntityAlias;
import com.agentweave.graphrag.domain.KnowledgeGraphRelationship;
import com.agentweave.graphrag.dto.GraphPathResponse;
import com.agentweave.graphrag.dto.GraphRagRetrievalRequest;
import com.agentweave.graphrag.dto.GraphRagRetrievalResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GraphPathSearchService {

    private static final int DEFAULT_MAX_PATH_COUNT = 5;

    private final GraphEntityResolver graphEntityResolver;

    public GraphPathSearchService(GraphEntityResolver graphEntityResolver) {
        this.graphEntityResolver = graphEntityResolver;
    }

    public GraphRagRetrievalResponse search(
            GraphRagRetrievalRequest request,
            List<KnowledgeGraphEntity> entities,
            List<KnowledgeGraphEntityAlias> aliases,
            List<KnowledgeGraphRelationship> relationships) {
        List<KnowledgeGraphEntity> filteredEntities = filterEntities(request, entities);
        if (filteredEntities.isEmpty()) {
            return GraphRagRetrievalResponse.empty();
        }

        Map<UUID, KnowledgeGraphEntity> entitiesById = indexEntities(filteredEntities);
        List<KnowledgeGraphEntityAlias> filteredAliases = aliases.stream()
                .filter(alias -> entitiesById.containsKey(alias.getEntityId()))
                .toList();
        List<KnowledgeGraphRelationship> filteredRelationships = relationships.stream()
                .filter(relationship -> entitiesById.containsKey(relationship.getSourceEntityId())
                        && entitiesById.containsKey(relationship.getTargetEntityId()))
                .toList();

        List<GraphEntityMatch> matches = graphEntityResolver.resolve(
                request.normalizedQuery(),
                filteredEntities,
                filteredAliases);
        List<String> resolvedEntities = matches.stream()
                .map(match -> match.entity().getName())
                .toList();
        if (!graphEntityResolver.shouldSearchGraph(request.normalizedQuery(), matches)) {
            return new GraphRagRetrievalResponse(
                    List.of(),
                    resolvedEntities,
                    List.of(),
                    confidenceSummary(List.of()),
                    0,
                    0);
        }

        Map<UUID, List<GraphEdge>> adjacency = buildAdjacency(filteredRelationships);
        List<PathDraft> rawPaths = new ArrayList<>();
        for (GraphEntityMatch match : matches.stream().limit(4).toList()) {
            explore(
                    match.entity().getId(),
                    match.score(),
                    request.maxDepth(),
                    entitiesById,
                    adjacency,
                    new ArrayList<>(List.of(match.entity().getId())),
                    new ArrayList<>(),
                    rawPaths);
        }

        int maxPathCount = request.maxPathCount() == null ? DEFAULT_MAX_PATH_COUNT : request.maxPathCount();
        List<PathDraft> deduplicated = deduplicate(rawPaths).stream()
                .sorted(Comparator.comparing(PathDraft::confidence).reversed()
                        .thenComparing(PathDraft::depth)
                        .thenComparing(PathDraft::pathId))
                .limit(maxPathCount)
                .toList();

        List<GraphPathResponse> graphPaths = deduplicated.stream()
                .map(PathDraft::toResponse)
                .toList();
        return new GraphRagRetrievalResponse(
                graphPaths,
                resolvedEntities,
                graphPaths.stream()
                        .flatMap(path -> path.sourceChunkIds().stream())
                        .distinct()
                        .toList(),
                confidenceSummary(graphPaths),
                rawPaths.size(),
                Math.max(0, rawPaths.size() - graphPaths.size()));
    }

    private List<KnowledgeGraphEntity> filterEntities(GraphRagRetrievalRequest request, List<KnowledgeGraphEntity> entities) {
        return entities.stream()
                .filter(entity -> request.normalizedDocumentId() == null
                        || request.documentId().equals(entity.getSourceDocumentId()))
                .filter(entity -> request.normalizedBusinessDomain() == null
                        || request.normalizedBusinessDomain().equals(entity.getBusinessDomain()))
                .filter(entity -> request.normalizedPermissionLevel() == null
                        || request.normalizedPermissionLevel().equals(entity.getPermissionLevel()))
                .toList();
    }

    private Map<UUID, KnowledgeGraphEntity> indexEntities(List<KnowledgeGraphEntity> entities) {
        Map<UUID, KnowledgeGraphEntity> indexed = new LinkedHashMap<>();
        for (KnowledgeGraphEntity entity : entities) {
            indexed.put(entity.getId(), entity);
        }
        return indexed;
    }

    private Map<UUID, List<GraphEdge>> buildAdjacency(List<KnowledgeGraphRelationship> relationships) {
        Map<UUID, List<GraphEdge>> adjacency = new LinkedHashMap<>();
        for (KnowledgeGraphRelationship relationship : relationships) {
            GraphEdge forward = new GraphEdge(
                    relationship.getId(),
                    relationship.getTargetEntityId(),
                    relationship.getType().name(),
                    relationship.getConfidence(),
                    relationship.getSourceChunkId());
            GraphEdge reverse = new GraphEdge(
                    relationship.getId(),
                    relationship.getSourceEntityId(),
                    relationship.getType().name(),
                    relationship.getConfidence(),
                    relationship.getSourceChunkId());
            adjacency.computeIfAbsent(relationship.getSourceEntityId(), ignored -> new ArrayList<>()).add(forward);
            adjacency.computeIfAbsent(relationship.getTargetEntityId(), ignored -> new ArrayList<>()).add(reverse);
        }
        return adjacency;
    }

    private void explore(
            UUID currentEntityId,
            double seedScore,
            int maxDepth,
            Map<UUID, KnowledgeGraphEntity> entitiesById,
            Map<UUID, List<GraphEdge>> adjacency,
            List<UUID> entityPath,
            List<GraphEdge> relationshipPath,
            List<PathDraft> results) {
        if (!relationshipPath.isEmpty()) {
            results.add(toDraft(entityPath, relationshipPath, seedScore, entitiesById));
        }
        if (relationshipPath.size() >= maxDepth) {
            return;
        }
        for (GraphEdge edge : adjacency.getOrDefault(currentEntityId, List.of())) {
            if (entityPath.contains(edge.neighborId())) {
                continue;
            }
            entityPath.add(edge.neighborId());
            relationshipPath.add(edge);
            explore(edge.neighborId(), seedScore, maxDepth, entitiesById, adjacency, entityPath, relationshipPath, results);
            relationshipPath.remove(relationshipPath.size() - 1);
            entityPath.remove(entityPath.size() - 1);
        }
    }

    private PathDraft toDraft(
            List<UUID> entityPath,
            List<GraphEdge> relationshipPath,
            double seedScore,
            Map<UUID, KnowledgeGraphEntity> entitiesById) {
        List<String> entityNames = entityPath.stream()
                .map(entitiesById::get)
                .filter(entity -> entity != null)
                .map(KnowledgeGraphEntity::getName)
                .toList();
        List<String> relationshipTypes = relationshipPath.stream()
                .map(GraphEdge::relationshipType)
                .toList();
        List<String> sourceChunkIds = new ArrayList<>();
        LinkedHashSet<String> uniqueChunkIds = new LinkedHashSet<>();
        for (UUID entityId : entityPath) {
            KnowledgeGraphEntity entity = entitiesById.get(entityId);
            if (entity == null) {
                continue;
            }
            for (UUID chunkId : entity.getSourceChunkIds()) {
                uniqueChunkIds.add(chunkId.toString());
            }
        }
        for (GraphEdge edge : relationshipPath) {
            uniqueChunkIds.add(edge.sourceChunkId().toString());
        }
        sourceChunkIds.addAll(uniqueChunkIds);

        double averageRelationshipConfidence = relationshipPath.stream()
                .mapToDouble(GraphEdge::confidence)
                .average()
                .orElse(0.0d);
        double confidence = clamp(averageRelationshipConfidence * seedScore);
        String canonicalKey = canonicalKey(entityPath, relationshipPath);
        return new PathDraft(
                canonicalKey,
                deterministicPathId(canonicalKey),
                entityNames,
                relationshipTypes,
                sourceChunkIds,
                confidence,
                relationshipPath.size());
    }

    private List<PathDraft> deduplicate(List<PathDraft> drafts) {
        Map<String, PathDraft> deduplicated = new LinkedHashMap<>();
        for (PathDraft draft : drafts) {
            deduplicated.putIfAbsent(draft.canonicalKey(), draft);
        }
        return new ArrayList<>(deduplicated.values());
    }

    private String canonicalKey(List<UUID> entityPath, List<GraphEdge> relationshipPath) {
        String forward = signature(entityPath, relationshipPath);
        String reverse = signature(reversedEntityPath(entityPath), reversedRelationshipPath(relationshipPath));
        return forward.compareTo(reverse) <= 0 ? forward : reverse;
    }

    private List<UUID> reversedEntityPath(List<UUID> values) {
        List<UUID> reversed = new ArrayList<>(values);
        java.util.Collections.reverse(reversed);
        return reversed;
    }

    private List<GraphEdge> reversedRelationshipPath(List<GraphEdge> values) {
        List<GraphEdge> reversed = new ArrayList<>(values);
        java.util.Collections.reverse(reversed);
        return reversed;
    }

    private String signature(List<UUID> entityPath, List<GraphEdge> relationshipPath) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < entityPath.size(); index++) {
            if (index > 0) {
                builder.append("->");
            }
            builder.append(entityPath.get(index));
        }
        builder.append('|');
        for (int index = 0; index < relationshipPath.size(); index++) {
            if (index > 0) {
                builder.append("->");
            }
            builder.append(relationshipPath.get(index).relationshipId());
        }
        return builder.toString();
    }

    private String deterministicPathId(String canonicalKey) {
        return UUID.nameUUIDFromBytes(canonicalKey.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String confidenceSummary(List<GraphPathResponse> graphPaths) {
        if (graphPaths.isEmpty()) {
            return "count=0";
        }
        double min = graphPaths.stream().mapToDouble(path -> path.confidence() == null ? 0.0d : path.confidence()).min().orElse(0.0d);
        double max = graphPaths.stream().mapToDouble(path -> path.confidence() == null ? 0.0d : path.confidence()).max().orElse(0.0d);
        double avg = graphPaths.stream().mapToDouble(path -> path.confidence() == null ? 0.0d : path.confidence()).average().orElse(0.0d);
        return "count=%d,min=%.3f,max=%.3f,avg=%.3f".formatted(graphPaths.size(), min, max, avg);
    }

    private double clamp(double value) {
        return Math.max(0.0d, Math.min(1.0d, value));
    }

    private record GraphEdge(
            UUID relationshipId,
            UUID neighborId,
            String relationshipType,
            double confidence,
            UUID sourceChunkId) {
    }

    private record PathDraft(
            String canonicalKey,
            String pathId,
            List<String> entities,
            List<String> relationships,
            List<String> sourceChunkIds,
            double confidence,
            int depth) {

        GraphPathResponse toResponse() {
            return new GraphPathResponse(pathId, depth, entities, relationships, sourceChunkIds, confidence);
        }
    }
}
