package com.regulus.platform.observability.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Audit Logger")
class AuditLoggerTest {

    private AuditLogger auditLogger;
    private TestAuditSink testSink;

    @BeforeEach
    void setUp() {
        auditLogger = new AuditLogger();
        testSink = new TestAuditSink();
        auditLogger.addSink(testSink);
    }

    @Nested
    @DisplayName("basic logging")
    class BasicLogging {

        @Test
        @DisplayName("should log event to all sinks")
        void shouldLogEventToAllSinks() {
            AuditEvent event = AuditEvent.builder()
                .type(AuditEvent.EventType.LLM_CALL)
                .correlationId("corr-123")
                .operation("test.operation")
                .build();

            auditLogger.log(event);

            assertThat(testSink.events).hasSize(1);
            assertThat(testSink.events.get(0).correlationId()).isEqualTo("corr-123");
        }

        @Test
        @DisplayName("should generate event ID if not provided")
        void shouldGenerateEventIdIfNotProvided() {
            AuditEvent event = AuditEvent.builder()
                .type(AuditEvent.EventType.AGENT_INVOCATION)
                .operation("agent.invoke")
                .build();

            auditLogger.log(event);

            assertThat(testSink.events.get(0).eventId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("LLM call logging")
    class LlmCallLogging {

        @Test
        @DisplayName("should log successful LLM call")
        void shouldLogSuccessfulLlmCall() {
            auditLogger.logLlmCall(
                "corr-456", "user-1", "gpt-4", "openai",
                100, 200, 500, true
            );

            assertThat(testSink.events).hasSize(1);
            AuditEvent event = testSink.events.get(0);

            assertThat(event.type()).isEqualTo(AuditEvent.EventType.LLM_CALL);
            assertThat(event.correlationId()).isEqualTo("corr-456");
            assertThat(event.userId()).isEqualTo("user-1");
            assertThat(event.resource()).isEqualTo("gpt-4");
            assertThat(event.outcome()).isEqualTo(AuditEvent.Outcome.SUCCESS);
            assertThat(event.details()).containsEntry("provider", "openai");
            assertThat(event.details()).containsEntry("inputTokens", 100);
            assertThat(event.details()).containsEntry("outputTokens", 200);
        }

        @Test
        @DisplayName("should log failed LLM call")
        void shouldLogFailedLlmCall() {
            auditLogger.logLlmCall(
                "corr-789", "user-2", "claude-3", "anthropic",
                50, 0, 100, false
            );

            AuditEvent event = testSink.events.get(0);
            assertThat(event.outcome()).isEqualTo(AuditEvent.Outcome.FAILURE);
        }
    }

    @Nested
    @DisplayName("policy violation logging")
    class PolicyViolationLogging {

        @Test
        @DisplayName("should log policy violation")
        void shouldLogPolicyViolation() {
            auditLogger.logPolicyViolation(
                "corr-111", "user-3", "consent-policy",
                "MISSING_CONSENT", "User has not provided consent for marketing"
            );

            assertThat(testSink.events).hasSize(1);
            AuditEvent event = testSink.events.get(0);

            assertThat(event.type()).isEqualTo(AuditEvent.EventType.POLICY_VIOLATION);
            assertThat(event.resource()).isEqualTo("consent-policy");
            assertThat(event.outcome()).isEqualTo(AuditEvent.Outcome.BLOCKED);
            assertThat(event.message()).contains("consent for marketing");
            assertThat(event.details()).containsEntry("violationType", "MISSING_CONSENT");
        }
    }

    @Nested
    @DisplayName("kill switch logging")
    class KillSwitchLogging {

        @Test
        @DisplayName("should log kill switch activation")
        void shouldLogKillSwitchActivation() {
            auditLogger.logKillSwitchActivation(
                "corr-222", "admin-user", "global",
                "Emergency shutdown due to detected anomaly"
            );

            assertThat(testSink.events).hasSize(1);
            AuditEvent event = testSink.events.get(0);

            assertThat(event.type()).isEqualTo(AuditEvent.EventType.KILL_SWITCH_ACTIVATED);
            assertThat(event.userId()).isEqualTo("admin-user");
            assertThat(event.resource()).isEqualTo("global");
            assertThat(event.outcome()).isEqualTo(AuditEvent.Outcome.SUCCESS);
            assertThat(event.message()).contains("Emergency shutdown");
        }
    }

    @Nested
    @DisplayName("sink error handling")
    class SinkErrorHandling {

        @Test
        @DisplayName("should continue logging when one sink fails")
        void shouldContinueWhenSinkFails() {
            FailingSink failingSink = new FailingSink();
            auditLogger.addSink(failingSink);

            AuditEvent event = AuditEvent.builder()
                .type(AuditEvent.EventType.DATA_ACCESS)
                .operation("data.read")
                .build();

            // Should not throw
            auditLogger.log(event);

            // Test sink should still receive the event
            assertThat(testSink.events).hasSize(1);
        }
    }

    @Nested
    @DisplayName("AuditEvent builder")
    class AuditEventBuilder {

        @Test
        @DisplayName("should build event with all fields")
        void shouldBuildEventWithAllFields() {
            AuditEvent event = AuditEvent.builder()
                .eventId("evt-123")
                .type(AuditEvent.EventType.TOOL_EXECUTION)
                .correlationId("corr-123")
                .userId("user-1")
                .agentId("agent-1")
                .operation("tool.execute")
                .resource("iso-validator")
                .outcome(AuditEvent.Outcome.SUCCESS)
                .message("Tool executed successfully")
                .details(java.util.Map.of("duration", 100))
                .metadata(java.util.Map.of("env", "prod"))
                .build();

            assertThat(event.eventId()).isEqualTo("evt-123");
            assertThat(event.type()).isEqualTo(AuditEvent.EventType.TOOL_EXECUTION);
            assertThat(event.agentId()).isEqualTo("agent-1");
            assertThat(event.metadata()).containsEntry("env", "prod");
        }

        @Test
        @DisplayName("should default outcome to SUCCESS")
        void shouldDefaultOutcomeToSuccess() {
            AuditEvent event = AuditEvent.builder()
                .type(AuditEvent.EventType.MCP_CALL)
                .build();

            assertThat(event.outcome()).isEqualTo(AuditEvent.Outcome.SUCCESS);
        }

        @Test
        @DisplayName("should set timestamp automatically")
        void shouldSetTimestampAutomatically() {
            AuditEvent event = AuditEvent.builder()
                .type(AuditEvent.EventType.A2A_CALL)
                .build();

            assertThat(event.timestamp()).isNotNull();
        }
    }

    // Test helpers
    private static class TestAuditSink implements AuditLogger.AuditSink {
        final List<AuditEvent> events = new ArrayList<>();

        @Override
        public void write(AuditEvent event) {
            events.add(event);
        }
    }

    private static class FailingSink implements AuditLogger.AuditSink {
        @Override
        public void write(AuditEvent event) {
            throw new RuntimeException("Simulated sink failure");
        }
    }
}
