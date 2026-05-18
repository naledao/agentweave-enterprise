package com.agentweave.shared.tracing;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class CorrelationContext {

    public static final String CONVERSATION_ID_KEY = "conversationId";
    public static final String MESSAGE_ID_KEY = "messageId";
    public static final String WORKFLOW_RUN_ID_KEY = "workflowRunId";
    public static final String WORKFLOW_STEP_ID_KEY = "workflowStepId";

    public Scope open(String traceId, UUID conversationId, UUID messageId) {
        return new Scope(traceId, conversationId, messageId, null, null);
    }

    public Scope openWorkflow(String traceId, UUID workflowRunId, UUID workflowStepId) {
        return new Scope(traceId, null, null, workflowRunId, workflowStepId);
    }

    public Scope openWorkflow(
            String traceId,
            UUID conversationId,
            UUID messageId,
            UUID workflowRunId,
            UUID workflowStepId) {
        return new Scope(traceId, conversationId, messageId, workflowRunId, workflowStepId);
    }

    public Scope open(TraceContext traceContext) {
        return new Scope(
                traceContext.traceId(),
                traceContext.conversationId(),
                traceContext.messageId(),
                traceContext.workflowRunId(),
                traceContext.workflowStepId());
    }

    public Optional<TraceContext> current() {
        return TraceContext.fromMdc();
    }

    public static final class Scope implements AutoCloseable {

        private final String previousTraceId;
        private final String previousConversationId;
        private final String previousMessageId;
        private final String previousWorkflowRunId;
        private final String previousWorkflowStepId;

        private Scope(String traceId, UUID conversationId, UUID messageId, UUID workflowRunId, UUID workflowStepId) {
            this.previousTraceId = MDC.get(TraceIdProvider.TRACE_ID_KEY);
            this.previousConversationId = MDC.get(CONVERSATION_ID_KEY);
            this.previousMessageId = MDC.get(MESSAGE_ID_KEY);
            this.previousWorkflowRunId = MDC.get(WORKFLOW_RUN_ID_KEY);
            this.previousWorkflowStepId = MDC.get(WORKFLOW_STEP_ID_KEY);

            putOrRemove(TraceIdProvider.TRACE_ID_KEY, traceId);
            putOrRemove(CONVERSATION_ID_KEY, conversationId == null ? null : conversationId.toString());
            putOrRemove(MESSAGE_ID_KEY, messageId == null ? null : messageId.toString());
            putOrRemove(WORKFLOW_RUN_ID_KEY, workflowRunId == null ? null : workflowRunId.toString());
            putOrRemove(WORKFLOW_STEP_ID_KEY, workflowStepId == null ? null : workflowStepId.toString());
        }

        @Override
        public void close() {
            putOrRemove(TraceIdProvider.TRACE_ID_KEY, previousTraceId);
            putOrRemove(CONVERSATION_ID_KEY, previousConversationId);
            putOrRemove(MESSAGE_ID_KEY, previousMessageId);
            putOrRemove(WORKFLOW_RUN_ID_KEY, previousWorkflowRunId);
            putOrRemove(WORKFLOW_STEP_ID_KEY, previousWorkflowStepId);
        }

        private static void putOrRemove(String key, String value) {
            if (value == null || value.isBlank()) {
                MDC.remove(key);
                return;
            }
            MDC.put(key, value);
        }
    }
}
