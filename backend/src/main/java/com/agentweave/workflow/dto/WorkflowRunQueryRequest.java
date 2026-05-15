package com.agentweave.workflow.dto;

import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import com.agentweave.workflow.domain.WorkflowRunStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record WorkflowRunQueryRequest(
        @Min(0)
        Integer page,

        @Min(1)
        @Max(100)
        Integer size,

        @Size(max = 40)
        String status) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    public int pageNumber() {
        return page == null ? DEFAULT_PAGE : page;
    }

    public int pageSize() {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    public WorkflowRunStatus normalizedStatus() {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        try {
            return WorkflowRunStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "invalid workflow run status");
        }
    }
}
