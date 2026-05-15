package com.agentweave.workflow.dto;

import java.util.List;
import java.util.Map;

public record AgentExecutionResult(
        boolean success,
        String outputSummary,
        List<WorkflowReviewResult.Citation> citations,
        List<WorkflowReviewResult.GraphPath> graphPaths,
        List<WorkflowReviewResult.ToolCallResult> toolCalls,
        String errorMessage,
        Map<String, Object> metadata
) {
    public static AgentExecutionResult success(String outputSummary, List<WorkflowReviewResult.Citation> citations, List<WorkflowReviewResult.GraphPath> graphPaths, List<WorkflowReviewResult.ToolCallResult> toolCalls) {
        return new AgentExecutionResult(true, outputSummary, citations, graphPaths, toolCalls, null, Map.of());
    }

    public static AgentExecutionResult failure(String errorMessage) {
        return new AgentExecutionResult(false, null, List.of(), List.of(), List.of(), errorMessage, Map.of());
    }
}