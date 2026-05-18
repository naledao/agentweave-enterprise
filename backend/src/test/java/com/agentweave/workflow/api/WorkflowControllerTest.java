package com.agentweave.workflow.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agentweave.langchain4j.agent.PlannerAgent;
import com.agentweave.auth.repository.UserRepository;
import com.agentweave.shared.audit.AuditLogService;
import com.agentweave.shared.security.AgentWeaveUserDetailsService;
import com.agentweave.shared.security.JwtTokenService;
import com.agentweave.shared.tracing.TraceIdProvider;
import com.agentweave.workflow.application.PlanValidator;
import com.agentweave.workflow.application.WorkflowExecutionService;
import com.agentweave.workflow.application.WorkflowRecoveryService;
import com.agentweave.workflow.application.WorkflowRunService;
import com.agentweave.workflow.dto.CreateWorkflowRunRequest;
import com.agentweave.workflow.dto.WorkflowRunListItemResponse;
import com.agentweave.workflow.dto.WorkflowRunListResponse;
import com.agentweave.workflow.dto.WorkflowRunQueryRequest;
import com.agentweave.workflow.dto.WorkflowRunResponse;
import com.agentweave.workflow.dto.WorkflowStepResponse;
import com.agentweave.workflow.domain.AgentRole;
import com.agentweave.workflow.domain.WorkflowRunStatus;
import com.agentweave.workflow.domain.AgentStepStatus;
import com.agentweave.workflow.domain.AgentStepType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkflowController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("WorkflowController")
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WorkflowRunService workflowRunService;

    @MockitoBean
    private WorkflowExecutionService workflowExecutionService;

    @MockitoBean
    private WorkflowRecoveryService workflowRecoveryService;

    @MockitoBean
    private PlannerAgent plannerAgent;

    @MockitoBean
    private PlanValidator planValidator;

    @MockitoBean
    private TraceIdProvider traceIdProvider;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AgentWeaveUserDetailsService userDetailsService;

    @Test
    @DisplayName("should list workflow runs with status filter")
    void shouldListWorkflowRunsSuccessfully() throws Exception {
        UUID runId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        WorkflowRunListResponse response = new WorkflowRunListResponse(
                List.of(new WorkflowRunListItemResponse(
                        runId,
                        UUID.randomUUID(),
                        userId,
                        "Analyze payment API failure rate",
                        WorkflowRunStatus.FAILED,
                        2,
                        null,
                        "WORKFLOW_FAILED",
                        "tool execution failed",
                        "trace-123",
                        now,
                        now,
                        now,
                        now)),
                0,
                20,
                1,
                1);

        when(workflowRunService.list(new WorkflowRunQueryRequest(0, 20, "FAILED"))).thenReturn(response);

        mockMvc.perform(get("/api/v1/workflows/runs")
                        .param("page", "0")
                        .param("size", "20")
                        .param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].runId").value(runId.toString()))
                .andExpect(jsonPath("$.items[0].status").value("FAILED"))
                .andExpect(jsonPath("$.items[0].errorMessage").value("tool execution failed"))
                .andExpect(jsonPath("$.items[0].traceId").value("trace-123"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @DisplayName("should create workflow run and trigger execution")
    void shouldCreateWorkflowRunSuccessfully() throws Exception {
        UUID runId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        WorkflowRunResponse created = new WorkflowRunResponse(
                runId, UUID.randomUUID(), userId,
                "Analyze payment API failure rate",
                WorkflowRunStatus.CREATED, 0, null, "trace-123",
                null, null, now);

        WorkflowRunResponse executed = new WorkflowRunResponse(
                runId, UUID.randomUUID(), userId,
                "Analyze payment API failure rate",
                WorkflowRunStatus.SUCCEEDED, 3, "Final answer",
                "trace-123", now, now, now);

        when(workflowRunService.create(any())).thenReturn(created);
        when(workflowExecutionService.executeWorkflow(runId)).thenReturn(executed);

        mockMvc.perform(post("/api/v1/workflows/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateWorkflowRunRequest(
                                UUID.randomUUID(),
                                "Analyze payment API failure rate"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").value(runId.toString()))
                .andExpect(jsonPath("$.goal").value("Analyze payment API failure rate"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    @DisplayName("should return 400 when goal is blank")
    void shouldReturn400WhenGoalIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/workflows/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateWorkflowRunRequest(
                                UUID.randomUUID(), ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should get workflow run successfully")
    void shouldGetWorkflowRunSuccessfully() throws Exception {
        UUID runId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        WorkflowRunResponse response = new WorkflowRunResponse(
                runId, UUID.randomUUID(), userId,
                "Test goal", WorkflowRunStatus.PLANNING, 2, null,
                "trace-123", now, null, now);

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
                        AgentRole.PLANNER, "trace-step-1",
                        AgentStepStatus.SUCCEEDED, "input1", "output1",
                        now, now, 100L, 0, null, null, null, null,
                        List.of(), List.of(), List.of()),
                new WorkflowStepResponse(
                        UUID.randomUUID(), 1, AgentStepType.RAG_SEARCH, "rag",
                        AgentRole.EXECUTOR, "trace-step-2",
                        AgentStepStatus.RUNNING, "input2", null,
                        now, null, null, 0, null, null, null, null,
                        List.of(new WorkflowStepResponse.WorkflowCitation(
                                "doc-1",
                                "Runbook",
                                "chunk-1",
                                "Runbook",
                                "kb",
                                "restart payment service",
                                0.87)),
                        List.of(new WorkflowStepResponse.WorkflowGraphPath(
                                "svc -> api",
                                1,
                                List.of("payment-service", "payment-api"),
                                List.of("DEPENDS_ON"),
                                List.of("chunk-1"),
                                0.92)),
                        List.of(new WorkflowStepResponse.WorkflowToolCall(
                                "tool:log:search",
                                "success",
                                Map.of("keyword", "payment").toString(),
                                "10 rows",
                                null,
                                null))));

        when(workflowRunService.getSteps(runId)).thenReturn(steps);

        mockMvc.perform(get("/api/v1/workflows/runs/{runId}/steps", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stepIndex").value(0))
                .andExpect(jsonPath("$[0].stepType").value("PLANNING"))
                .andExpect(jsonPath("$[0].agentRole").value("PLANNER"))
                .andExpect(jsonPath("$[0].traceId").value("trace-step-1"))
                .andExpect(jsonPath("$[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$[1].stepIndex").value(1))
                .andExpect(jsonPath("$[1].stepType").value("RAG_SEARCH"))
                .andExpect(jsonPath("$[1].status").value("RUNNING"))
                .andExpect(jsonPath("$[1].citations[0].snippet").value("restart payment service"))
                .andExpect(jsonPath("$[1].graphPaths[0].relationships[0]").value("DEPENDS_ON"))
                .andExpect(jsonPath("$[1].toolCalls[0].toolCode").value("tool:log:search"));
    }

    @Test
    @DisplayName("should cancel workflow run successfully")
    void shouldCancelWorkflowRunSuccessfully() throws Exception {
        UUID runId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        WorkflowRunResponse response = new WorkflowRunResponse(
                runId, UUID.randomUUID(), userId,
                "Test goal", WorkflowRunStatus.CANCELLED, 3, null,
                "trace-123", now, now, now);

        when(workflowRunService.cancel(runId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/workflows/runs/{runId}/cancel", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.finishedAt").isNotEmpty());
    }
}
