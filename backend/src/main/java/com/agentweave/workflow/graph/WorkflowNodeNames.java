package com.agentweave.workflow.graph;

public final class WorkflowNodeNames {

    public static final String LOAD_CONTEXT = "load_context";
    public static final String PLANNER = "planner";
    public static final String VALIDATE_PLAN = "validate_plan";
    public static final String ROUTE_STEP = "route_step";
    public static final String RAG_NODE = "rag_node";
    public static final String GRAPH_RAG_NODE = "graph_rag_node";
    public static final String TOOL_NODE = "tool_node";
    public static final String HUMAN_APPROVAL_NODE = "human_approval_node";
    public static final String REVIEWER = "reviewer";
    public static final String PERSIST_RESULT = "persist_result";
    public static final String ERROR = "error";

    private WorkflowNodeNames() {
    }
}
