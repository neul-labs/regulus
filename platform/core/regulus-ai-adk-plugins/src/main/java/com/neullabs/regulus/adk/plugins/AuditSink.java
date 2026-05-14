package com.neullabs.regulus.adk.plugins;

import java.util.Map;

/**
 * Where audit events go. Implementations must be safe to call concurrently
 * and must not lose events on transient failure (buffer + retry on the
 * implementation side; or fail-loud if the audit guarantee can't be met).
 */
public interface AuditSink {

    void emit(Map<String, Object> event);

    static AuditSink kafka(String topic) {
        return new KafkaAuditSink(topic);
    }

    static AuditSink stdout() {
        return event -> System.out.println("[regulus-audit] " + event);
    }
}
