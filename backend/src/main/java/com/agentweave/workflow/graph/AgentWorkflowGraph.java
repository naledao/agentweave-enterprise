package com.agentweave.workflow.graph;

import com.agentweave.workflow.node.WorkflowNodeExecutor;
import com.agentweave.workflow.state.AgentWorkflowState;
import com.agentweave.workflow.state.AgentWorkflowStateSerializer;
import com.agentweave.workflow.graph.WorkflowNodeNames;
import java.util.HashMap;
import java.util.Map;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphDefinition;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.checkpoint.PostgresSaver;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.springframework.stereotype.Component;

@Component
public class AgentWorkflowGraph {

    private final CompiledGraph<AgentWorkflowState> compiledGraph;

    public AgentWorkflowGraph(
            WorkflowNodeExecutor nodeExecutor,
            AgentWorkflowStateSerializer stateSerializer,
            PostgresSaver checkpointSaver) {
        try {
            this.compiledGraph = buildGraph(nodeExecutor, stateSerializer)
                    .compile(CompileConfig.builder()
                            .checkpointSaver(checkpointSaver)
                            .interruptAfter(WorkflowNodeNames.HUMAN_APPROVAL_NODE)
                            .releaseThread(false)
                            .build());
        } catch (GraphStateException ex) {
            throw new IllegalStateException("Failed to compile Agent workflow graph", ex);
        }
    }

    public CompiledGraph<AgentWorkflowState> compiledGraph() {
        return compiledGraph;
    }

    private StateGraph<AgentWorkflowState> buildGraph(
            WorkflowNodeExecutor nodeExecutor,
            AgentWorkflowStateSerializer stateSerializer) throws GraphStateException {
        StateGraph<AgentWorkflowState> graph = new StateGraph<>(channels(), stateSerializer);
        graph.addNode(WorkflowNodeNames.LOAD_CONTEXT, AsyncNodeAction.node_async(nodeExecutor::loadContext));
        graph.addNode(WorkflowNodeNames.PLANNER, AsyncNodeAction.node_async(nodeExecutor::planner));
        graph.addNode(WorkflowNodeNames.VALIDATE_PLAN, AsyncNodeAction.node_async(nodeExecutor::validatePlan));
        graph.addNode(WorkflowNodeNames.ROUTE_STEP, AsyncNodeAction.node_async(nodeExecutor::routeStep));
        graph.addNode(WorkflowNodeNames.RAG_NODE, AsyncNodeAction.node_async(nodeExecutor::rag));
        graph.addNode(WorkflowNodeNames.GRAPH_RAG_NODE, AsyncNodeAction.node_async(nodeExecutor::graphRag));
        graph.addNode(WorkflowNodeNames.TOOL_NODE, AsyncNodeAction.node_async(nodeExecutor::tool));
        graph.addNode(WorkflowNodeNames.HUMAN_APPROVAL_NODE, AsyncNodeAction.node_async(nodeExecutor::humanApproval));
        graph.addNode(WorkflowNodeNames.REVIEWER, AsyncNodeAction.node_async(nodeExecutor::reviewer));
        graph.addNode(WorkflowNodeNames.PERSIST_RESULT, AsyncNodeAction.node_async(nodeExecutor::persistResult));
        graph.addNode(WorkflowNodeNames.ERROR, AsyncNodeAction.node_async(nodeExecutor::error));

        graph.addEdge(GraphDefinition.START, WorkflowNodeNames.LOAD_CONTEXT);
        graph.addEdge(WorkflowNodeNames.LOAD_CONTEXT, WorkflowNodeNames.PLANNER);
        graph.addEdge(WorkflowNodeNames.PLANNER, WorkflowNodeNames.VALIDATE_PLAN);
        graph.addEdge(WorkflowNodeNames.VALIDATE_PLAN, WorkflowNodeNames.ROUTE_STEP);
        graph.addConditionalEdges(
                WorkflowNodeNames.ROUTE_STEP,
                AsyncEdgeAction.edge_async(nodeExecutor::nextNode),
                routingEdges());
        graph.addEdge(WorkflowNodeNames.RAG_NODE, WorkflowNodeNames.ROUTE_STEP);
        graph.addEdge(WorkflowNodeNames.GRAPH_RAG_NODE, WorkflowNodeNames.ROUTE_STEP);
        graph.addEdge(WorkflowNodeNames.TOOL_NODE, WorkflowNodeNames.ROUTE_STEP);
        graph.addEdge(WorkflowNodeNames.HUMAN_APPROVAL_NODE, WorkflowNodeNames.REVIEWER);
        graph.addEdge(WorkflowNodeNames.REVIEWER, WorkflowNodeNames.PERSIST_RESULT);
        graph.addEdge(WorkflowNodeNames.PERSIST_RESULT, GraphDefinition.END);
        graph.addEdge(WorkflowNodeNames.ERROR, GraphDefinition.END);
        return graph;
    }

    private Map<String, String> routingEdges() {
        return Map.of(
                WorkflowNodeNames.RAG_NODE, WorkflowNodeNames.RAG_NODE,
                WorkflowNodeNames.GRAPH_RAG_NODE, WorkflowNodeNames.GRAPH_RAG_NODE,
                WorkflowNodeNames.TOOL_NODE, WorkflowNodeNames.TOOL_NODE,
                WorkflowNodeNames.HUMAN_APPROVAL_NODE, WorkflowNodeNames.HUMAN_APPROVAL_NODE,
                WorkflowNodeNames.REVIEWER, WorkflowNodeNames.REVIEWER,
                WorkflowNodeNames.ERROR, WorkflowNodeNames.ERROR);
    }

    private Map<String, Channel<?>> channels() {
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put(AgentWorkflowState.RUN_ID, Channels.base(() -> null));
        channels.put(AgentWorkflowState.CONVERSATION_ID, Channels.base(() -> null));
        channels.put(AgentWorkflowState.USER_ID, Channels.base(() -> null));
        channels.put(AgentWorkflowState.TRACE_ID, Channels.base(() -> ""));
        channels.put(AgentWorkflowState.GOAL, Channels.base(() -> ""));
        channels.put(AgentWorkflowState.PLAN, Channels.base(() -> null));
        channels.put(AgentWorkflowState.CURRENT_STEP_INDEX, Channels.base(() -> 0));
        channels.put(AgentWorkflowState.STEP_RESULTS, Channels.appender(java.util.ArrayList::new));
        channels.put(AgentWorkflowState.CITATIONS, Channels.appender(java.util.ArrayList::new));
        channels.put(AgentWorkflowState.GRAPH_PATHS, Channels.appender(java.util.ArrayList::new));
        channels.put(AgentWorkflowState.TOOL_CALLS, Channels.appender(java.util.ArrayList::new));
        channels.put(AgentWorkflowState.RISK_LEVEL, Channels.base(() -> ""));
        channels.put(AgentWorkflowState.APPROVAL_STATUS, Channels.base(() -> ""));
        channels.put(AgentWorkflowState.APPROVAL_ID, Channels.base(() -> null));
        channels.put(AgentWorkflowState.FINAL_ANSWER, Channels.base(() -> ""));
        channels.put(AgentWorkflowState.ERROR, Channels.base(() -> null));
        channels.put(AgentWorkflowState.NEXT_NODE, Channels.base(() -> WorkflowNodeNames.REVIEWER));
        return channels;
    }
}
