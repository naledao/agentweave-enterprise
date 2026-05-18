package com.agentweave.workflow.application;

import com.agentweave.observability.application.AgentWeaveMetrics;
import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.domain.AgentStepEntity;
import org.springframework.stereotype.Service;

@Service
public class WorkflowMetricsService {

    private final AgentWeaveMetrics agentWeaveMetrics;

    public WorkflowMetricsService(AgentWeaveMetrics agentWeaveMetrics) {
        this.agentWeaveMetrics = agentWeaveMetrics;
    }

    public void recordRunCompleted(AgentRunEntity run) {
        agentWeaveMetrics.recordWorkflowRun(run);
    }

    public void recordStepCompleted(AgentStepEntity step) {
        agentWeaveMetrics.recordWorkflowStep(step);
    }

    public void recordStepRetry(AgentStepEntity step) {
        agentWeaveMetrics.recordWorkflowStepRetry(step);
    }

    public void recordApprovalWaitCompleted(AgentStepEntity step) {
        agentWeaveMetrics.recordWorkflowApprovalWait(step);
    }
}
