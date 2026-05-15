package com.agentweave.shared.exception;

public enum ErrorCode {

    UNAUTHORIZED("AUTH_401", "Unauthorized"),
    ACCESS_DENIED("AUTH_403", "Access denied"),
    BAD_CREDENTIALS("AUTH_001", "Username or password is incorrect"),
    USER_DISABLED("AUTH_002", "User is disabled"),
    RESOURCE_NOT_FOUND("COMMON_404", "Resource not found"),
    VALIDATION_FAILED("COMMON_400", "Validation failed"),
    TOO_MANY_REQUESTS("COMMON_429", "Too many requests"),
    TOOL_TIMEOUT("TOOL_408", "Tool execution timeout"),
    WORKFLOW_CHECKPOINT_INVALID("WORKFLOW_400", "Workflow checkpoint is invalid"),
    WORKFLOW_RECOVERY_NOT_ALLOWED("WORKFLOW_409", "Workflow recovery is not allowed"),
    BUSINESS_ERROR("COMMON_001", "Business error");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
