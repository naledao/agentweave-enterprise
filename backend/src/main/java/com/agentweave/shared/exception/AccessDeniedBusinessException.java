package com.agentweave.shared.exception;

public class AccessDeniedBusinessException extends BusinessException {

    public AccessDeniedBusinessException() {
        super(ErrorCode.ACCESS_DENIED);
    }

    public AccessDeniedBusinessException(String message) {
        super(ErrorCode.ACCESS_DENIED, message);
    }
}
