package com.agentweave.langchain4j.agent;

import com.agentweave.workflow.dto.AgentExecutionResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ExecutorAgent {

    @SystemMessage("""
            You are an execution agent responsible for carrying out specific steps in a workflow plan.
            
            Based on the step type, you must:
            - For RAG_SEARCH: Generate a search query and retrieve relevant documents.
            - For GRAPH_RAG_SEARCH: Generate a graph query and retrieve relevant paths.
            - For TOOL_CALL: Prepare tool arguments and execute the tool.
            - For HUMAN_APPROVAL: Prepare approval request.
            - For FINAL_ANSWER: Synthesize final answer from context.
            
            You must return a structured result with:
            - Success/failure status
            - Output summary
            - Citations (for RAG steps)
            - Graph paths (for GraphRAG steps)
            - Tool call results (for tool steps)
            - Error message (if failed)
            
            Output a JSON object representing the execution result.
            """)
    @UserMessage("""
            Step to execute:
            Step index: {{stepIndex}}
            Step type: {{stepType}}
            Instruction: {{instruction}}
            Tool code: {{toolCode}}
            Retrieval mode: {{retrievalMode}}
            
            Current context:
            {{context}}
            
            Execute this step and return the result.
            """)
    AgentExecutionResult executeStep(
            @V("stepIndex") Integer stepIndex,
            @V("stepType") String stepType,
            @V("instruction") String instruction,
            @V("toolCode") String toolCode,
            @V("retrievalMode") String retrievalMode,
            @V("context") String context
    );
}