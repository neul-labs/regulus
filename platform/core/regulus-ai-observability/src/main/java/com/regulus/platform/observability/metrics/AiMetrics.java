package com.regulus.platform.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Central metrics registry for AI operations.
 * Provides Micrometer-based metrics for LLM calls, agent operations, and governance.
 */
public class AiMetrics {

    private static final Logger log = LoggerFactory.getLogger(AiMetrics.class);

    private static final String PREFIX = "regulus.ai";

    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    public AiMetrics(MeterRegistry registry) {
        this.registry = registry;
        log.info("AiMetrics initialized with registry: {}", registry.getClass().getSimpleName());
    }

    // ==================== LLM Metrics ====================

    /**
     * Record an LLM call with timing and token counts.
     */
    public void recordLlmCall(String model, String provider, Duration duration,
                              int inputTokens, int outputTokens, boolean success) {
        Tags tags = Tags.of(
            "model", model,
            "provider", provider,
            "success", String.valueOf(success)
        );

        // Call duration
        getTimer("llm.call.duration", tags).record(duration);

        // Token counters
        getCounter("llm.tokens.input", tags).increment(inputTokens);
        getCounter("llm.tokens.output", tags).increment(outputTokens);

        // Call count
        getCounter("llm.calls", tags).increment();

        if (!success) {
            getCounter("llm.errors", tags).increment();
        }
    }

    /**
     * Record model routing decision.
     */
    public void recordRouting(String selectedModel, String routingReason) {
        Tags tags = Tags.of(
            "model", selectedModel,
            "reason", routingReason
        );
        getCounter("llm.routing", tags).increment();
    }

    // ==================== Agent Metrics ====================

    /**
     * Record an agent invocation.
     */
    public void recordAgentInvocation(String agentId, String operation,
                                       Duration duration, boolean success) {
        Tags tags = Tags.of(
            "agent", agentId,
            "operation", operation,
            "success", String.valueOf(success)
        );

        getTimer("agent.invocation.duration", tags).record(duration);
        getCounter("agent.invocations", tags).increment();

        if (!success) {
            getCounter("agent.errors", tags).increment();
        }
    }

    /**
     * Record tool execution within an agent.
     */
    public void recordToolExecution(String agentId, String toolName,
                                     Duration duration, boolean success) {
        Tags tags = Tags.of(
            "agent", agentId,
            "tool", toolName,
            "success", String.valueOf(success)
        );

        getTimer("agent.tool.duration", tags).record(duration);
        getCounter("agent.tool.calls", tags).increment();
    }

    // ==================== Governance Metrics ====================

    /**
     * Record a policy evaluation.
     */
    public void recordPolicyEvaluation(String policyName, boolean passed, Duration duration) {
        Tags tags = Tags.of(
            "policy", policyName,
            "result", passed ? "passed" : "violated"
        );

        getTimer("policy.evaluation.duration", tags).record(duration);
        getCounter("policy.evaluations", tags).increment();

        if (!passed) {
            getCounter("policy.violations", Tags.of("policy", policyName)).increment();
        }
    }

    /**
     * Record privacy redaction.
     */
    public void recordRedaction(String filterName, int fieldsRedacted) {
        Tags tags = Tags.of("filter", filterName);

        getCounter("privacy.redactions", tags).increment(fieldsRedacted);
        getCounter("privacy.filter.invocations", tags).increment();
    }

    /**
     * Record kill switch event.
     */
    public void recordKillSwitchEvent(String eventType, String scope) {
        Tags tags = Tags.of(
            "event", eventType,
            "scope", scope
        );
        getCounter("killswitch.events", tags).increment();
    }

    // ==================== MCP/A2A Metrics ====================

    /**
     * Record MCP tool call.
     */
    public void recordMcpCall(String serverUrl, String toolName,
                               Duration duration, boolean success) {
        Tags tags = Tags.of(
            "server", sanitizeUrl(serverUrl),
            "tool", toolName,
            "success", String.valueOf(success)
        );

        getTimer("mcp.call.duration", tags).record(duration);
        getCounter("mcp.calls", tags).increment();
    }

    /**
     * Record A2A agent communication.
     */
    public void recordA2aCall(String targetAgent, String operation,
                               Duration duration, boolean success) {
        Tags tags = Tags.of(
            "target", targetAgent,
            "operation", operation,
            "success", String.valueOf(success)
        );

        getTimer("a2a.call.duration", tags).record(duration);
        getCounter("a2a.calls", tags).increment();
    }

    // ==================== Helper Methods ====================

    private Timer getTimer(String name, Tags tags) {
        String key = name + tags.toString();
        return timers.computeIfAbsent(key, k ->
            Timer.builder(PREFIX + "." + name)
                .tags(tags)
                .description("Timer for " + name)
                .register(registry));
    }

    private Counter getCounter(String name, Tags tags) {
        String key = name + tags.toString();
        return counters.computeIfAbsent(key, k ->
            Counter.builder(PREFIX + "." + name)
                .tags(tags)
                .description("Counter for " + name)
                .register(registry));
    }

    private String sanitizeUrl(String url) {
        // Remove credentials and query params from URL for tagging
        if (url == null) return "unknown";
        return url.replaceAll("://[^@]+@", "://***@")
                  .replaceAll("\\?.*", "");
    }

    /**
     * Timer context for recording operation duration.
     */
    public static class TimerContext implements AutoCloseable {
        private final long startTime;
        private final Timer timer;

        public TimerContext(Timer timer) {
            this.timer = timer;
            this.startTime = System.nanoTime();
        }

        @Override
        public void close() {
            timer.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }
    }

    public TimerContext startTimer(String name, Tags tags) {
        return new TimerContext(getTimer(name, tags));
    }
}
