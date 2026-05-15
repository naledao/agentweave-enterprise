package com.agentweave.langchain4j.agent;

import com.agentweave.workflow.dto.WorkflowPlan;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface PlannerAgent {

    @SystemMessage("""
            You are a planning agent responsible for decomposing complex user goals into structured execution plans.
            
            Your task is to analyze the user's goal and create a step-by-step plan that can be executed by other agents.
            
            Each step must have:
            - A clear type (RAG_SEARCH, GRAPH_RAG_SEARCH, TOOL_CALL, HUMAN_APPROVAL, FINAL_ANSWER)
            - A specific instruction
            - Required inputs
            - Expected outputs
            
            For RAG steps, specify retrieval mode (VECTOR, GRAPH, HYBRID).
            For tool steps, specify the tool code.
            For high-risk steps, mark risk level as HIGH.
            
            The plan must end with either a FINAL_ANSWER step or a REVIEW step.
            
            Output a JSON object representing the plan.
            """)
    @UserMessage("""
            User goal: {{goal}}
            
            Conversation history (if any):
            {{context}}
            
            Create a structured execution plan for this goal.
            """)
    WorkflowPlan createPlan(@V("goal") String goal, @V("context") String context);
}