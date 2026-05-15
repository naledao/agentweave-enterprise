package com.agentweave.workflow.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class WorkflowStatusMachine {

    private static final Map<WorkflowRunStatus, Set<WorkflowRunStatus>> TRANSITIONS = new EnumMap<>(WorkflowRunStatus.class);

    static {
        TRANSITIONS.put(WorkflowRunStatus.CREATED, Set.of(
                WorkflowRunStatus.PLANNING,
                WorkflowRunStatus.FAILED,
                WorkflowRunStatus.CANCELLED));

        TRANSITIONS.put(WorkflowRunStatus.PLANNING, Set.of(
                WorkflowRunStatus.EXECUTING,
                WorkflowRunStatus.FAILED,
                WorkflowRunStatus.CANCELLED));

        TRANSITIONS.put(WorkflowRunStatus.EXECUTING, Set.of(
                WorkflowRunStatus.WAITING_APPROVAL,
                WorkflowRunStatus.REVIEWING,
                WorkflowRunStatus.FAILED,
                WorkflowRunStatus.CANCELLED));

        TRANSITIONS.put(WorkflowRunStatus.WAITING_APPROVAL, Set.of(
                WorkflowRunStatus.EXECUTING,
                WorkflowRunStatus.REVIEWING,
                WorkflowRunStatus.FAILED,
                WorkflowRunStatus.CANCELLED));

        TRANSITIONS.put(WorkflowRunStatus.REVIEWING, Set.of(
                WorkflowRunStatus.SUCCEEDED,
                WorkflowRunStatus.FAILED,
                WorkflowRunStatus.CANCELLED));

        TRANSITIONS.put(WorkflowRunStatus.SUCCEEDED, Set.of());
        TRANSITIONS.put(WorkflowRunStatus.FAILED, Set.of());
        TRANSITIONS.put(WorkflowRunStatus.CANCELLED, Set.of());
    }

    private WorkflowStatusMachine() {
    }

    public static boolean canTransit(WorkflowRunStatus from, WorkflowRunStatus to) {
        if (from == null || to == null) {
            return false;
        }
        Set<WorkflowRunStatus> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public static void validateTransition(WorkflowRunStatus from, WorkflowRunStatus to) {
        if (!canTransit(from, to)) {
            throw new IllegalStateException(
                    String.format("Invalid workflow status transition from %s to %s", from, to));
        }
    }
}
