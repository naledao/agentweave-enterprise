package com.agentweave.shared.tracing;

import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class CorrelationContext {

    public static final String CONVERSATION_ID_KEY = "conversationId";
    public static final String MESSAGE_ID_KEY = "messageId";

    public Scope open(String traceId, UUID conversationId, UUID messageId) {
        return new Scope(traceId, conversationId, messageId);
    }

    public static final class Scope implements AutoCloseable {

        private final String previousTraceId;
        private final String previousConversationId;
        private final String previousMessageId;

        private Scope(String traceId, UUID conversationId, UUID messageId) {
            this.previousTraceId = MDC.get(TraceIdProvider.TRACE_ID_KEY);
            this.previousConversationId = MDC.get(CONVERSATION_ID_KEY);
            this.previousMessageId = MDC.get(MESSAGE_ID_KEY);

            putOrRemove(TraceIdProvider.TRACE_ID_KEY, traceId);
            putOrRemove(CONVERSATION_ID_KEY, conversationId == null ? null : conversationId.toString());
            putOrRemove(MESSAGE_ID_KEY, messageId == null ? null : messageId.toString());
        }

        @Override
        public void close() {
            putOrRemove(TraceIdProvider.TRACE_ID_KEY, previousTraceId);
            putOrRemove(CONVERSATION_ID_KEY, previousConversationId);
            putOrRemove(MESSAGE_ID_KEY, previousMessageId);
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
