package com.agentweave.graphrag.application;

import com.agentweave.graphrag.domain.KnowledgeGraphChunkAssociation;
import com.agentweave.graphrag.domain.KnowledgeGraphEntity;
import com.agentweave.graphrag.domain.KnowledgeGraphEntityAlias;
import com.agentweave.graphrag.domain.KnowledgeGraphEntityType;
import com.agentweave.graphrag.dto.GraphRagChunkExtraction;
import com.agentweave.graphrag.dto.GraphRagEntityCandidate;
import com.agentweave.graphrag.dto.GraphRagRelationshipCandidate;
import com.agentweave.knowledge.domain.DocumentEntity;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class GraphRagEntityNormalizer {

    public GraphRagEntityNormalizationResult normalize(
            DocumentEntity document,
            List<GraphRagChunkExtraction> chunkExtractions) {
        Map<GraphRagEntityKey, EntityAggregate> aggregates = new LinkedHashMap<>();
        for (GraphRagChunkExtraction chunkExtraction : chunkExtractions) {
            collectExplicitEntities(document, chunkExtraction, aggregates);
            collectImplicitRelationshipEntities(document, chunkExtraction, aggregates);
        }

        List<KnowledgeGraphEntity> entities = new ArrayList<>();
        List<KnowledgeGraphEntityAlias> aliases = new ArrayList<>();
        List<KnowledgeGraphChunkAssociation> associations = new ArrayList<>();
        Map<GraphRagEntityKey, UUID> entityIdsByKey = new LinkedHashMap<>();

        for (Map.Entry<GraphRagEntityKey, EntityAggregate> entry : aggregates.entrySet()) {
            GraphRagEntityKey key = entry.getKey();
            EntityAggregate aggregate = entry.getValue();
            KnowledgeGraphEntity entity = new KnowledgeGraphEntity(
                    aggregate.id,
                    document.getId(),
                    aggregate.preferredName(),
                    key.normalizedName(),
                    key.type(),
                    aggregate.description(),
                    aggregate.aliases(),
                    document.getBusinessDomain(),
                    document.getPermissionLevel(),
                    aggregate.sourceChunkIds());
            entities.add(entity);
            entityIdsByKey.put(key, aggregate.id);

            for (String alias : aggregate.aliases()) {
                aliases.add(new KnowledgeGraphEntityAlias(
                        UUID.randomUUID(),
                        aggregate.id,
                        document.getId(),
                        alias,
                        normalizeText(alias)));
            }

            for (Map.Entry<UUID, Integer> chunkMention : aggregate.chunkMentions().entrySet()) {
                associations.add(new KnowledgeGraphChunkAssociation(
                        UUID.randomUUID(),
                        document.getId(),
                        chunkMention.getKey(),
                        aggregate.id,
                        chunkMention.getValue()));
            }
        }

        return new GraphRagEntityNormalizationResult(entities, aliases, associations, entityIdsByKey);
    }

    private void collectExplicitEntities(
            DocumentEntity document,
            GraphRagChunkExtraction chunkExtraction,
            Map<GraphRagEntityKey, EntityAggregate> aggregates) {
        for (GraphRagEntityCandidate candidate : chunkExtraction.entities()) {
            EntityAggregate aggregate = aggregate(
                    aggregates,
                    candidate.name(),
                    candidate.type(),
                    candidate.description(),
                    candidate.aliases(),
                    chunkExtraction.chunkId());
            aggregate.recordMention(chunkExtraction.chunkId());
            aggregate.addSourceType(KnowledgeGraphEntityType.from(candidate.type()));
        }
    }

    private void collectImplicitRelationshipEntities(
            DocumentEntity document,
            GraphRagChunkExtraction chunkExtraction,
            Map<GraphRagEntityKey, EntityAggregate> aggregates) {
        for (GraphRagRelationshipCandidate candidate : chunkExtraction.relationships()) {
            EntityAggregate source = aggregate(
                    aggregates,
                    candidate.sourceName(),
                    candidate.sourceType(),
                    candidate.description(),
                    List.of(candidate.sourceName()),
                    chunkExtraction.chunkId());
            source.recordMention(chunkExtraction.chunkId());
            source.addSourceType(KnowledgeGraphEntityType.from(candidate.sourceType()));

            EntityAggregate target = aggregate(
                    aggregates,
                    candidate.targetName(),
                    candidate.targetType(),
                    candidate.description(),
                    List.of(candidate.targetName()),
                    chunkExtraction.chunkId());
            target.recordMention(chunkExtraction.chunkId());
            target.addSourceType(KnowledgeGraphEntityType.from(candidate.targetType()));
        }
    }

    private EntityAggregate aggregate(
            Map<GraphRagEntityKey, EntityAggregate> aggregates,
            String name,
            String type,
            String description,
            List<String> aliases,
            UUID chunkId) {
        String normalizedName = normalizeText(name);
        KnowledgeGraphEntityType entityType = KnowledgeGraphEntityType.from(type);
        GraphRagEntityKey key = new GraphRagEntityKey(normalizedName, entityType);
        EntityAggregate aggregate = aggregates.computeIfAbsent(key, ignored -> new EntityAggregate(name, description));
        aggregate.merge(name, description, aliases, chunkId);
        return aggregate;
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .trim();
        return normalized.replaceAll("[\\p{Punct}\\s]+", " ");
    }

    private static final class EntityAggregate {

        private final UUID id = UUID.randomUUID();
        private String preferredName;
        private String description;
        private final LinkedHashMap<String, String> aliases = new LinkedHashMap<>();
        private final LinkedHashSet<UUID> sourceChunkIds = new LinkedHashSet<>();
        private final LinkedHashMap<UUID, Integer> chunkMentions = new LinkedHashMap<>();
        private final LinkedHashSet<KnowledgeGraphEntityType> sourceTypes = new LinkedHashSet<>();

        private EntityAggregate(String name, String description) {
            this.preferredName = name == null || name.isBlank() ? "unknown" : name.trim();
            this.description = description;
        }

        private void merge(String name, String description, List<String> aliases, UUID chunkId) {
            if (name != null && !name.isBlank() && preferredName.length() < name.trim().length()) {
                preferredName = name.trim();
            }
            if (description != null && !description.isBlank()) {
                if (this.description == null || description.length() > this.description.length()) {
                    this.description = description.trim();
                }
            }
            if (aliases != null) {
                aliases.stream()
                        .filter(alias -> alias != null && !alias.isBlank())
                        .map(String::trim)
                        .forEach(this::addAlias);
            }
            if (name != null && !name.isBlank()) {
                addAlias(name.trim());
            }
            sourceChunkIds.add(chunkId);
        }

        private void recordMention(UUID chunkId) {
            chunkMentions.merge(chunkId, 1, Integer::sum);
            sourceChunkIds.add(chunkId);
        }

        private void addSourceType(KnowledgeGraphEntityType type) {
            sourceTypes.add(type);
        }

        private String preferredName() {
            return preferredName;
        }

        private String description() {
            return description;
        }

        private List<String> aliases() {
            return new ArrayList<>(aliases.values());
        }

        private List<UUID> sourceChunkIds() {
            return sourceChunkIds.stream().toList();
        }

        private Map<UUID, Integer> chunkMentions() {
            return chunkMentions;
        }

        private void addAlias(String alias) {
            String normalizedAlias = normalizeText(alias);
            aliases.putIfAbsent(normalizedAlias, alias);
        }
    }
}
