package com.agentweave.workflow.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("WorkflowStatusMachine")
class WorkflowStatusMachineTest {

    @ParameterizedTest
    @CsvSource({
            "CREATED, PLANNING",
            "CREATED, FAILED",
            "CREATED, CANCELLED",
            "PLANNING, EXECUTING",
            "PLANNING, FAILED",
            "PLANNING, CANCELLED",
            "EXECUTING, REVIEWING",
            "EXECUTING, FAILED",
            "EXECUTING, CANCELLED",
            "REVIEWING, SUCCEEDED",
            "REVIEWING, FAILED",
            "REVIEWING, CANCELLED"
    })
    void shouldAllowValidTransitions(WorkflowRunStatus from, WorkflowRunStatus to) {
        assertThat(WorkflowStatusMachine.canTransit(from, to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "CREATED, EXECUTING",
            "CREATED, REVIEWING",
            "CREATED, SUCCEEDED",
            "PLANNING, CREATED",
            "PLANNING, REVIEWING",
            "PLANNING, SUCCEEDED",
            "EXECUTING, CREATED",
            "EXECUTING, PLANNING",
            "EXECUTING, SUCCEEDED",
            "REVIEWING, CREATED",
            "REVIEWING, PLANNING",
            "REVIEWING, EXECUTING",
            "SUCCEEDED, CREATED",
            "SUCCEEDED, PLANNING",
            "SUCCEEDED, EXECUTING",
            "SUCCEEDED, REVIEWING",
            "SUCCEEDED, FAILED",
            "SUCCEEDED, CANCELLED",
            "FAILED, CREATED",
            "FAILED, PLANNING",
            "FAILED, EXECUTING",
            "FAILED, REVIEWING",
            "FAILED, SUCCEEDED",
            "FAILED, CANCELLED",
            "CANCELLED, CREATED",
            "CANCELLED, PLANNING",
            "CANCELLED, EXECUTING",
            "CANCELLED, REVIEWING",
            "CANCELLED, SUCCEEDED",
            "CANCELLED, FAILED"
    })
    void shouldRejectInvalidTransitions(WorkflowRunStatus from, WorkflowRunStatus to) {
        assertThat(WorkflowStatusMachine.canTransit(from, to)).isFalse();
    }

    @Test
    @DisplayName("should reject null from status")
    void shouldRejectNullFromStatus() {
        assertThat(WorkflowStatusMachine.canTransit(null, WorkflowRunStatus.PLANNING)).isFalse();
    }

    @Test
    @DisplayName("should reject null to status")
    void shouldRejectNullToStatus() {
        assertThat(WorkflowStatusMachine.canTransit(WorkflowRunStatus.CREATED, null)).isFalse();
    }

    @Test
    @DisplayName("should throw exception on invalid transition validation")
    void shouldThrowExceptionOnInvalidTransitionValidation() {
        assertThatThrownBy(() -> WorkflowStatusMachine.validateTransition(
                WorkflowRunStatus.CREATED, WorkflowRunStatus.SUCCEEDED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid workflow status transition");
    }

    @Test
    @DisplayName("should not throw exception on valid transition validation")
    void shouldNotThrowExceptionOnValidTransitionValidation() {
        WorkflowStatusMachine.validateTransition(WorkflowRunStatus.CREATED, WorkflowRunStatus.PLANNING);
    }
}
