package com.agentweave.tool.api;

import com.agentweave.tool.application.ToolInvocationService;
import com.agentweave.tool.dto.ToolInvocationListResponse;
import com.agentweave.tool.dto.ToolInvocationQueryRequest;
import com.agentweave.tool.dto.ToolInvocationResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tools/invocations")
public class ToolInvocationController {

    private final ToolInvocationService toolInvocationService;

    public ToolInvocationController(ToolInvocationService toolInvocationService) {
        this.toolInvocationService = toolInvocationService;
    }

    @GetMapping
    public ToolInvocationListResponse list(@Valid ToolInvocationQueryRequest request) {
        return toolInvocationService.list(request);
    }

    @GetMapping("/{invocationId}")
    public ToolInvocationResponse get(@PathVariable UUID invocationId) {
        return toolInvocationService.get(invocationId);
    }
}
