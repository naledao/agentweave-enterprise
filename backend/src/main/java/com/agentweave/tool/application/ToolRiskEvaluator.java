package com.agentweave.tool.application;

import com.agentweave.tool.domain.ToolDefinitionEntity;
import com.agentweave.tool.domain.ToolRiskLevel;
import com.agentweave.workflow.dto.WorkflowPlanStep;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ToolRiskEvaluator {

    private final ToolDefinitionService toolDefinitionService;

    public ToolRiskEvaluator(ToolDefinitionService toolDefinitionService) {
        this.toolDefinitionService = toolDefinitionService;
    }

    public ToolRiskLevel evaluate(WorkflowPlanStep step) {
        if (step == null || step.toolCode() == null || step.toolCode().isBlank()) {
            return ToolRiskLevel.LOW;
        }
        return toolDefinitionService.findByPermissionCode(step.toolCode())
                .map(ToolDefinitionEntity::getRiskLevel)
                .orElseGet(() -> fromPlanRiskLevel(step.riskLevel()));
    }

    public boolean requiresApproval(WorkflowPlanStep step) {
        return evaluate(step) == ToolRiskLevel.HIGH;
    }

    private ToolRiskLevel fromPlanRiskLevel(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return ToolRiskLevel.LOW;
        }
        try {
            return ToolRiskLevel.valueOf(riskLevel.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ToolRiskLevel.LOW;
        }
    }
}
