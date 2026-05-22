package com.neullabs.regulus.observability.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.neullabs.regulus.observability.audit.integrity.AuditChain;
import com.neullabs.regulus.observability.audit.integrity.SealedAuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central audit logging service for compliance events.
 * Supports multiple audit sinks (log, Kafka, database). When an
 * {@link AuditChain} is configured, every event is sealed into a
 * {@link SealedAuditEvent} before fan-out so sinks persist the tamper-evident
 * form. When no chain is configured, behaviour is identical to the
 * pre-integrity implementation.
 */
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final ObjectMapper objectMapper;
    private final List<AuditSink> sinks;
    private final Optional<AuditChain> chain;

    public AuditLogger() {
        this(null);
    }

    public AuditLogger(AuditChain chain) {
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.sinks = new CopyOnWriteArrayList<>();
        this.chain = Optional.ofNullable(chain);

        // Always log to structured logger
        sinks.add(new LoggingAuditSink());
    }

    public void addSink(AuditSink sink) {
        sinks.add(sink);
        log.info("Added audit sink: {}", sink.getClass().getSimpleName());
    }

    public Optional<AuditChain> chain() {
        return chain;
    }

    /**
     * Log an audit event to all configured sinks. When an {@link AuditChain}
     * is configured, the event is sealed first so each sink sees the same
     * tamper-evident wrapper via {@link AuditSink#writeSealed(SealedAuditEvent)}.
     */
    public void log(AuditEvent event) {
        SealedAuditEvent sealed = chain.map(c -> c.append(event)).orElse(null);
        for (AuditSink sink : sinks) {
            try {
                if (sealed != null) {
                    sink.writeSealed(sealed);
                } else {
                    sink.write(event);
                }
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
     * Audit sink interface for pluggable audit destinations. Sinks that want
     * to surface chain metadata should override {@link #writeSealed} —
     * the default delegates to {@link #write(AuditEvent)} so existing sinks
     * keep working when integrity is enabled.
     */
    public interface AuditSink {
        void write(AuditEvent event);

        default void writeSealed(SealedAuditEvent sealed) {
            write(sealed.event());
        }
    }

    /**
     * Default logging sink that writes to structured logger. When integrity
     * is enabled, the sealed wrapper (hash + chain index) is serialised so
     * the log line itself is the offline-verifiable record.
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

        @Override
        public void writeSealed(SealedAuditEvent sealed) {
            try {
                String json = objectMapper.writeValueAsString(sealed);
                auditLog.info(json);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize sealed audit event", e);
                write(sealed.event());
            }
        }
    }
}
