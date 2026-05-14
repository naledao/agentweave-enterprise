package com.agentweave.tool.api;

import com.agentweave.shared.security.RequireToolPermission;
import com.agentweave.tool.dto.ToolExecutionResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/v1/tools/demo")
public class DemoToolController {

    @GetMapping("/tickets")
    @RequireToolPermission("tool:ticket:query")
    public ToolExecutionResponse queryTicket(
            @RequestParam(defaultValue = "INC-10001") @NotBlank @Size(max = 64) String ticketNo) {
        return ToolExecutionResponse.succeeded(
                "ticket-query",
                Map.of("ticketNo", ticketNo, "title", "Demo ticket", "state", "OPEN"));
    }

    @GetMapping("/logs")
    @RequireToolPermission("tool:log:search")
    public ToolExecutionResponse searchLogs(
            @RequestParam(defaultValue = "agentweave") @NotBlank @Size(max = 120) String keyword) {
        return ToolExecutionResponse.succeeded(
                "log-search",
                Map.of("keyword", keyword, "matches", 3));
    }

    @GetMapping("/api-status")
    @RequireToolPermission("tool:api-status:query")
    public ToolExecutionResponse queryApiStatus(
            @RequestParam(defaultValue = "knowledge-service") @NotBlank @Size(max = 120) String service) {
        return ToolExecutionResponse.succeeded(
                "api-status-query",
                Map.of("service", service, "status", "UP", "latencyMs", 42));
    }

    @GetMapping("/slow-ticket")
    @RequireToolPermission("tool:ticket:query")
    public ToolExecutionResponse querySlowTicket(
            @RequestParam(defaultValue = "INC-10001") @NotBlank @Size(max = 64) String ticketNo,
            @RequestParam(defaultValue = "0") long delayMs) throws InterruptedException {
        if (delayMs > 0) {
            Thread.sleep(delayMs);
        }
        return ToolExecutionResponse.succeeded(
                "slow-ticket-query",
                Map.of("ticketNo", ticketNo, "title", "Slow demo ticket", "state", "OPEN"));
    }

    @GetMapping("/failing-ticket")
    @RequireToolPermission("tool:ticket:query")
    public ToolExecutionResponse queryFailingTicket(
            @RequestParam(defaultValue = "INC-10001") @NotBlank @Size(max = 64) String ticketNo) {
        throw new IllegalStateException("demo ticket tool failure: " + ticketNo);
    }
}
