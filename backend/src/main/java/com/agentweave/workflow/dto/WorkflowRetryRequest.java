package com.agentweave.workflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record WorkflowRetryRequest(
        @Min(0)
        Integer fromStepIndex,

        @Size(max = 500)
        String reason
) {
}
