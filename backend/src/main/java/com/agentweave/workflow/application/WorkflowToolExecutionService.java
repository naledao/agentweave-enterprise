package com.agentweave.workflow.application;

import com.agentweave.langchain4j.tool.EndpointTools;
import com.agentweave.langchain4j.tool.LogTools;
import com.agentweave.langchain4j.tool.TicketTools;
import com.agentweave.shared.tracing.CorrelationContext;
import com.agentweave.tool.endpoint.EndpointStatusRequest;
import com.agentweave.tool.log.LogQueryRequest;
import com.agentweave.tool.ticket.TicketQueryRequest;
import com.agentweave.workflow.domain.AgentStepEntity;
import com.agentweave.workflow.dto.AgentExecutionResult;
import com.agentweave.workflow.dto.WorkflowPlanStep;
import com.agentweave.workflow.dto.WorkflowReviewResult;
import com.agentweave.workflow.state.AgentWorkflowState;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class WorkflowToolExecutionService {

    private static final Pattern TICKET_NO_PATTERN = Pattern.compile("INC-\\d{5}", Pattern.CASE_INSENSITIVE);
    private static final String DEFAULT_TICKET_NO = "INC-10001";
    private static final String DEFAULT_SERVICE_NAME = "agentweave";
    private static final String DEFAULT_LOG_KEYWORD = "error";
    private static final String DEFAULT_ENDPOINT = "knowledge-service";

    private final TicketTools ticketTools;
    private final LogTools logTools;
    private final EndpointTools endpointTools;
    private final CorrelationContext correlationContext;

    public WorkflowToolExecutionService(
            TicketTools ticketTools,
            LogTools logTools,
            EndpointTools endpointTools,
            CorrelationContext correlationContext) {
        this.ticketTools = ticketTools;
        this.logTools = logTools;
        this.endpointTools = endpointTools;
        this.correlationContext = correlationContext;
    }

    public AgentExecutionResult execute(AgentWorkflowState state, WorkflowPlanStep step, AgentStepEntity stepEntity) {
        try (CorrelationContext.Scope ignored = correlationContext.openWorkflow(
                state.traceId(),
                state.conversationId(),
                null,
                state.runId(),
                stepEntity.getId())) {
            String toolCode = normalizeToolCode(step.toolCode());
            Map<String, Object> arguments = arguments(toolCode, step);
            Object result = invoke(toolCode, arguments);
            WorkflowReviewResult.ToolCallResult toolCall = new WorkflowReviewResult.ToolCallResult(
                    step.toolCode(),
                    arguments,
                    result,
                    true,
                    null);
            return new AgentExecutionResult(
                    true,
                    resultSummary(toolCode, result),
                    List.of(),
                    List.of(),
                    List.of(toolCall),
                    null,
                    Map.of("toolCode", step.toolCode()));
        } catch (RuntimeException ex) {
            WorkflowReviewResult.ToolCallResult toolCall = new WorkflowReviewResult.ToolCallResult(
                    step.toolCode(),
                    Map.of("instruction", safe(step.instruction())),
                    null,
                    false,
                    ex.getMessage());
            return new AgentExecutionResult(
                    false,
                    null,
                    List.of(),
                    List.of(),
                    List.of(toolCall),
                    ex.getMessage(),
                    Map.of("toolCode", safe(step.toolCode())));
        }
    }

    private Object invoke(String toolCode, Map<String, Object> arguments) {
        return switch (toolCode) {
            case "tool:ticket:query" -> ticketTools.queryTicket(
                    new TicketQueryRequest((String) arguments.get("ticketNo")));
            case "tool:log:search" -> logTools.queryLogs(new LogQueryRequest(
                    (String) arguments.get("serviceName"),
                    (String) arguments.get("keyword"),
                    new LogQueryRequest.TimeRange(
                            (Instant) arguments.get("from"),
                            (Instant) arguments.get("to")),
                    (Integer) arguments.get("limit")));
            case "tool:api-status:query" -> endpointTools.queryEndpointStatus(
                    new EndpointStatusRequest((String) arguments.get("endpoint")));
            default -> throw new IllegalArgumentException("Unsupported tool code: " + toolCode);
        };
    }

    private Map<String, Object> arguments(String toolCode, WorkflowPlanStep step) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        String text = searchableText(step);
        switch (toolCode) {
            case "tool:ticket:query" -> arguments.put("ticketNo", ticketNo(text));
            case "tool:log:search" -> {
                Instant to = Instant.now().truncatedTo(ChronoUnit.SECONDS);
                arguments.put("serviceName", serviceName(text));
                arguments.put("keyword", keyword(text));
                arguments.put("from", to.minus(1, ChronoUnit.HOURS));
                arguments.put("to", to);
                arguments.put("limit", 20);
            }
            case "tool:api-status:query" -> arguments.put("endpoint", endpoint(text));
            default -> arguments.put("instruction", safe(step.instruction()));
        }
        return arguments;
    }

    private String ticketNo(String text) {
        Matcher matcher = TICKET_NO_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group().toUpperCase();
        }
        return DEFAULT_TICKET_NO;
    }

    private String serviceName(String text) {
        String extracted = valueAfterKey(text, "serviceName");
        if (extracted == null) {
            extracted = valueAfterKey(text, "service");
        }
        return extracted == null ? DEFAULT_SERVICE_NAME : extracted;
    }

    private String keyword(String text) {
        String extracted = valueAfterKey(text, "keyword");
        return extracted == null ? DEFAULT_LOG_KEYWORD : extracted;
    }

    private String endpoint(String text) {
        String extracted = valueAfterKey(text, "endpoint");
        if (extracted == null) {
            extracted = valueAfterKey(text, "service");
        }
        return extracted == null ? DEFAULT_ENDPOINT : extracted;
    }

    private String valueAfterKey(String text, String key) {
        Pattern pattern = Pattern.compile("(?i)\\b" + Pattern.quote(key) + "\\s*[:=]\\s*([A-Za-z0-9._:/-]+)");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String searchableText(WorkflowPlanStep step) {
        StringBuilder builder = new StringBuilder();
        builder.append(safe(step.instruction()));
        for (String value : step.requiredInputs()) {
            builder.append(' ').append(value);
        }
        for (String value : step.expectedOutputs()) {
            builder.append(' ').append(value);
        }
        return builder.toString();
    }

    private String resultSummary(String toolCode, Object result) {
        if (result == null) {
            return "Tool " + toolCode + " completed";
        }
        String summary = String.valueOf(result);
        return summary.length() > 1000 ? summary.substring(0, 1000) : summary;
    }

    private String normalizeToolCode(String toolCode) {
        return toolCode == null ? "" : toolCode.trim().toLowerCase();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
