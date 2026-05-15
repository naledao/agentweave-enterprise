package com.agentweave.workflow.dto;

import java.util.List;
import java.util.Map;

public record WorkflowReviewResult(
        String finalAnswer,
        List<Citation> citations,
        List<GraphPath> graphPaths,
        List<ToolCallResult> toolCalls,
        List<String> failureReasons,
        boolean sufficient,
        String summary
) {
    public record Citation(
            String documentId,
            String chunkId,
            String content,
            Double score,
            String source
    ) {}

    public record GraphPath(
            List<String> nodeIds,
            List<String> relationshipTypes,
            String description
    ) {}

    public record ToolCallResult(
            String toolCode,
            Map<String, Object> arguments,
            Object result,
            boolean success,
            String errorMessage
    ) {}

    public static WorkflowReviewResult sufficient(String finalAnswer, List<Citation> citations, List<GraphPath> graphPaths, List<ToolCallResult> toolCalls) {
        return new WorkflowReviewResult(
                finalAnswer,
                citations,
                graphPaths,
                toolCalls,
                List.of(),
                true,
                "Answer is sufficient"
        );
    }

    public static WorkflowReviewResult insufficient(List<String> failureReasons) {
        return new WorkflowReviewResult(
                null,
                List.of(),
                List.of(),
                List.of(),
                failureReasons,
                false,
                "Answer is insufficient"
        );
    }
}