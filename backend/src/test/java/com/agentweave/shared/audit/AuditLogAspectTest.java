package com.agentweave.shared.audit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentweave.shared.exception.AccessDeniedBusinessException;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

class AuditLogAspectTest {

    private final AuditLogService auditLogService = org.mockito.Mockito.mock(AuditLogService.class);
    private final CurrentUserService currentUserService = org.mockito.Mockito.mock(CurrentUserService.class);
    private final AuditSummarySanitizer sanitizer = new AuditSummarySanitizer(new ObjectMapper());

    @Test
    void auditedMethodWritesSuccessLogWithoutChangingReturnValue() {
        UUID userId = UUID.randomUUID();
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(new CurrentUser(
                userId,
                "alice",
                "Alice",
                Set.of("USER"),
                Set.of())));
        SampleService proxy = proxy();

        String result = proxy.echo(new SampleRequest("doc-1", "secret-password"));

        org.assertj.core.api.Assertions.assertThat(result).isEqualTo("ok-doc-1");
        ArgumentCaptor<AuditLogCommand> captor = ArgumentCaptor.forClass(AuditLogCommand.class);
        verify(auditLogService).record(captor.capture());
        AuditLogCommand command = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(command.eventType()).isEqualTo(AuditEventType.DOCUMENT_UPLOAD);
        org.assertj.core.api.Assertions.assertThat(command.userId()).isEqualTo(userId);
        org.assertj.core.api.Assertions.assertThat(command.username()).isEqualTo("alice");
        org.assertj.core.api.Assertions.assertThat(command.resourceType()).isEqualTo("document");
        org.assertj.core.api.Assertions.assertThat(command.resourceId()).isEqualTo("doc-1");
        org.assertj.core.api.Assertions.assertThat(command.result()).isEqualTo(AuditResult.SUCCESS);
        org.assertj.core.api.Assertions.assertThat(command.durationMs()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(command.requestSummary()).contains("\"password\":\"******\"");
        org.assertj.core.api.Assertions.assertThat(command.requestSummary()).doesNotContain("secret-password");
        org.assertj.core.api.Assertions.assertThat(command.responseSummary()).contains("ok-doc-1");
    }

    @Test
    void auditedMethodWritesFailureLogAndRethrows() {
        when(currentUserService.getCurrentUser()).thenReturn(Optional.empty());
        SampleService proxy = proxy();

        assertThatThrownBy(() -> proxy.fail(new SampleRequest("doc-2", "secret-password")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("failure token=abc123");

        ArgumentCaptor<AuditLogCommand> captor = ArgumentCaptor.forClass(AuditLogCommand.class);
        verify(auditLogService).record(captor.capture());
        AuditLogCommand command = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(command.result()).isEqualTo(AuditResult.FAILURE);
        org.assertj.core.api.Assertions.assertThat(command.resourceId()).isEqualTo("doc-2");
        org.assertj.core.api.Assertions.assertThat(command.errorMessage()).contains("token=******");
        org.assertj.core.api.Assertions.assertThat(command.errorMessage()).doesNotContain("abc123");
    }

    @Test
    void auditedMethodClassifiesAccessDeniedAsDenied() {
        when(currentUserService.getCurrentUser()).thenReturn(Optional.empty());
        SampleService proxy = proxy();

        assertThatThrownBy(() -> proxy.deny(new SampleRequest("doc-3", "secret-password")))
                .isInstanceOf(AccessDeniedBusinessException.class);

        ArgumentCaptor<AuditLogCommand> captor = ArgumentCaptor.forClass(AuditLogCommand.class);
        verify(auditLogService).record(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().result()).isEqualTo(AuditResult.DENIED);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().errorMessage()).contains("denied");
    }

    private SampleService proxy() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new SampleService());
        factory.addAspect(new AuditLogAspect(auditLogService, currentUserService, sanitizer));
        return factory.getProxy();
    }

    static class SampleService {

        @AuditLog(
                eventType = AuditEventType.DOCUMENT_UPLOAD,
                resourceType = "document",
                resourceId = "#p0.documentId")
        String echo(SampleRequest request) {
            return "ok-" + request.documentId();
        }

        @AuditLog(
                eventType = AuditEventType.DOCUMENT_UPLOAD,
                resourceType = "document",
                resourceId = "#p0.documentId")
        String fail(SampleRequest request) {
            throw new IllegalStateException("failure token=abc123");
        }

        @AuditLog(
                eventType = AuditEventType.DOCUMENT_UPLOAD,
                resourceType = "document",
                resourceId = "#p0.documentId")
        String deny(SampleRequest request) {
            throw new AccessDeniedBusinessException("denied");
        }
    }

    record SampleRequest(String documentId, String password) {
    }
}
