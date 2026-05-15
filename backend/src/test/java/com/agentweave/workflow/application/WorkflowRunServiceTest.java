package com.agentweave.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentweave.shared.exception.ResourceNotFoundException;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.shared.tracing.TraceIdProvider;
import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.domain.WorkflowRunStatus;
import com.agentweave.workflow.dto.CreateWorkflowRunRequest;
import com.agentweave.workflow.dto.WorkflowRunResponse;
import com.agentweave.workflow.repository.AgentStepRepository;
import com.agentweave.workflow.repository.WorkflowRunRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowRunService")
class WorkflowRunServiceTest {

    @Mock
    private WorkflowRunRepository workflowRunRepository;

    @Mock
    private AgentStepRepository agentStepRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private TraceIdProvider traceIdProvider;

    @InjectMocks
    private WorkflowRunService workflowRunService;

    private UUID userId;
    private CurrentUser currentUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        currentUser = new CurrentUser(userId, "testuser", "Test User", "USER");
    }

    @Test
    @DisplayName("should create workflow run successfully")
    void shouldCreateWorkflowRunSuccessfully() {
        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
        when(traceIdProvider.currentTraceId()).thenReturn("trace-123");
        when(workflowRunRepository.save(any(AgentRunEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateWorkflowRunRequest request = new CreateWorkflowRunRequest(
                UUID.randomUUID(),
                "Analyze payment API failure rate");

        WorkflowRunResponse response = workflowRunService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.goal()).isEqualTo("Analyze payment API failure rate");
        assertThat(response.status()).isEqualTo(WorkflowRunStatus.CREATED);
        assertThat(response.traceId()).isEqualTo("trace-123");

        verify(workflowRunRepository).save(any(AgentRunEntity.class));
    }

    @Test
    @DisplayName("should get workflow run successfully")
    void shouldGetWorkflowRunSuccessfully() {
        UUID runId = UUID.randomUUID();
        AgentRunEntity run = new AgentRunEntity(runId, userId, "Test goal");

        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
        when(workflowRunRepository.findById(runId)).thenReturn(Optional.of(run));

        WorkflowRunResponse response = workflowRunService.get(runId);

        assertThat(response).isNotNull();
        assertThat(response.runId()).isEqualTo(runId);
        assertThat(response.goal()).isEqualTo("Test goal");
    }

    @Test
    @DisplayName("should throw exception when run not found")
    void shouldThrowExceptionWhenRunNotFound() {
        UUID runId = UUID.randomUUID();

        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
        when(workflowRunRepository.findById(runId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workflowRunService.get(runId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Workflow run not found");
    }

    @Test
    @DisplayName("should throw exception when user has no access")
    void shouldThrowExceptionWhenUserHasNoAccess() {
        UUID runId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        AgentRunEntity run = new AgentRunEntity(runId, otherUserId, "Test goal");

        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
        when(workflowRunRepository.findById(runId)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> workflowRunService.get(runId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Workflow run not found");
    }

    @Test
    @DisplayName("should cancel workflow run successfully")
    void shouldCancelWorkflowRunSuccessfully() {
        UUID runId = UUID.randomUUID();
        AgentRunEntity run = new AgentRunEntity(runId, userId, "Test goal");

        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
        when(workflowRunRepository.findById(runId)).thenReturn(Optional.of(run));
        when(workflowRunRepository.save(any(AgentRunEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkflowRunResponse response = workflowRunService.cancel(runId);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(WorkflowRunStatus.CANCELLED);
        assertThat(response.finishedAt()).isNotNull();

        verify(workflowRunRepository).save(any(AgentRunEntity.class));
    }

    @Test
    @DisplayName("should throw exception when cancelling non-cancellable run")
    void shouldThrowExceptionWhenCancellingNonCancellableRun() {
        UUID runId = UUID.randomUUID();
        AgentRunEntity run = new AgentRunEntity(runId, userId, "Test goal");
        run.succeed("Final answer", java.time.Instant.now());

        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
        when(workflowRunRepository.findById(runId)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> workflowRunService.cancel(runId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid workflow status transition");
    }
}
