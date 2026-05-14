package com.agentweave.shared.exception;

public class ToolExecutionTimeoutException extends BusinessException {

    public ToolExecutionTimeoutException(String message) {
        super(ErrorCode.TOOL_TIMEOUT, message);
    }
}
