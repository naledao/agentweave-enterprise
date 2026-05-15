package com.agentweave.workflow.domain;

public enum WorkflowRunStatus {
    CREATED,
    PLANNING,
    EXECUTING,
    REVIEWING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}
