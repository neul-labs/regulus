package com.regulus.platform.grc.adapter;

import com.regulus.platform.grc.GrcEvidenceAdapter;
import com.regulus.platform.grc.GrcEvidenceEnvelope;

/**
 * Placeholder Kafka adapter — the Spring Boot starter substitutes a real
 * Spring-Kafka-backed implementation. This default is for non-Spring
 * callers who wire up their own producer.
 */
public final class KafkaAdapter implements GrcEvidenceAdapter {

    private final String topic;

    public KafkaAdapter(String topic) { this.topic = topic; }

    @Override public String vendorId() { return "kafka"; }

    @Override
    public void emit(GrcEvidenceEnvelope envelope) {
        throw new IllegalStateException(
                "KafkaAdapter requires the regulus-ai-adk-spring-boot-starter or a custom "
                + "GrcEvidenceAdapter implementation. Topic was: " + topic);
    }
}
