package com.agentweave.workflow.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agentweave.workflow.application.WorkflowRunService;
import com.agentweave.workflow.dto.CreateWorkflowRunRequest;
import com.agentweave.workflow.dto.WorkflowRunResponse;
import com.agentweave.workflow.dto.WorkflowStepResponse;
import com.agentweave.workflow.domain.WorkflowRunStatus;
import com.agentweave.workflow.domain.AgentStepStatus;
import com.agentweave.workflow.domain.AgentStepType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkflowController.class)
@DisplayName("WorkflowController")
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkflowRunService workflowRunService;

    @Test
    @DisplayName("should create workflow run successfully")
    void shouldCreateWorkflowRunSuccessfully() throws Exception {
        UUID runId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        WorkflowRunResponse response = new WorkflowRunResponse(
                runId,
                UUID.randomUUID(),
                userId,
                "Analyze payment API failure rate",
                WorkflowRunStatus.CREATED,
                0,
                null,
                "trace-123",
                null,
                null,
                now);

        when(workflowRunService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/workflows/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateWorkflowRunRequest(
                                UUID.randomUUID(),
                                "Analyze payment API failure rate"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").value(runId.toString()))
                .andExpect(jsonPath("$.goal").value("Analyze payment API failure rate"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.traceId").value("trace-123"));
    }

    @Test
    @DisplayName("should return 400 when goal is blank")
    void shouldReturn400WhenGoalIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/workflows/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateWorkflowRunRequest(
                                UUID.randomUUID(),
                                ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should get workflow run successfully")
    void shouldGetWorkflowRunSuccessfully() throws Exception {
        UUID runId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        WorkflowRunResponse response = new WorkflowRunResponse(
                runId,
                UUID.randomUUID(),
                userId,
                "Test goal",
                WorkflowRunStatus.PLANNING,
                2,
                null,
                "trace-123",
                now,
                null,
                now);

        when(workflowRunService.get(runId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/workflows/runs/{runId}", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId.toString()))
                .andExpect(jsonPath("$.status").value("PLANNING"))
                .andExpect(jsonPath("$.currentStepIndex").value(2));
    }

    @Test
    @DisplayName("should get workflow steps successfully")
    void shouldGetWorkflowStepsSuccessfully() throws Exception {
        UUID runId = UUID.randomUUID();
        Instant now = Instant.now();

        List<WorkflowStepResponse> steps = List.of(
                new WorkflowStepResponse(
                        UUID.randomUUID(), 0, AgentStepType.PLANNING, "planner",
                        AgentStepStatus.SUCCEEDED, "input1", "output1",
                        now, now, 100L, null, null),
                new WorkflowStepResponse(
                        UUID.randomUUID(), 1, AgentStepType.RAG_SEARCH, "rag",
                        AgentStepStatus.RUNNING, "input2", null,
                        now, null, null, null, null));

        when(workflowRunService.getSteps(runId)).thenReturn(steps);

        mockMvc.perform(get("/api/v1/workflows/runs/{runId}/steps", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stepIndex").value(0))
                .andExpect(jsonPath("$[0].stepType").value("PLANNING"))
                .andExpect(jsonPath("$[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$[1].stepIndex").value(1))
                .andExpect(jsonPath("$[1].stepType").value("RAG_SEARCH"))
                .andExpect(jsonPath("$[1].status").value("RUNNING"));
    }

    @Test
    @DisplayName("should cancel workflow run successfully")
    void shouldCancelWorkflowRunSuccessfully() throws Exception {
        UUID runId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        WorkflowRunResponse response = new WorkflowRunResponse(
                runId,
                UUID.randomUUID(),
                userId,
                "Test goal",
                WorkflowRunStatus.CANCELLED,
                3,
                null,
                "trace-123",
                now,
                now,
                now);

        when(workflowRunService.cancel(runId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/workflows/runs/{runId}/cancel", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.finishedAt").isNotEmpty());
    }
}
