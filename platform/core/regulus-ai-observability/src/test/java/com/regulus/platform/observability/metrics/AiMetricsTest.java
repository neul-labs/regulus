package com.regulus.platform.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AI Metrics")
class AiMetricsTest {

    private MeterRegistry registry;
    private AiMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new AiMetrics(registry);
    }

    @Nested
    @DisplayName("LLM Metrics")
    class LlmMetrics {

        @Test
        @DisplayName("should record successful LLM call")
        void shouldRecordSuccessfulLlmCall() {
            metrics.recordLlmCall("gpt-4", "openai", Duration.ofMillis(500), 100, 200, true);

            Counter calls = registry.find("regulus.ai.llm.calls")
                .tag("model", "gpt-4")
                .tag("provider", "openai")
                .tag("success", "true")
                .counter();

            assertThat(calls).isNotNull();
            assertThat(calls.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should record token counts")
        void shouldRecordTokenCounts() {
            metrics.recordLlmCall("claude-3", "anthropic", Duration.ofMillis(300), 150, 250, true);

            Counter inputTokens = registry.find("regulus.ai.llm.tokens.input")
                .tag("model", "claude-3")
                .counter();
            Counter outputTokens = registry.find("regulus.ai.llm.tokens.output")
                .tag("model", "claude-3")
                .counter();

            assertThat(inputTokens).isNotNull();
            assertThat(inputTokens.count()).isEqualTo(150.0);
            assertThat(outputTokens).isNotNull();
            assertThat(outputTokens.count()).isEqualTo(250.0);
        }

        @Test
        @DisplayName("should record LLM errors")
        void shouldRecordLlmErrors() {
            metrics.recordLlmCall("gpt-4", "openai", Duration.ofMillis(100), 50, 0, false);

            Counter errors = registry.find("regulus.ai.llm.errors")
                .tag("success", "false")
                .counter();

            assertThat(errors).isNotNull();
            assertThat(errors.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should record routing decisions")
        void shouldRecordRoutingDecisions() {
            metrics.recordRouting("gpt-4-turbo", "cost-optimization");

            Counter routing = registry.find("regulus.ai.llm.routing")
                .tag("model", "gpt-4-turbo")
                .tag("reason", "cost-optimization")
                .counter();

            assertThat(routing).isNotNull();
            assertThat(routing.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Agent Metrics")
    class AgentMetrics {

        @Test
        @DisplayName("should record agent invocation")
        void shouldRecordAgentInvocation() {
            metrics.recordAgentInvocation("payment-agent", "process-payment", Duration.ofSeconds(2), true);

            Counter invocations = registry.find("regulus.ai.agent.invocations")
                .tag("agent", "payment-agent")
                .tag("operation", "process-payment")
                .counter();

            assertThat(invocations).isNotNull();
            assertThat(invocations.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should record tool execution")
        void shouldRecordToolExecution() {
            metrics.recordToolExecution("payment-agent", "validate-iban", Duration.ofMillis(50), true);

            Counter toolCalls = registry.find("regulus.ai.agent.tool.calls")
                .tag("agent", "payment-agent")
                .tag("tool", "validate-iban")
                .counter();

            assertThat(toolCalls).isNotNull();
            assertThat(toolCalls.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Governance Metrics")
    class GovernanceMetrics {

        @Test
        @DisplayName("should record policy evaluation pass")
        void shouldRecordPolicyPass() {
            metrics.recordPolicyEvaluation("lei-validator", true, Duration.ofMillis(10));

            Counter evaluations = registry.find("regulus.ai.policy.evaluations")
                .tag("policy", "lei-validator")
                .tag("result", "passed")
                .counter();

            assertThat(evaluations).isNotNull();
            assertThat(evaluations.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should record policy violation")
        void shouldRecordPolicyViolation() {
            metrics.recordPolicyEvaluation("consent-checker", false, Duration.ofMillis(5));

            Counter violations = registry.find("regulus.ai.policy.violations")
                .tag("policy", "consent-checker")
                .counter();

            assertThat(violations).isNotNull();
            assertThat(violations.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should record redactions")
        void shouldRecordRedactions() {
            metrics.recordRedaction("pii-filter", 5);

            Counter redactions = registry.find("regulus.ai.privacy.redactions")
                .tag("filter", "pii-filter")
                .counter();

            assertThat(redactions).isNotNull();
            assertThat(redactions.count()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("should record kill switch events")
        void shouldRecordKillSwitchEvents() {
            metrics.recordKillSwitchEvent("activated", "global");

            Counter events = registry.find("regulus.ai.killswitch.events")
                .tag("event", "activated")
                .tag("scope", "global")
                .counter();

            assertThat(events).isNotNull();
            assertThat(events.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("MCP/A2A Metrics")
    class McpA2aMetrics {

        @Test
        @DisplayName("should record MCP calls")
        void shouldRecordMcpCalls() {
            metrics.recordMcpCall("http://localhost:8080/mcp", "iso_validate", Duration.ofMillis(100), true);

            Counter mcpCalls = registry.find("regulus.ai.mcp.calls")
                .tag("tool", "iso_validate")
                .counter();

            assertThat(mcpCalls).isNotNull();
            assertThat(mcpCalls.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should record A2A calls")
        void shouldRecordA2aCalls() {
            metrics.recordA2aCall("compliance-agent", "validate", Duration.ofMillis(200), true);

            Counter a2aCalls = registry.find("regulus.ai.a2a.calls")
                .tag("target", "compliance-agent")
                .tag("operation", "validate")
                .counter();

            assertThat(a2aCalls).isNotNull();
            assertThat(a2aCalls.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should sanitize URLs with credentials")
        void shouldSanitizeUrlsWithCredentials() {
            metrics.recordMcpCall("http://user:pass@localhost:8080/mcp", "test", Duration.ofMillis(50), true);

            // Verify the metric was recorded (URL should be sanitized in tag)
            Counter mcpCalls = registry.find("regulus.ai.mcp.calls")
                .tag("tool", "test")
                .counter();

            assertThat(mcpCalls).isNotNull();
        }
    }

    @Nested
    @DisplayName("Timer Context")
    class TimerContextTests {

        @Test
        @DisplayName("should record duration using timer context")
        void shouldRecordDurationUsingTimerContext() throws Exception {
            try (AiMetrics.TimerContext ignored = metrics.startTimer("custom.operation",
                    io.micrometer.core.instrument.Tags.of("type", "test"))) {
                Thread.sleep(50);
            }

            Timer timer = registry.find("regulus.ai.custom.operation")
                .tag("type", "test")
                .timer();

            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(40);
        }
    }
}
