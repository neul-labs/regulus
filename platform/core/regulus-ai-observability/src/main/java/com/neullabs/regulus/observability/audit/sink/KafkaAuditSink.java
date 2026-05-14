package com.neullabs.regulus.observability.audit.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.neullabs.regulus.observability.audit.AuditEvent;
import com.neullabs.regulus.observability.audit.AuditLogger.AuditSink;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Kafka audit sink for durable audit event storage.
 * Publishes audit events to Kafka with exactly-once semantics.
 */
public class KafkaAuditSink implements AuditSink, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KafkaAuditSink.class);

    private final KafkaProducer<String, String> producer;
    private final String topic;
    private final String dlqTopic;
    private final ObjectMapper objectMapper;
    private final AtomicLong publishedCount = new AtomicLong(0);
    private final AtomicLong failedCount = new AtomicLong(0);

    public KafkaAuditSink(KafkaAuditSinkConfig config) {
        this.topic = config.getTopic();
        this.dlqTopic = config.getDlqTopic();

        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Reliability settings
        props.put(ProducerConfig.ACKS_CONFIG, config.getAcks());
        props.put(ProducerConfig.RETRIES_CONFIG, config.getRetries());
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, config.isIdempotent());

        // Performance settings
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, config.getBatchSize());
        props.put(ProducerConfig.LINGER_MS_CONFIG, config.getLingerMs());
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, config.getBufferMemory());
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, config.getCompressionType());

        // Client ID for monitoring
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "regulus-audit-producer");

        this.producer = new KafkaProducer<>(props);

        log.info("Kafka audit sink initialized: topic={}, dlq={}, servers={}",
            topic, dlqTopic, config.getBootstrapServers());
    }

    @Override
    public void write(AuditEvent event) {
        try {
            String key = event.correlationId();
            String value = objectMapper.writeValueAsString(event);

            RecordHeaders headers = new RecordHeaders();
            headers.add("event-type", event.type().name().getBytes(StandardCharsets.UTF_8));
            headers.add("timestamp", String.valueOf(event.timestamp().toEpochMilli()).getBytes(StandardCharsets.UTF_8));
            if (event.userId() != null) {
                headers.add("user-id", event.userId().getBytes(StandardCharsets.UTF_8));
            }

            ProducerRecord<String, String> record = new ProducerRecord<>(
                topic, null, event.timestamp().toEpochMilli(), key, value, headers
            );

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    handlePublishFailure(event, value, exception);
                } else {
                    publishedCount.incrementAndGet();
                    log.debug("Audit event published: topic={}, partition={}, offset={}",
                        metadata.topic(), metadata.partition(), metadata.offset());
                }
            });

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit event: {}", e.getMessage());
            failedCount.incrementAndGet();
        }
    }

    private void handlePublishFailure(AuditEvent event, String value, Exception exception) {
        log.error("Failed to publish audit event to {}: {}", topic, exception.getMessage());
        failedCount.incrementAndGet();

        // Try to send to DLQ
        if (dlqTopic != null && !dlqTopic.isBlank()) {
            try {
                ProducerRecord<String, String> dlqRecord = new ProducerRecord<>(
                    dlqTopic, event.correlationId(), value
                );
                dlqRecord.headers().add("original-topic", topic.getBytes(StandardCharsets.UTF_8));
                dlqRecord.headers().add("error", exception.getMessage().getBytes(StandardCharsets.UTF_8));

                producer.send(dlqRecord, (metadata, dlqException) -> {
                    if (dlqException != null) {
                        log.error("Failed to publish to DLQ: {}", dlqException.getMessage());
                    } else {
                        log.warn("Audit event sent to DLQ: {}", event.correlationId());
                    }
                });
            } catch (Exception dlqError) {
                log.error("Failed to send to DLQ: {}", dlqError.getMessage());
            }
        }
    }

    /**
     * Flush pending messages.
     */
    public void flush() {
        producer.flush();
    }

    /**
     * Get count of successfully published events.
     */
    public long getPublishedCount() {
        return publishedCount.get();
    }

    /**
     * Get count of failed events.
     */
    public long getFailedCount() {
        return failedCount.get();
    }

    @Override
    public void close() {
        log.info("Closing Kafka audit sink: published={}, failed={}",
            publishedCount.get(), failedCount.get());
        producer.close(Duration.ofSeconds(10));
    }

    /**
     * Configuration for Kafka audit sink.
     */
    public static class KafkaAuditSinkConfig {
        private String bootstrapServers = "localhost:9092";
        private String topic = "regulus.audit.events";
        private String dlqTopic = "regulus.audit.events.dlq";
        private String acks = "all";
        private int retries = 3;
        private boolean idempotent = true;
        private int batchSize = 16384;
        private int lingerMs = 100;
        private long bufferMemory = 33554432L;
        private String compressionType = "lz4";

        public String getBootstrapServers() { return bootstrapServers; }
        public void setBootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; }
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        public String getDlqTopic() { return dlqTopic; }
        public void setDlqTopic(String dlqTopic) { this.dlqTopic = dlqTopic; }
        public String getAcks() { return acks; }
        public void setAcks(String acks) { this.acks = acks; }
        public int getRetries() { return retries; }
        public void setRetries(int retries) { this.retries = retries; }
        public boolean isIdempotent() { return idempotent; }
        public void setIdempotent(boolean idempotent) { this.idempotent = idempotent; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public int getLingerMs() { return lingerMs; }
        public void setLingerMs(int lingerMs) { this.lingerMs = lingerMs; }
        public long getBufferMemory() { return bufferMemory; }
        public void setBufferMemory(long bufferMemory) { this.bufferMemory = bufferMemory; }
        public String getCompressionType() { return compressionType; }
        public void setCompressionType(String compressionType) { this.compressionType = compressionType; }
    }
}
