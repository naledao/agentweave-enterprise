package com.agentweave.langchain4j.agent;

import com.agentweave.workflow.dto.WorkflowReviewResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ReviewerAgent {

    @SystemMessage("""
            You are a review agent responsible for evaluating step results and synthesizing a final answer.
            
            Your tasks:
            1. Check if the step results are sufficient to answer the user's goal.
            2. If sufficient, create a comprehensive final answer with proper citations.
            3. If insufficient, identify missing information and suggest next steps.
            4. For each citation, include document/chunk source and relevance score.
            5. For graph paths, describe the relationships found.
            6. For tool calls, summarize the results obtained.
            
            Output a JSON object representing the review result.
            """)
    @UserMessage("""
            User goal: {{goal}}
            
            Step results:
            {{stepResults}}
            
            Evaluate the results and provide a final review.
            """)
    WorkflowReviewResult review(@V("goal") String goal, @V("stepResults") String stepResults);
}