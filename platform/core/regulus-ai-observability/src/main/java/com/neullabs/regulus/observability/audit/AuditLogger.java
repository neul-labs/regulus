package com.neullabs.regulus.observability.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central audit logging service for compliance events.
 * Supports multiple audit sinks (log, Kafka, database).
 */
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final ObjectMapper objectMapper;
    private final List<AuditSink> sinks;

    public AuditLogger() {
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.sinks = new CopyOnWriteArrayList<>();

        // Always log to structured logger
        sinks.add(new LoggingAuditSink());
    }

    public void addSink(AuditSink sink) {
        sinks.add(sink);
        log.info("Added audit sink: {}", sink.getClass().getSimpleName());
    }

    /**
     * Log an audit event to all configured sinks.
     */
    public void log(AuditEvent event) {
        for (AuditSink sink : sinks) {
            try {
                sink.write(event);
            } catch (Exception e) {
                log.error("Failed to write audit event to sink {}: {}",
                    sink.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * Log an LLM call event.
     */
    public void logLlmCall(String correlationId, String userId, String model,
                           String provider, int inputTokens, int outputTokens,
                           long durationMs, boolean success) {
        log(AuditEvent.builder()
            .type(AuditEvent.EventType.LLM_CALL)
            .correlationId(correlationId)
            .userId(userId)
            .operation("llm.call")
            .resource(model)
            .outcome(success ? AuditEvent.Outcome.SUCCESS : AuditEvent.Outcome.FAILURE)
            .details(java.util.Map.of(
                "provider", provider,
                "inputTokens", inputTokens,
                "outputTokens", outputTokens,
                "durationMs", durationMs
            ))
            .build());
    }

    /**
     * Log a policy violation event.
     */
    public void logPolicyViolation(String correlationId, String userId,
                                    String policyName, String violationType,
                                    String message) {
        log(AuditEvent.builder()
            .type(AuditEvent.EventType.POLICY_VIOLATION)
            .correlationId(correlationId)
            .userId(userId)
            .operation("policy.evaluate")
            .resource(policyName)
            .outcome(AuditEvent.Outcome.BLOCKED)
            .message(message)
            .details(java.util.Map.of(
                "violationType", violationType
            ))
            .build());
    }

    /**
     * Log a kill switch activation event.
     */
    public void logKillSwitchActivation(String correlationId, String activatedBy,
                                         String scope, String reason) {
        log(AuditEvent.builder()
            .type(AuditEvent.EventType.KILL_SWITCH_ACTIVATED)
            .correlationId(correlationId)
            .userId(activatedBy)
            .operation("killswitch.activate")
            .resource(scope)
            .outcome(AuditEvent.Outcome.SUCCESS)
            .message(reason)
            .build());
    }

    /**
     * Audit sink interface for pluggable audit destinations.
     */
    public interface AuditSink {
        void write(AuditEvent event);
    }

    /**
     * Default logging sink that writes to structured logger.
     */
    private class LoggingAuditSink implements AuditSink {
        @Override
        public void write(AuditEvent event) {
            try {
                String json = objectMapper.writeValueAsString(event);
                auditLog.info(json);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize audit event", e);
                auditLog.info("type={} correlationId={} outcome={}",
                    event.type(), event.correlationId(), event.outcome());
            }
        }
    }
}
