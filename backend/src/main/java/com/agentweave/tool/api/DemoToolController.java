package com.agentweave.tool.api;

import com.agentweave.shared.security.RequireToolPermission;
import com.agentweave.tool.dto.ToolExecutionResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tools/demo")
public class DemoToolController {

    @GetMapping("/tickets")
    @RequireToolPermission("tool:ticket:query")
    public ToolExecutionResponse queryTicket(@RequestParam(defaultValue = "INC-10001") String ticketNo) {
        return ToolExecutionResponse.succeeded(
                "ticket-query",
                Map.of("ticketNo", ticketNo, "title", "Demo ticket", "state", "OPEN"));
    }

    @GetMapping("/logs")
    @RequireToolPermission("tool:log:search")
    public ToolExecutionResponse searchLogs(@RequestParam(defaultValue = "agentweave") String keyword) {
        return ToolExecutionResponse.succeeded(
                "log-search",
                Map.of("keyword", keyword, "matches", 3));
    }

    @GetMapping("/api-status")
    @RequireToolPermission("tool:api-status:query")
    public ToolExecutionResponse queryApiStatus(@RequestParam(defaultValue = "knowledge-service") String service) {
        return ToolExecutionResponse.succeeded(
                "api-status-query",
                Map.of("service", service, "status", "UP", "latencyMs", 42));
    }
}
