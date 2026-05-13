package com.agentweave.conversation.application;

import com.agentweave.conversation.dto.CitationEventResponse;
import com.agentweave.conversation.dto.SseEventPayload;
import com.agentweave.conversation.dto.WorkflowStepEventResponse;
import com.agentweave.graphrag.dto.GraphPathResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;

@Component
public class SseEventFactory {

    public ServerSentEvent<SseEventPayload> messageDelta(
            UUID conversationId,
            UUID messageId,
            String delta,
            String traceId) {
        return ServerSentEvent.builder(base(conversationId, messageId, traceId, null)
                        .delta(delta)
                        .build())
                .event("message_delta")
                .build();
    }

    public ServerSentEvent<SseEventPayload> toolCallStarted(
            UUID conversationId,
            UUID messageId,
            String toolCallId,
            String toolName,
            String inputSummary,
            String traceId) {
        return ServerSentEvent.builder(base(conversationId, messageId, traceId, null)
                        .toolCallId(toolCallId)
                        .toolName(toolName)
                        .inputSummary(inputSummary)
                        .build())
                .event("tool_call_started")
                .build();
    }

    public ServerSentEvent<SseEventPayload> toolCallFinished(
            UUID conversationId,
            UUID messageId,
            String toolCallId,
            String status,
            Long latencyMs,
            String resultSummary,
            String traceId) {
        return ServerSentEvent.builder(base(conversationId, messageId, traceId, null)
                        .toolCallId(toolCallId)
                        .status(status)
                        .latencyMs(latencyMs)
                        .resultSummary(resultSummary)
                        .build())
                .event("tool_call_finished")
                .build();
    }

    public ServerSentEvent<SseEventPayload> citation(
            UUID conversationId,
            UUID messageId,
            CitationEventResponse citation,
            String traceId) {
        return ServerSentEvent.builder(base(conversationId, messageId, traceId, null)
                        .documentId(citation.documentId())
                        .documentName(citation.documentName())
                        .chunkId(citation.chunkId())
                        .title(citation.title())
                        .source(citation.source())
                        .snippet(citation.snippet())
                        .score(citation.score())
                        .businessDomain(citation.businessDomain())
                        .documentType(citation.documentType())
                        .permissionLevel(citation.permissionLevel())
                        .build())
                .event("citation")
                .build();
    }

    public ServerSentEvent<SseEventPayload> graphPath(
            UUID conversationId,
            UUID messageId,
            GraphPathResponse graphPath,
            String traceId) {
        return ServerSentEvent.builder(base(conversationId, messageId, traceId, null)
                        .graphPath(graphPath)
                        .build())
                .event("graph_path")
                .build();
    }

    public ServerSentEvent<SseEventPayload> workflowStep(
            UUID conversationId,
            UUID messageId,
            WorkflowStepEventResponse step,
            String traceId) {
        return ServerSentEvent.builder(base(conversationId, messageId, traceId, null)
                        .workflowRunId(step.workflowRunId())
                        .stepName(step.stepName())
                        .status(step.status())
                        .build())
                .event("workflow_step")
                .build();
    }

    public ServerSentEvent<SseEventPayload> done(UUID conversationId, UUID messageId, String traceId) {
        return ServerSentEvent.builder(base(conversationId, messageId, traceId, null)
                        .status("SUCCEEDED")
                        .build())
                .event("done")
                .build();
    }

    public ServerSentEvent<SseEventPayload> error(
            UUID conversationId,
            UUID messageId,
            String code,
            String message,
            String traceId) {
        return ServerSentEvent.builder(base(conversationId, messageId, traceId, null)
                        .code(code)
                        .message(message)
                        .build())
                .event("error")
                .build();
    }

    private SseEventPayloadBuilder base(
            UUID conversationId,
            UUID messageId,
            String traceId,
            String eventId) {
        return new SseEventPayloadBuilder()
                .eventId(eventId == null ? "evt-" + UUID.randomUUID() : eventId)
                .conversationId(conversationId)
                .messageId(messageId)
                .traceId(traceId)
                .timestamp(Instant.now());
    }

