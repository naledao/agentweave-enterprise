package com.agentweave.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateWorkflowRunRequest(
        UUID conversationId,

        @NotBlank(message = "Goal is required")
        @Size(max = 5000, message = "Goal must not exceed 5000 characters")
        String goal
) {
}
