package com.agentweave.workflow.state;

import com.agentweave.workflow.dto.AgentExecutionResult;
import com.agentweave.workflow.dto.WorkflowPlan;
import com.agentweave.workflow.dto.WorkflowReviewResult;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bsc.langgraph4j.state.AgentState;

public class AgentWorkflowState extends AgentState implements Serializable {

    public static final String RUN_ID = "runId";
    public static final String CONVERSATION_ID = "conversationId";
    public static final String USER_ID = "userId";
    public static final String TRACE_ID = "traceId";
    public static final String GOAL = "goal";
    public static final String PLAN = "plan";
    public static final String CURRENT_STEP_INDEX = "currentStepIndex";
    public static final String STEP_RESULTS = "stepResults";
    public static final String CITATIONS = "citations";
    public static final String GRAPH_PATHS = "graphPaths";
    public static final String TOOL_CALLS = "toolCalls";
    public static final String RISK_LEVEL = "riskLevel";
    public static final String APPROVAL_STATUS = "approvalStatus";
    public static final String APPROVAL_ID = "approvalId";
    public static final String FINAL_ANSWER = "finalAnswer";
    public static final String ERROR = "error";
    public static final String NEXT_NODE = "nextNode";

    public AgentWorkflowState(Map<String, Object> initData) {
        super(initData);
    }

    public UUID runId() {
        return uuidValue(RUN_ID);
    }

    public UUID conversationId() {
        return uuidValue(CONVERSATION_ID);
    }

    public UUID userId() {
        return uuidValue(USER_ID);
    }

    public String traceId() {
        return value(TRACE_ID, "");
    }

    public String goal() {
        return value(GOAL, "");
    }

    public WorkflowPlan plan() {
        return value(PLAN).map(WorkflowPlan.class::cast).orElse(null);
    }

    public int currentStepIndex() {
        return value(CURRENT_STEP_INDEX, 0);
    }

    public List<AgentExecutionResult> stepResults() {
        return value(STEP_RESULTS, List.<AgentExecutionResult>of());
    }

    public List<WorkflowReviewResult.Citation> citations() {
        return value(CITATIONS, List.<WorkflowReviewResult.Citation>of());
    }

    public List<WorkflowReviewResult.GraphPath> graphPaths() {
        return value(GRAPH_PATHS, List.<WorkflowReviewResult.GraphPath>of());
    }

    public List<WorkflowReviewResult.ToolCallResult> toolCalls() {
        return value(TOOL_CALLS, List.<WorkflowReviewResult.ToolCallResult>of());
    }

    public String riskLevel() {
        return value(RISK_LEVEL, "");
    }

    public String approvalStatus() {
        return value(APPROVAL_STATUS, "");
    }

    public UUID approvalId() {
        return uuidValue(APPROVAL_ID);
    }

    public String finalAnswer() {
        return value(FINAL_ANSWER, "");
    }

    public WorkflowError error() {
        return value(ERROR).map(WorkflowError.class::cast).orElse(null);
    }

    public boolean hasError() {
        return error() != null;
    }

    private UUID uuidValue(String key) {
        Object raw = data().get(key);
        if (raw instanceof UUID uuid) {
            return uuid;
        }
        if (raw instanceof String value && !value.isBlank()) {
            return UUID.fromString(value);
        }
        return null;
    }

    public record WorkflowError(
            String code,
            String message,
            String nodeName,
            Integer stepIndex,
            boolean recoverable) implements Serializable {
    }
}
