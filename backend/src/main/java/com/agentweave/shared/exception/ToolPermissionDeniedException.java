package com.agentweave.shared.exception;

public class ToolPermissionDeniedException extends AccessDeniedBusinessException {

    public ToolPermissionDeniedException(String message) {
        super(message);
    }
}
