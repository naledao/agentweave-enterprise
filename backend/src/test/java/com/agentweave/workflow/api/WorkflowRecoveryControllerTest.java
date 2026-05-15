package com.agentweave.workflow.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agentweave.auth.repository.UserRepository;
import com.agentweave.langchain4j.agent.PlannerAgent;
import com.agentweave.shared.audit.AuditLogService;
import com.agentweave.shared.security.AgentWeaveUserDetailsService;
import com.agentweave.shared.security.JwtTokenService;
import com.agentweave.shared.tracing.TraceIdProvider;
import com.agentweave.workflow.application.PlanValidator;
import com.agentweave.workflow.application.WorkflowExecutionService;
import com.agentweave.workflow.application.WorkflowRecoveryService;
import com.agentweave.workflow.application.WorkflowRunService;
import com.agentweave.workflow.dto.WorkflowCheckpointResponse;
import com.agentweave.workflow.dto.WorkflowRunResponse;
import com.agentweave.workflow.domain.WorkflowRunStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkflowController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("WorkflowController recovery endpoints")
class WorkflowRecoveryControllerTest {

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
    @DisplayName("should get latest checkpoint")
    void shouldGetLatestCheckpoint() throws Exception {
        UUID runId = UUID.randomUUID();
        when(workflowRecoveryService.latestCheckpoint(runId)).thenReturn(new WorkflowCheckpointResponse(
                UUID.randomUUID(),
                runId,
                2,
                "tool_node",
                1,
                "checksum",
                true,
                null,
                null,
                Instant.now()));

        mockMvc.perform(get("/api/v1/workflows/runs/{runId}/checkpoints/latest", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId.toString()))
                .andExpect(jsonPath("$.nodeName").value("tool_node"));
    }

    @Test
    @DisplayName("should retry workflow run")
    void shouldRetryWorkflowRun() throws Exception {
        UUID runId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        WorkflowRunResponse response = new WorkflowRunResponse(
                runId, UUID.randomUUID(), userId,
                "Test goal", WorkflowRunStatus.EXECUTING, 1, null,
                "trace-123", now, null, now);

        when(workflowExecutionService.retryWorkflow(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/workflows/runs/{runId}/retry", runId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new com.agentweave.workflow.dto.WorkflowRetryRequest(1, "manual retry"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId.toString()))
                .andExpect(jsonPath("$.status").value("EXECUTING"));
    }
}
