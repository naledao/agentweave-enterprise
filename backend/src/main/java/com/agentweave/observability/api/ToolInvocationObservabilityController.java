package com.agentweave.observability.api;

import com.agentweave.observability.application.ToolInvocationObservabilityQueryService;
import com.agentweave.observability.dto.ToolInvocationSummaryResponse;
import com.agentweave.tool.dto.ToolInvocationQueryRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/observability/tool-invocations")
public class ToolInvocationObservabilityController {

    private final ToolInvocationObservabilityQueryService toolInvocationObservabilityQueryService;

    public ToolInvocationObservabilityController(
            ToolInvocationObservabilityQueryService toolInvocationObservabilityQueryService) {
        this.toolInvocationObservabilityQueryService = toolInvocationObservabilityQueryService;
    }

    @GetMapping
    public ToolInvocationSummaryResponse summary(@Valid ToolInvocationQueryRequest request) {
        return toolInvocationObservabilityQueryService.summary(request);
    }
}
