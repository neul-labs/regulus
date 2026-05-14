package com.neullabs.regulus.adk.plugins;

import java.util.Map;

/**
 * Placeholder Kafka sink. The Spring Boot starter substitutes a Spring-Kafka
 * backed implementation; this default implementation is for non-Spring callers
 * who wire up their own producer.
 */
final class KafkaAuditSink implements AuditSink {

    private final String topic;

    KafkaAuditSink(String topic) {
        this.topic = topic;
    }

    @Override
    public void emit(Map<String, Object> event) {
        // The actual KafkaTemplate / producer wiring is provided by the Spring
        // auto-config in regulus-ai-adk-spring-boot-starter, which replaces
        // this sink with a fully-wired implementation at runtime.
        throw new IllegalStateException(
                "KafkaAuditSink requires the regulus-ai-adk-spring-boot-starter "
                + "or a custom AuditSink implementation. Topic was: " + topic);
    }
}
