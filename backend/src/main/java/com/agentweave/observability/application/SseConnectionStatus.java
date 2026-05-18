package com.agentweave.observability.application;

public enum SseConnectionStatus {
    CONNECTING,
    STREAMING,
    TOOL_CALLING,
    COMPLETED,
    FAILED,
    TIMEOUT,
    CANCELLED,
    CLIENT_DISCONNECTED
}
