package com.agentweave.conversation.application;

public record StreamTermination(
        StreamTerminationType type,
        String code,
        String message) {

    public static StreamTermination cancelled(String message) {
        return new StreamTermination(StreamTerminationType.CANCELLED, "CHAT_ASSISTANT_CANCELLED", message);
    }

    public static StreamTermination timeout(String message) {
        return new StreamTermination(StreamTerminationType.TIMEOUT, "CHAT_STREAM_TIMEOUT", message);
    }
}
