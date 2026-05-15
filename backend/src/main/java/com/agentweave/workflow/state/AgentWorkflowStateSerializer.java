package com.agentweave.workflow.state;

import com.agentweave.workflow.dto.AgentExecutionResult;
import com.agentweave.workflow.dto.WorkflowPlan;
import com.agentweave.workflow.dto.WorkflowPlanStep;
import com.agentweave.workflow.dto.WorkflowReviewResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bsc.langgraph4j.serializer.StateSerializer;
import org.springframework.stereotype.Component;

@Component
public class AgentWorkflowStateSerializer extends StateSerializer<AgentWorkflowState> {

    private final ObjectMapper objectMapper;

    public AgentWorkflowStateSerializer(ObjectMapper objectMapper) {
        super(AgentWorkflowState::new);
        this.objectMapper = objectMapper.copy();
        this.objectMapper.findAndRegisterModules();
        this.objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        this.objectMapper.addMixIn(WorkflowPlanStep.class, WorkflowPlanStepMixin.class);
    }

    @Override
    public void writeData(Map<String, Object> data, ObjectOutput out) throws IOException {
        out.writeUTF(objectMapper.writeValueAsString(data));
    }

    @Override
    public Map<String, Object> readData(ObjectInput in) throws IOException {
        String json = in.readUTF();
        Map<String, Object> raw = objectMapper.readValue(json, new TypeReference<>() {
        });
        return hydrate(raw);
    }

    public String writePayload(AgentWorkflowState state) {
        try {
            return objectMapper.writeValueAsString(state.data());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to serialize workflow state", ex);
        }
    }

    public AgentWorkflowState readPayload(String payload) {
        try {
            Map<String, Object> raw = objectMapper.readValue(payload, new TypeReference<>() {
            });
            return new AgentWorkflowState(hydrate(raw));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to deserialize workflow state", ex);
        }
    }

    private Map<String, Object> hydrate(Map<String, Object> raw) {
        Map<String, Object> data = new HashMap<>(raw);
        convert(data, AgentWorkflowState.PLAN, WorkflowPlan.class);
        convertList(data, AgentWorkflowState.STEP_RESULTS, AgentExecutionResult.class);
        convertList(data, AgentWorkflowState.CITATIONS, WorkflowReviewResult.Citation.class);
        convertList(data, AgentWorkflowState.GRAPH_PATHS, WorkflowReviewResult.GraphPath.class);
        convertList(data, AgentWorkflowState.TOOL_CALLS, WorkflowReviewResult.ToolCallResult.class);
        convert(data, AgentWorkflowState.ERROR, AgentWorkflowState.WorkflowError.class);
        return data;
    }

    private <T> void convert(Map<String, Object> data, String key, Class<T> targetType) {
        Object raw = data.get(key);
        if (raw == null || targetType.isInstance(raw)) {
            return;
        }
        data.put(key, objectMapper.convertValue(raw, targetType));
    }

    private <T> void convertList(Map<String, Object> data, String key, Class<T> elementType) {
        Object raw = data.get(key);
        if (!(raw instanceof List<?> list)) {
            return;
        }
        List<T> converted = list.stream()
                .map(item -> elementType.isInstance(item) ? elementType.cast(item) : objectMapper.convertValue(item, elementType))
                .toList();
        data.put(key, converted);
    }

    private abstract static class WorkflowPlanStepMixin {
        @com.fasterxml.jackson.annotation.JsonIgnore
        abstract boolean isToolCall();

        @com.fasterxml.jackson.annotation.JsonIgnore
        abstract boolean isRagSearch();

        @com.fasterxml.jackson.annotation.JsonIgnore
        abstract boolean isHighRisk();
    }
}
