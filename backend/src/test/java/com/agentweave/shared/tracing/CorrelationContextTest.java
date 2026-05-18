package com.agentweave.shared.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CorrelationContextTest {

    private final CorrelationContext correlationContext = new CorrelationContext();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldOpenAndRestoreCorrelationScope() {
        MDC.put(TraceIdProvider.TRACE_ID_KEY, "previous-trace");
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        try (CorrelationContext.Scope ignored = correlationContext.open("next-trace", conversationId, messageId)) {
            assertThat(MDC.get(TraceIdProvider.TRACE_ID_KEY)).isEqualTo("next-trace");
            assertThat(MDC.get(CorrelationContext.CONVERSATION_ID_KEY)).isEqualTo(conversationId.toString());
            assertThat(MDC.get(CorrelationContext.MESSAGE_ID_KEY)).isEqualTo(messageId.toString());
            assertThat(correlationContext.current()).contains(TraceContext.of("next-trace", conversationId, messageId));
        }

        assertThat(MDC.get(TraceIdProvider.TRACE_ID_KEY)).isEqualTo("previous-trace");
        assertThat(MDC.get(CorrelationContext.CONVERSATION_ID_KEY)).isNull();
        assertThat(MDC.get(CorrelationContext.MESSAGE_ID_KEY)).isNull();
    }

    @Test
    void shouldOpenScopeFromTraceContext() {
        TraceContext traceContext = TraceContext.of("context-trace", UUID.randomUUID(), UUID.randomUUID());

        try (CorrelationContext.Scope ignored = correlationContext.open(traceContext)) {
            assertThat(correlationContext.current()).contains(traceContext);
        }
    }

    @Test
    void shouldOpenScopeFromWorkflowTraceContext() {
        TraceContext traceContext = new TraceContext(
                "workflow-context-trace",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID());

        try (CorrelationContext.Scope ignored = correlationContext.open(traceContext)) {
            assertThat(correlationContext.current()).contains(traceContext);
        }
    }

    @Test
    void shouldExposeWorkflowCorrelationInCurrentTraceContext() {
        UUID workflowRunId = UUID.randomUUID();
        UUID workflowStepId = UUID.randomUUID();

        try (CorrelationContext.Scope ignored =
                correlationContext.openWorkflow("workflow-trace", workflowRunId, workflowStepId)) {
            assertThat(correlationContext.current())
                    .contains(TraceContext.ofWorkflow("workflow-trace", workflowRunId, workflowStepId));
        }
    }

    @Test
    void shouldExposeConversationAndWorkflowCorrelationTogether() {
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID workflowRunId = UUID.randomUUID();
        UUID workflowStepId = UUID.randomUUID();

        try (CorrelationContext.Scope ignored = correlationContext.openWorkflow(
                "workflow-trace",
                conversationId,
                messageId,
                workflowRunId,
                workflowStepId)) {
            assertThat(correlationContext.current())
                    .contains(new TraceContext(
                            "workflow-trace",
                            conversationId,
                            messageId,
                            workflowRunId,
                            workflowStepId));
        }
    }
}
