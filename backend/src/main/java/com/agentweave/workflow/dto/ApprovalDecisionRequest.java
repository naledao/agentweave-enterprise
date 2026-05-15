package com.agentweave.workflow.dto;

import jakarta.validation.constraints.Size;

public record ApprovalDecisionRequest(
        @Size(max = 500) String reason
) {
}
