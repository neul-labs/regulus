package com.neullabs.regulus.adk.plugins;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditSinkTest {

    @Test
    void stdoutSinkDoesNotThrow() {
        AuditSink sink = AuditSink.stdout();
        // Capture wouldn't add value here — just verify no exception.
        sink.emit(Map.of(
                "event_id", "01J6X4ABCDEFG",
                "actor", "user:42",
                "action", "model-call"));
    }

    @Test
    void kafkaSinkPlaceholderThrowsWhenInvokedWithoutSpringWiring() {
        AuditSink sink = AuditSink.kafka("audit.regulus.v1");
        assertThatThrownBy(() -> sink.emit(Map.of("event_id", "x")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("regulus-ai-adk-spring-boot-starter");
    }

    @Test
    void kafkaSinkVendorIdMessageIncludesTopic() {
        AuditSink sink = AuditSink.kafka("audit.custom.v9");
        assertThatThrownBy(() -> sink.emit(Map.of()))
                .hasMessageContaining("audit.custom.v9");
    }
}
