package com.agentweave.workflow.domain;

public enum WorkflowRunStatus {
    CREATED,
    PLANNING,
    EXECUTING,
    WAITING_APPROVAL,
    REVIEWING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}
