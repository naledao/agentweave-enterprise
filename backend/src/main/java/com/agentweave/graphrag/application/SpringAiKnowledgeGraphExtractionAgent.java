package com.agentweave.graphrag.application;

import com.agentweave.graphrag.dto.GraphRagEntityCandidate;
import com.agentweave.graphrag.dto.GraphRagExtractionCommand;
import com.agentweave.graphrag.dto.GraphRagExtractionResult;
import com.agentweave.graphrag.dto.GraphRagRelationshipCandidate;
import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class SpringAiKnowledgeGraphExtractionAgent implements KnowledgeGraphExtractionAgent {

    private static final String SYSTEM_PROMPT = """
            你是 AgentWeave Enterprise 的企业知识图谱抽取器。
            任务：从单个文档 chunk 中抽取候选实体和实体关系。
            要求：
            1. 只输出 JSON，不要输出解释、Markdown 或代码块。
            2. 不要编造 chunk 中不存在的信息。
            3. 实体类型仅允许：SERVICE, API, DATABASE, ERROR_CODE, TICKET, PRODUCT, MODULE, PERSON, TEAM, DOCUMENT, CONCEPT。
            4. 关系类型仅允许：DEPENDS_ON, CALLS, OWNS, BELONGS_TO, MENTIONS, CAUSES, RESOLVES, RELATED_TO, AFFECTS, DOCUMENTED_BY。
            5. 每个关系必须包含 sourceName、sourceType、targetName、targetType、type、description、confidence。
            6. 输出结构必须是：
               {"entities":[...],"relationships":[...]}
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public SpringAiKnowledgeGraphExtractionAgent(
            ChatClient.Builder chatClientBuilder,
            ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public GraphRagExtractionResult extract(GraphRagExtractionCommand command) {
        String prompt = buildPrompt(command);
        String content = chatClient.prompt()
                .user(prompt)
                .call()
                .chatResponse()
                .getResult()
                .getOutput()
                .getText();
        return parse(content);
    }

    private String buildPrompt(GraphRagExtractionCommand command) {
        return """
                文档ID：%s
                ChunkID：%s
                业务域：%s
                文档类型：%s
                权限级别：%s

                请分析下面的 chunk 内容，并返回符合约束的 JSON：

                %s
                """.formatted(
                command.documentId(),
                command.chunkId(),
                command.businessDomain(),
                command.documentType(),
                command.permissionLevel(),
                command.chunkContent());
    }

    private GraphRagExtractionResult parse(String content) {
        try {
            String json = extractJson(content);
            ExtractionPayload payload = objectMapper.readValue(json, ExtractionPayload.class);
            List<GraphRagEntityCandidate> entities = payload.entities() == null
                    ? List.of()
                    : payload.entities().stream()
                            .map(item -> new GraphRagEntityCandidate(
                                    item.name(),
                                    item.type(),
                                    item.description(),
                                    item.aliases(),
                                    confidence(item.confidence())))
                            .toList();
            List<GraphRagRelationshipCandidate> relationships = payload.relationships() == null
                    ? List.of()
                    : payload.relationships().stream()
                            .map(item -> new GraphRagRelationshipCandidate(
                                    item.sourceName(),
                                    item.sourceType(),
                                    item.targetName(),
                                    item.targetType(),
                                    item.type(),
                                    item.description(),
                                    confidence(item.confidence())))
                            .toList();
            return new GraphRagExtractionResult(entities, relationships);
        } catch (Exception ex) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "failed to parse graph extraction result");
        }
    }

    private String extractJson(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "graph extraction returned empty content");
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "graph extraction result must be JSON");
        }
        return trimmed.substring(start, end + 1);
    }

    private double confidence(Double confidence) {
        if (confidence == null || confidence.isNaN() || confidence.isInfinite()) {
            return 0.5d;
        }
        return Math.max(0.0d, Math.min(1.0d, confidence));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ExtractionPayload(
            List<EntityPayload> entities,
            List<RelationshipPayload> relationships) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EntityPayload(
            String name,
            String type,
            String description,
            List<String> aliases,
            Double confidence) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RelationshipPayload(
            String sourceName,
            String sourceType,
            String targetName,
            String targetType,
            String type,
            String description,
            Double confidence) {
    }
}
