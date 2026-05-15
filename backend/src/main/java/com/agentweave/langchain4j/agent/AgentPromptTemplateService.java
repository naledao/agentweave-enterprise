package com.agentweave.langchain4j.agent;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AgentPromptTemplateService {

    public String buildPlannerContext(String goal, String conversationHistory) {
        return String.format("""
                User Goal: %s
                
                Conversation History:
                %s
                """, goal, conversationHistory != null ? conversationHistory : "No previous conversation");
    }

    public String buildExecutorContext(String stepInstruction, String toolCode, String retrievalMode, String previousResults) {
        return String.format("""
                Step Instruction: %s
                Tool Code: %s
                Retrieval Mode: %s
                
                Previous Step Results:
                %s
                """, 
                stepInstruction,
                toolCode != null ? toolCode : "N/A",
                retrievalMode != null ? retrievalMode : "N/A",
                previousResults != null ? previousResults : "No previous results");
    }

    public String buildReviewerContext(String goal, String stepResults) {
        return String.format("""
                User Goal: %s
                
                Step Results:
                %s
                """, goal, stepResults);
    }

    public String formatStepResults(Map<Integer, Object> stepResults) {
        if (stepResults == null || stepResults.isEmpty()) {
            return "No step results available";
        }
        
        StringBuilder sb = new StringBuilder();
        stepResults.forEach((stepIndex, result) -> {
            sb.append("Step ").append(stepIndex).append(": ").append(result).append("\n");
        });
        return sb.toString();
    }
}