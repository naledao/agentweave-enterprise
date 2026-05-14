package com.agentweave.langchain4j.tool;

import com.agentweave.shared.security.RequireToolPermission;
import com.agentweave.tool.log.LogQueryRequest;
import com.agentweave.tool.log.LogQueryResult;
import com.agentweave.tool.log.LogQueryService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.validation.Valid;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
public class LogTools {

    private final LogQueryService logQueryService;

    public LogTools(LogQueryService logQueryService) {
        this.logQueryService = logQueryService;
    }

    @Tool(name = "query_logs", value = "Search recent service logs by service name, keyword, and time range.")
    @RequireToolPermission("tool:log:search")
    public LogQueryResult queryLogs(
            @P(value = "Log query containing serviceName, keyword, timeRange, and optional limit.", required = true)
            @Valid LogQueryRequest request) {
        return logQueryService.query(request);
    }
}
