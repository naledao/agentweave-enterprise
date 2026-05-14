package com.agentweave.langchain4j.tool;

import com.agentweave.shared.security.RequireToolPermission;
import com.agentweave.tool.endpoint.EndpointStatusRequest;
import com.agentweave.tool.endpoint.EndpointStatusResult;
import com.agentweave.tool.endpoint.EndpointStatusService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.validation.Valid;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
public class EndpointTools {

    private final EndpointStatusService endpointStatusService;

    public EndpointTools(EndpointStatusService endpointStatusService) {
        this.endpointStatusService = endpointStatusService;
    }

    @Tool(name = "query_endpoint_status", value = "Query status metrics for a registered internal endpoint.")
    @RequireToolPermission("tool:api-status:query")
    public EndpointStatusResult queryEndpointStatus(
            @P(value = "Registered endpoint path, endpoint code, or internal service endpoint.", required = true)
            @Valid EndpointStatusRequest request) {
        return endpointStatusService.query(request);
    }
}
