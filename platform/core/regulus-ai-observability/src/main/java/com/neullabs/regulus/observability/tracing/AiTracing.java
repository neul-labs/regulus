package com.neullabs.regulus.observability.tracing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Supplier;

/**
 * OpenTelemetry-based distributed tracing for AI operations.
 */
public class AiTracing {

    private static final Logger log = LoggerFactory.getLogger(AiTracing.class);

    private static final String INSTRUMENTATION_NAME = "regulus-ai";

    private final Tracer tracer;

    public AiTracing() {
        this.tracer = GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME);
        log.info("AiTracing initialized with OpenTelemetry tracer");
    }

    public AiTracing(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Create a span for an LLM call.
     */
    public SpanContext startLlmCall(String model, String provider) {
        Span span = tracer.spanBuilder("llm.call")
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute("ai.model", model)
            .setAttribute("ai.provider", provider)
            .setAttribute("ai.operation", "inference")
            .startSpan();

        return new SpanContext(span);
    }

    /**
     * Create a span for an agent invocation.
     */
    public SpanContext startAgentInvocation(String agentId, String operation) {
        Span span = tracer.spanBuilder("agent.invoke")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("ai.agent.id", agentId)
            .setAttribute("ai.agent.operation", operation)
            .startSpan();

        return new SpanContext(span);
    }

    /**
     * Create a span for a tool execution.
     */
    public SpanContext startToolExecution(String toolName, String agentId) {
        Span span = tracer.spanBuilder("tool.execute")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("ai.tool.name", toolName)
            .setAttribute("ai.agent.id", agentId)
            .startSpan();

        return new SpanContext(span);
    }

    /**
     * Create a span for MCP call.
     */
    public SpanContext startMcpCall(String serverUrl, String toolName) {
        Span span = tracer.spanBuilder("mcp.call")
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute("mcp.server.url", serverUrl)
            .setAttribute("mcp.tool.name", toolName)
            .startSpan();

        return new SpanContext(span);
    }

    /**
     * Create a span for A2A call.
     */
    public SpanContext startA2aCall(String targetAgent, String operation) {
        Span span = tracer.spanBuilder("a2a.call")
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute("a2a.target.agent", targetAgent)
            .setAttribute("a2a.operation", operation)
            .startSpan();

        return new SpanContext(span);
    }

    /**
     * Create a span for policy evaluation.
     */
    public SpanContext startPolicyEvaluation(String policyName) {
        Span span = tracer.spanBuilder("policy.evaluate")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("policy.name", policyName)
            .startSpan();

        return new SpanContext(span);
    }

    /**
     * Execute a traced operation.
     */
    public <T> T traced(String spanName, Map<String, String> attributes, Supplier<T> operation) {
        var spanBuilder = tracer.spanBuilder(spanName)
            .setSpanKind(SpanKind.INTERNAL);

        for (Map.Entry<String, String> attr : attributes.entrySet()) {
            spanBuilder.setAttribute(attr.getKey(), attr.getValue());
        }

        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            T result = operation.get();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Wrapper for span management.
     */
    public static class SpanContext implements AutoCloseable {
        private final Span span;
        private final Scope scope;

        SpanContext(Span span) {
            this.span = span;
            this.scope = span.makeCurrent();
        }

        public void setAttribute(String key, String value) {
            span.setAttribute(key, value);
        }

        public void setAttribute(String key, long value) {
            span.setAttribute(key, value);
        }

        public void setAttribute(String key, boolean value) {
            span.setAttribute(key, value);
        }

        public void setSuccess() {
            span.setStatus(StatusCode.OK);
        }

        public void setError(String message) {
            span.setStatus(StatusCode.ERROR, message);
        }

        public void recordException(Throwable exception) {
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR, exception.getMessage());
        }

        public String getTraceId() {
            return span.getSpanContext().getTraceId();
        }

        public String getSpanId() {
            return span.getSpanContext().getSpanId();
        }

        @Override
        public void close() {
            scope.close();
            span.end();
        }
    }
}
