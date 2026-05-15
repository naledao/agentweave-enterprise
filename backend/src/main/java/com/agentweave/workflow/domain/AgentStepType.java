package com.agentweave.workflow.domain;

public enum AgentStepType {
    PLANNING,
    RAG_SEARCH,
    GRAPH_RAG_SEARCH,
    TOOL_CALL,
    REVIEW,
    FINAL_ANSWER,
    HUMAN_APPROVAL,
    CHECKPOINT,
    ERROR
}
