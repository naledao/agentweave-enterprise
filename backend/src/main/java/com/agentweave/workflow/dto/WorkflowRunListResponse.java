package com.agentweave.workflow.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record WorkflowRunListResponse(
        List<WorkflowRunListItemResponse> items,
        int page,
        int size,
        long total,
        int totalPages) {

    public static WorkflowRunListResponse from(Page<WorkflowRunListItemResponse> runs) {
        return new WorkflowRunListResponse(
                runs.getContent(),
                runs.getNumber(),
                runs.getSize(),
                runs.getTotalElements(),
                runs.getTotalPages());
    }
}
