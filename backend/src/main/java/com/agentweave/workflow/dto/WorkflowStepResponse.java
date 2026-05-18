package com.agentweave.workflow.dto;

import com.agentweave.workflow.domain.AgentStepEntity;
import com.agentweave.workflow.domain.AgentStepStatus;
import com.agentweave.workflow.domain.AgentStepType;
import com.agentweave.workflow.domain.AgentRole;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record WorkflowStepResponse(
        UUID stepId,
        int stepIndex,
        AgentStepType stepType,
        String nodeName,
        AgentRole agentRole,
        String traceId,
        AgentStepStatus status,
        String inputSummary,
        String outputSummary,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs,
        int retryCount,
        String retryReason,
        Instant lastRetriedAt,
        String errorCode,
        String errorMessage,
        List<WorkflowCitation> citations,
        List<WorkflowGraphPath> graphPaths,
        List<WorkflowToolCall> toolCalls
) {

    public static WorkflowStepResponse from(AgentStepEntity entity) {
        return new WorkflowStepResponse(
                entity.getId(),
                entity.getStepIndex(),
                entity.getStepType(),
                entity.getNodeName(),
                entity.getAgentRole(),
                entity.getTraceId(),
                entity.getStatus(),
                entity.getInputSummary(),
                entity.getOutputSummary(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getDurationMs(),
                entity.getRetryCount(),
                entity.getRetryReason(),
                entity.getLastRetriedAt(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getCitations().stream()
                        .map(WorkflowCitation::from)
                        .toList(),
                entity.getGraphPaths().stream()
                        .map(WorkflowGraphPath::from)
                        .toList(),
                entity.getToolCalls().stream()
                        .map(WorkflowToolCall::from)
                        .toList());
    }

    public record WorkflowCitation(
            String documentId,
            String documentName,
            String chunkId,
            String title,
            String source,
            String snippet,
            Double score
    ) {
        public static WorkflowCitation from(WorkflowReviewResult.Citation citation) {
            return new WorkflowCitation(
                    citation.documentId(),
                    null,
                    citation.chunkId(),
                    citation.source(),
                    citation.source(),
                    citation.content(),
                    citation.score());
        }
    }

    public record WorkflowGraphPath(
            String pathId,
            int depth,
            List<String> entities,
            List<String> relationships,
            List<String> sourceChunkIds,
            Double confidence
    ) {
        public static WorkflowGraphPath from(WorkflowReviewResult.GraphPath graphPath) {
            List<String> entities = graphPath.nodeIds() == null ? List.of() : graphPath.nodeIds();
            List<String> relationships = graphPath.relationshipTypes() == null ? List.of() : graphPath.relationshipTypes();
            return new WorkflowGraphPath(
                    graphPath.description(),
                    relationships.size(),
                    entities,
                    relationships,
                    List.of(),
                    null);
        }
    }

    public record WorkflowToolCall(
            String toolCode,
            String status,
            String inputSummary,
            String resultSummary,
            Long latencyMs,
            String traceId
    ) {
        public static WorkflowToolCall from(WorkflowReviewResult.ToolCallResult toolCall) {
            return new WorkflowToolCall(
                    toolCall.toolCode(),
                    toolCall.success() ? "success" : "failed",
                    summarize(toolCall.arguments()),
                    summarize(toolCall.result()),
                    null,
                    null);
        }

        private static String summarize(Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof Map<?, ?> map && map.isEmpty()) {
                return null;
            }
            return String.valueOf(value);
        }
    }
}
