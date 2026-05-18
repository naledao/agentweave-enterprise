package com.agentweave.observability.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SseConnectionTrackerTest {

    @Test
    void tracksCompletedConnectionCountsAndFirstTokenMetrics() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        SseConnectionTracker tracker = new SseConnectionTracker(new AgentWeaveMetrics(meterRegistry));
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        SseConnectionTracker.SseConnectionScope scope = tracker.start(
                userId,
                conversationId,
                messageId,
                "trace-sse",
                "/api/v1/conversations/{conversationId}/stream",
                "unknown");
        tracker.modelResolved(scope, "gpt-test");
        tracker.recordEvent(scope, "citation");
        tracker.recordEvent(scope, "message_delta");
        tracker.recordEvent(scope, "message_delta");
        tracker.complete(scope);

        assertThat(meterRegistry.find("agentweave.sse.connection.active").gauge().value()).isZero();
        assertThat(meterRegistry.find("agentweave.sse.connection.duration")
                        .tag("status", "COMPLETED")
                        .tag("modelName", "gpt-test")
                        .timer()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.sse.first_token.duration")
                        .tag("status", "STREAMING")
                        .tag("modelName", "gpt-test")
                        .timer()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.sse.event.count")
                        .tag("eventType", "message_delta")
                        .counter()
                        .count())
                .isEqualTo(2);
    }

    @Test
    void treatsClientDisconnectAsConnectionFailure() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        SseConnectionTracker tracker = new SseConnectionTracker(new AgentWeaveMetrics(meterRegistry));

        SseConnectionTracker.SseConnectionScope scope = tracker.start(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "trace-disconnect",
                "/api/v1/conversations/{conversationId}/stream",
                "unknown");
        tracker.clientDisconnected(scope);

        assertThat(meterRegistry.find("agentweave.sse.connection.failures")
                        .tag("status", "CLIENT_DISCONNECTED")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.sse.connection.active").gauge().value()).isZero();
    }
}
