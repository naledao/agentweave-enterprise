package com.agentweave.tool.api;

import com.agentweave.shared.security.RequireToolPermission;
import com.agentweave.tool.dto.ToolExecutionResponse;
import com.agentweave.tool.endpoint.EndpointStatusRequest;
import com.agentweave.tool.endpoint.EndpointStatusResult;
import com.agentweave.tool.endpoint.EndpointStatusService;
import com.agentweave.tool.ticket.TicketQueryRequest;
import com.agentweave.tool.ticket.TicketQueryResult;
import com.agentweave.tool.ticket.TicketQueryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    private final TicketQueryService ticketQueryService;
    private final EndpointStatusService endpointStatusService;

    public DemoToolController(
            TicketQueryService ticketQueryService,
            EndpointStatusService endpointStatusService) {
        this.ticketQueryService = ticketQueryService;
        this.endpointStatusService = endpointStatusService;
    }

    @GetMapping("/tickets")
    @RequireToolPermission("tool:ticket:query")
    public ToolExecutionResponse queryTicket(
            @RequestParam(defaultValue = "INC-10001")
                    @NotBlank
                    @Size(max = 64)
                    @Pattern(regexp = "^INC-\\d{5}$", message = "must match INC-00000 format")
                    String ticketNo) {
        TicketQueryResult result = ticketQueryService.query(new TicketQueryRequest(ticketNo));
        return ToolExecutionResponse.succeeded(
                "ticket-query",
                Map.of(
                        "ticketNo", result.ticketNo(),
                        "title", result.title(),
                        "status", result.status(),
                        "priority", result.priority(),
                        "assignee", result.assignee(),
                        "updatedAt", result.updatedAt()));
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
            @RequestParam(defaultValue = "knowledge-service")
                    @NotBlank
                    @Size(max = 160)
                    @Pattern(
                            regexp = "^[A-Za-z0-9._:/-]+$",
                            message = "must contain only endpoint identifier characters")
                    String endpoint) {
        EndpointStatusResult result = endpointStatusService.query(new EndpointStatusRequest(endpoint));
        return ToolExecutionResponse.succeeded(
                "api-status-query",
                Map.of(
                        "endpoint", result.endpoint(),
                        "httpStatus", result.httpStatus(),
                        "averageLatencyMs", result.averageLatencyMs(),
                        "failureRate", result.failureRate(),
                        "checkedAt", result.checkedAt()));
    }

    @GetMapping("/slow-ticket")
    @RequireToolPermission("tool:ticket:query")
    public ToolExecutionResponse querySlowTicket(
            @RequestParam(defaultValue = "INC-10001")
                    @NotBlank
                    @Size(max = 64)
                    @Pattern(regexp = "^INC-\\d{5}$", message = "must match INC-00000 format")
                    String ticketNo,
            @RequestParam(defaultValue = "0") long delayMs) throws InterruptedException {
        if (delayMs > 0) {
            Thread.sleep(delayMs);
        }
        TicketQueryResult result = ticketQueryService.query(new TicketQueryRequest(ticketNo));
        return ToolExecutionResponse.succeeded(
                "slow-ticket-query",
                Map.of(
                        "ticketNo", result.ticketNo(),
                        "title", result.title(),
                        "status", result.status(),
                        "priority", result.priority(),
                        "assignee", result.assignee(),
                        "updatedAt", result.updatedAt()));
    }

    @GetMapping("/failing-ticket")
    @RequireToolPermission("tool:ticket:query")
    public ToolExecutionResponse queryFailingTicket(
            @RequestParam(defaultValue = "INC-10001") @NotBlank @Size(max = 64) String ticketNo) {
        throw new IllegalStateException("demo ticket tool failure: " + ticketNo);
    }
}