    private static class SseEventPayloadBuilder {

        private String eventId;
        private UUID conversationId;
        private UUID messageId;
        private String traceId;
        private Instant timestamp;
        private Instant createdAt;
        private String delta;
        private String toolCallId;
        private String toolName;
        private String inputSummary;
        private String status;
        private Long latencyMs;
        private String resultSummary;
        private String documentId;
        private String documentName;
        private String chunkId;
        private String title;
        private String source;
        private String snippet;
        private Double score;
        private String businessDomain;
        private String documentType;
        private String permissionLevel;
        private String workflowRunId;
        private String stepName;
        private String code;
        private String message;
        private GraphPathResponse graphPath;

        SseEventPayloadBuilder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        SseEventPayloadBuilder conversationId(UUID conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        SseEventPayloadBuilder messageId(UUID messageId) {
            this.messageId = messageId;
            return this;
        }

        SseEventPayloadBuilder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        SseEventPayloadBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            this.createdAt = timestamp;
            return this;
        }

        SseEventPayloadBuilder delta(String delta) {
            this.delta = delta;
            return this;
        }

        SseEventPayloadBuilder toolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
            return this;
        }

        SseEventPayloadBuilder toolName(String toolName) {
            this.toolName = toolName;
            return this;
        }

        SseEventPayloadBuilder inputSummary(String inputSummary) {
            this.inputSummary = inputSummary;
            return this;
        }

        SseEventPayloadBuilder status(String status) {
            this.status = status;
            return this;
        }

        SseEventPayloadBuilder latencyMs(Long latencyMs) {
            this.latencyMs = latencyMs;
            return this;
        }

        SseEventPayloadBuilder resultSummary(String resultSummary) {
            this.resultSummary = resultSummary;
            return this;
        }

        SseEventPayloadBuilder documentId(String documentId) {
            this.documentId = documentId;
            return this;
        }

        SseEventPayloadBuilder documentName(String documentName) {
            this.documentName = documentName;
            return this;
        }

        SseEventPayloadBuilder chunkId(String chunkId) {
            this.chunkId = chunkId;
            return this;
        }

        SseEventPayloadBuilder title(String title) {
            this.title = title;
            return this;
        }

        SseEventPayloadBuilder source(String source) {
            this.source = source;
            return this;
        }

        SseEventPayloadBuilder snippet(String snippet) {
            this.snippet = snippet;
            return this;
        }

        SseEventPayloadBuilder score(Double score) {
            this.score = score;
            return this;
        }

        SseEventPayloadBuilder businessDomain(String businessDomain) {
            this.businessDomain = businessDomain;
            return this;
        }

        SseEventPayloadBuilder documentType(String documentType) {
            this.documentType = documentType;
            return this;
        }

        SseEventPayloadBuilder permissionLevel(String permissionLevel) {
            this.permissionLevel = permissionLevel;
            return this;
        }

        SseEventPayloadBuilder workflowRunId(String workflowRunId) {
            this.workflowRunId = workflowRunId;
            return this;
        }

        SseEventPayloadBuilder stepName(String stepName) {
            this.stepName = stepName;
            return this;
        }

        SseEventPayloadBuilder code(String code) {
            this.code = code;
            return this;
        }

        SseEventPayloadBuilder message(String message) {
            this.message = message;
            return this;
        }

        SseEventPayloadBuilder graphPath(GraphPathResponse graphPath) {
            this.graphPath = graphPath;
            return this;
        }

        SseEventPayload build() {
            return new SseEventPayload(
                    eventId,
                    conversationId,
                    messageId,
                    traceId,
                    timestamp,
                    createdAt,
                    delta,
                    toolCallId,
                    toolName,
                    inputSummary,
                    status,
                    latencyMs,
                    resultSummary,
                    documentId,
                    documentName,
                    chunkId,
                    title,
                    source,
                    snippet,
                    score,
                    businessDomain,
                    documentType,
                    permissionLevel,
                    workflowRunId,
                    stepName,
                    code,
                    message,
                    graphPath);
        }
    }
}
