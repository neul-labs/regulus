package com.regulus.platform.agents.web;

import com.regulus.platform.killswitch.interceptor.KillSwitchException;
import com.regulus.platform.policy.guard.PolicyViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for consistent error responses.
 * All exceptions are logged with correlation IDs for traceability.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PolicyViolationException.class)
    public ResponseEntity<ErrorResponse> handlePolicyViolation(
            PolicyViolationException ex, WebRequest request) {

        String correlationId = getOrCreateCorrelationId();
        log.warn("Policy violation [{}]: {}", correlationId, ex.getMessage());

        var firstViolation = ex.getFirstViolation();
        ErrorResponse response = ErrorResponse.builder()
            .errorCode("POLICY_VIOLATION")
            .message("Request blocked by policy: " + ex.getMessage())
            .correlationId(correlationId)
            .timestamp(Instant.now())
            .details(Map.of(
                "policyName", firstViolation != null ? firstViolation.policyName() : "unknown",
                "violationType", firstViolation != null ? firstViolation.violationType() : "unknown"
            ))
            .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(KillSwitchException.class)
    public ResponseEntity<ErrorResponse> handleKillSwitch(
            KillSwitchException ex, WebRequest request) {

        String correlationId = getOrCreateCorrelationId();
        log.error("Kill switch active [{}]: {}", correlationId, ex.getMessage());

        var state = ex.getState();
        ErrorResponse response = ErrorResponse.builder()
            .errorCode("KILL_SWITCH_ACTIVE")
            .message("Service temporarily disabled")
            .correlationId(correlationId)
            .timestamp(Instant.now())
            .details(Map.of(
                "scope", state != null && state.scope() != null ? state.scope().name() : "GLOBAL",
                "reason", state != null && state.reason() != null ? state.reason() : "Emergency shutdown"
            ))
            .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(LlmProviderException.class)
    public ResponseEntity<ErrorResponse> handleLlmError(
            LlmProviderException ex, WebRequest request) {

        String correlationId = getOrCreateCorrelationId();
        log.error("LLM provider error [{}]: {}", correlationId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
            .errorCode("LLM_ERROR")
            .message("AI service temporarily unavailable")
            .correlationId(correlationId)
            .timestamp(Instant.now())
            .details(Map.of(
                "provider", ex.getProvider() != null ? ex.getProvider() : "unknown"
            ))
            .build();

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            IllegalArgumentException ex, WebRequest request) {

        String correlationId = getOrCreateCorrelationId();
        log.warn("Bad request [{}]: {}", correlationId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
            .errorCode("BAD_REQUEST")
            .message(ex.getMessage())
            .correlationId(correlationId)
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        String correlationId = getOrCreateCorrelationId();
        log.error("Unexpected error [{}]", correlationId, ex);

        ErrorResponse response = ErrorResponse.builder()
            .errorCode("INTERNAL_ERROR")
            .message("An unexpected error occurred")
            .correlationId(correlationId)
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String getOrCreateCorrelationId() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }
        return correlationId;
    }

    /**
     * Standardized error response.
     */
    public record ErrorResponse(
        String errorCode,
        String message,
        String correlationId,
        Instant timestamp,
        Map<String, Object> details
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String errorCode;
            private String message;
            private String correlationId;
            private Instant timestamp;
            private Map<String, Object> details = Map.of();

            public Builder errorCode(String errorCode) {
                this.errorCode = errorCode;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder correlationId(String correlationId) {
                this.correlationId = correlationId;
                return this;
            }

            public Builder timestamp(Instant timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder details(Map<String, Object> details) {
                this.details = details;
                return this;
            }

            public ErrorResponse build() {
                return new ErrorResponse(errorCode, message, correlationId, timestamp, details);
            }
        }
    }

    /**
     * Exception for LLM provider errors.
     */
    public static class LlmProviderException extends RuntimeException {
        private final String provider;

        public LlmProviderException(String provider, String message) {
            super(message);
            this.provider = provider;
        }

        public LlmProviderException(String provider, String message, Throwable cause) {
            super(message, cause);
            this.provider = provider;
        }

        public String getProvider() {
            return provider;
        }
    }
}
