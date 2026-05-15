package com.agentweave.workflow.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agentweave.auth.repository.UserRepository;
import com.agentweave.shared.audit.AuditLogService;
import com.agentweave.shared.security.AgentWeaveUserDetailsService;
import com.agentweave.shared.security.JwtTokenService;
import com.agentweave.shared.tracing.TraceIdProvider;
import com.agentweave.tool.domain.ToolRiskLevel;
import com.agentweave.workflow.application.WorkflowApprovalService;
import com.agentweave.workflow.application.WorkflowExecutionService;
import com.agentweave.workflow.domain.WorkflowApprovalStatus;
import com.agentweave.workflow.dto.ApprovalDecisionRequest;
import com.agentweave.workflow.dto.WorkflowApprovalResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkflowApprovalController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("WorkflowApprovalController")
class WorkflowApprovalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WorkflowApprovalService workflowApprovalService;

    @MockitoBean
    private WorkflowExecutionService workflowExecutionService;

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
    @DisplayName("should approve workflow approval and resume run")
    void shouldApproveWorkflowApprovalAndResumeRun() throws Exception {
        UUID approvalId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        WorkflowApprovalResponse response = new WorkflowApprovalResponse(
                approvalId,
                runId,
                UUID.randomUUID(),
                1,
                "tool:log:search",
                ToolRiskLevel.HIGH,
                "masked",
                WorkflowApprovalStatus.APPROVED,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ok",
                Instant.now(),
                Instant.now());
        when(workflowApprovalService.approve(approvalId, new ApprovalDecisionRequest("ok")))
                .thenReturn(new WorkflowApprovalService.ApprovalDecision(response, true));

        mockMvc.perform(post("/api/v1/workflows/approvals/{approvalId}/approve", approvalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovalDecisionRequest("ok"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalId").value(approvalId.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(workflowExecutionService).resumeAfterApproval(runId, approvalId);
    }
}
