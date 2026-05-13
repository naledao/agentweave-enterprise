package com.agentweave.graphrag.application;

import com.agentweave.graphrag.domain.KnowledgeGraphEntity;
import com.agentweave.graphrag.domain.KnowledgeGraphEntityAlias;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GraphEntityResolver {

    private static final double MIN_SCORE = 0.75d;
    private static final Set<String> GRAPH_KEYWORDS = Set.of(
            "依赖",
            "调用",
            "上游",
            "下游",
            "负责人",
            "影响范围",
            "影响",
            "为什么",
            "导致",
            "关联",
            "链路",
            "路径",
            "关系",
            "多跳",
            "dependency",
            "dependencies",
            "call",
            "calls",
            "path",
            "paths",
            "relation",
            "relations",
            "impact",
            "cause",
            "causes",
            "why",
            "upstream",
            "downstream");

    public List<GraphEntityMatch> resolve(
            String query,
            List<KnowledgeGraphEntity> entities,
            List<KnowledgeGraphEntityAlias> aliases) {
        String normalizedQuery = normalizeText(query);
        List<String> fragments = queryFragments(normalizedQuery);
        Map<UUID, List<KnowledgeGraphEntityAlias>> aliasesByEntity = groupAliases(aliases);
        List<GraphEntityMatch> matches = new ArrayList<>();
        for (KnowledgeGraphEntity entity : entities) {
            GraphEntityMatch bestMatch = bestMatch(entity, aliasesByEntity.getOrDefault(entity.getId(), List.of()), normalizedQuery, fragments);
            if (bestMatch != null) {
                matches.add(bestMatch);
            }
        }
        matches.sort(Comparator.comparing(GraphEntityMatch::score).reversed()
                .thenComparing(match -> match.entity().getName(), String.CASE_INSENSITIVE_ORDER));
        return matches.size() <= 8 ? matches : matches.subList(0, 8);
    }

    public boolean shouldSearchGraph(String query, List<GraphEntityMatch> matches) {
        if (containsGraphKeyword(query)) {
            return true;
        }
        return matches.stream().filter(GraphEntityMatch::isStrong).count() >= 2;
    }

    private GraphEntityMatch bestMatch(
            KnowledgeGraphEntity entity,
            List<KnowledgeGraphEntityAlias> aliases,
            String normalizedQuery,
            List<String> fragments) {
        String bestMatchedText = null;
        double bestScore = scoreCandidate(normalizedQuery, fragments, entity.getNormalizedName());
        if (bestScore > 0) {
            bestMatchedText = entity.getName();
        }
        for (String alias : entity.getAliases()) {
            double aliasScore = scoreCandidate(normalizedQuery, fragments, normalizeText(alias));
            if (aliasScore > bestScore) {
                bestScore = aliasScore;
                bestMatchedText = alias;
            }
        }
        for (KnowledgeGraphEntityAlias alias : aliases) {
            double aliasScore = scoreCandidate(normalizedQuery, fragments, alias.getNormalizedAlias());
            if (aliasScore > bestScore) {
                bestScore = aliasScore;
                bestMatchedText = alias.getAlias();
            }
        }
        if (bestScore < MIN_SCORE) {
            return null;
        }
        return new GraphEntityMatch(entity, bestScore, bestMatchedText == null ? entity.getName() : bestMatchedText);
    }

    private double scoreCandidate(String normalizedQuery, List<String> fragments, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return 0.0d;
        }
        String normalizedCandidate = normalizeText(candidate);
        if (normalizedQuery.equals(normalizedCandidate)) {
            return 1.0d;
        }
        if (normalizedQuery.contains(normalizedCandidate) || normalizedCandidate.contains(normalizedQuery)) {
            return 0.97d;
        }
        double best = 0.0d;
        for (String fragment : fragments) {
            if (fragment.equals(normalizedCandidate)) {
                best = Math.max(best, 0.95d);
            }
            if (fragment.contains(normalizedCandidate) || normalizedCandidate.contains(fragment)) {
                best = Math.max(best, 0.88d);
            }
        }
        return best;
    }

    private Map<UUID, List<KnowledgeGraphEntityAlias>> groupAliases(List<KnowledgeGraphEntityAlias> aliases) {
        Map<UUID, List<KnowledgeGraphEntityAlias>> grouped = new LinkedHashMap<>();
        for (KnowledgeGraphEntityAlias alias : aliases) {
            grouped.computeIfAbsent(alias.getEntityId(), ignored -> new ArrayList<>()).add(alias);
        }
        return grouped;
    }

    private List<String> queryFragments(String normalizedQuery) {
        LinkedHashSet<String> fragments = new LinkedHashSet<>();
        if (!normalizedQuery.isBlank()) {
            fragments.add(normalizedQuery);
            for (String token : normalizedQuery.split("\\s+")) {
                if (token.length() >= 2) {
                    fragments.add(token);
                }
            }
        }
        return new ArrayList<>(fragments);
    }

    private boolean containsGraphKeyword(String query) {
        String normalizedQuery = normalizeText(query);
        for (String keyword : GRAPH_KEYWORDS) {
            if (normalizedQuery.contains(normalizeText(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .trim();
        return normalized.replaceAll("[\\p{Punct}\\s]+", " ");
    }
}
