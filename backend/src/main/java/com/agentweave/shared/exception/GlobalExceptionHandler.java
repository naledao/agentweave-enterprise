package com.agentweave.shared.exception;

import com.agentweave.shared.audit.AuditLogService;
import com.agentweave.shared.tracing.TraceIdProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final TraceIdProvider traceIdProvider;
    private final AuditLogService auditLogService;

    public GlobalExceptionHandler(TraceIdProvider traceIdProvider, AuditLogService auditLogService) {
        this.traceIdProvider = traceIdProvider;
        this.auditLogService = auditLogService;
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler({AccessDeniedBusinessException.class, AccessDeniedException.class})
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(Exception ex, HttpServletRequest request) {
        if (ex instanceof AccessDeniedException) {
            auditLogService.recordPermissionDenied(
                    "HTTP",
                    request.getRequestURI(),
                    request.getMethod(),
                    ex.getMessage());
            return build(
                    HttpStatus.FORBIDDEN,
                    ErrorCode.ACCESS_DENIED,
                    ErrorCode.ACCESS_DENIED.defaultMessage(),
                    request);
        }
        BusinessException businessException = (BusinessException) ex;
        return build(
                HttpStatus.FORBIDDEN,
                businessException.getErrorCode(),
                businessException.getMessage(),
                request);
    }

    @ExceptionHandler(ToolExecutionTimeoutException.class)
    public ResponseEntity<ApiErrorResponse> handleToolTimeout(
            ToolExecutionTimeoutException ex,
            HttpServletRequest request) {
        return build(HttpStatus.GATEWAY_TIMEOUT, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiErrorResponse> handleTooManyRequests(
            TooManyRequestsException ex,
            HttpServletRequest request) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(this::formatConstraintViolation)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.BUSINESS_ERROR, "Internal server error", request);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + " " + fieldError.getDefaultMessage();
    }

    private String formatConstraintViolation(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + " " + violation.getMessage();
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            ErrorCode errorCode,
            String message,
            HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(
                errorCode.code(),
                message,
                request.getRequestURI(),
                traceIdProvider.currentTraceId(request),
                Instant.now());
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}
