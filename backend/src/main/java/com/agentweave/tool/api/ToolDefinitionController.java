package com.agentweave.tool.api;

import com.agentweave.tool.application.ToolDefinitionService;
import com.agentweave.tool.dto.ToolDefinitionResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tools")
public class ToolDefinitionController {

    private final ToolDefinitionService toolDefinitionService;

    public ToolDefinitionController(ToolDefinitionService toolDefinitionService) {
        this.toolDefinitionService = toolDefinitionService;
    }

    @GetMapping
    public List<ToolDefinitionResponse> listTools() {
        return toolDefinitionService.listTools();
    }
}
