package com.agentweave.workflow.domain;

public enum AgentStepStatus {
    PENDING,
    RUNNING,
    WAITING_APPROVAL,
    RETRYING,
    SUCCEEDED,
    FAILED,
    SKIPPED
}
