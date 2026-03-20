package com.regulus.platform.observability.audit.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.regulus.platform.observability.audit.AuditEvent;
import com.regulus.platform.observability.audit.AuditLogger.AuditSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Database audit sink for persistent audit storage.
 * Supports batching for performance optimization.
 */
public class DatabaseAuditSink implements AuditSink, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DatabaseAuditSink.class);

    private static final String INSERT_SQL = """
        INSERT INTO audit_events (
            id, correlation_id, event_type, timestamp, user_id, agent_id,
            model_id, tool_name, input_summary, output_summary, outcome,
            duration_ms, token_count, estimated_cost, policy_decision,
            policy_violations, lei, purpose_code, consent_granted,
            kill_switch_active, metadata_json, source_ip, user_agent, created_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final int batchSize;
    private final long flushIntervalMs;

    private final BlockingQueue<AuditEvent> eventQueue;
    private final ScheduledExecutorService scheduler;
    private final AtomicLong writtenCount = new AtomicLong(0);
    private final AtomicLong failedCount = new AtomicLong(0);

    public DatabaseAuditSink(DataSource dataSource, int batchSize, long flushIntervalMs) {
        this.dataSource = dataSource;
        this.batchSize = batchSize;
        this.flushIntervalMs = flushIntervalMs;

        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.eventQueue = new LinkedBlockingQueue<>(10000);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "audit-db-flusher");
            t.setDaemon(true);
            return t;
        });

        // Schedule periodic flush
        scheduler.scheduleAtFixedRate(this::flushBatch, flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);

        log.info("Database audit sink initialized: batchSize={}, flushInterval={}ms", batchSize, flushIntervalMs);
    }

    @Override
    public void write(AuditEvent event) {
        if (!eventQueue.offer(event)) {
            log.warn("Audit event queue full, dropping event: {}", event.correlationId());
            failedCount.incrementAndGet();
            return;
        }

        // Flush immediately if batch size reached
        if (eventQueue.size() >= batchSize) {
            scheduler.execute(this::flushBatch);
        }
    }

    private synchronized void flushBatch() {
        List<AuditEvent> batch = new ArrayList<>(batchSize);
        eventQueue.drainTo(batch, batchSize);

        if (batch.isEmpty()) {
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {
                for (AuditEvent event : batch) {
                    setParameters(stmt, event);
                    stmt.addBatch();
                }

                int[] results = stmt.executeBatch();
                conn.commit();

                int written = 0;
                for (int result : results) {
                    if (result >= 0 || result == PreparedStatement.SUCCESS_NO_INFO) {
                        written++;
                    }
                }

                writtenCount.addAndGet(written);
                log.debug("Flushed {} audit events to database", written);

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            log.error("Failed to flush audit batch: {}", e.getMessage());
            failedCount.addAndGet(batch.size());

            // Re-queue failed events (best effort)
            for (AuditEvent event : batch) {
                eventQueue.offer(event);
            }
        }
    }

    private void setParameters(PreparedStatement stmt, AuditEvent event) throws SQLException {
        int i = 1;
        stmt.setString(i++, java.util.UUID.randomUUID().toString());
        stmt.setString(i++, event.correlationId());
        stmt.setString(i++, event.type().name());
        stmt.setTimestamp(i++, Timestamp.from(event.timestamp()));
        stmt.setString(i++, event.userId());
        stmt.setString(i++, getDetailString(event, "agentId"));
        stmt.setString(i++, getDetailString(event, "modelId"));
        stmt.setString(i++, event.resource());
        stmt.setString(i++, truncate(getDetailString(event, "input"), 1000));
        stmt.setString(i++, truncate(getDetailString(event, "output"), 1000));
        stmt.setString(i++, event.outcome() != null ? event.outcome().name() : null);
        stmt.setObject(i++, getDetailLong(event, "durationMs"));
        stmt.setObject(i++, getDetailInt(event, "tokenCount"));
        stmt.setObject(i++, getDetailDouble(event, "cost"));
        stmt.setString(i++, getDetailString(event, "policyDecision"));
        stmt.setString(i++, getDetailString(event, "policyViolations"));
        stmt.setString(i++, getDetailString(event, "lei"));
        stmt.setString(i++, getDetailString(event, "purposeCode"));
        stmt.setObject(i++, getDetailBoolean(event, "consentGranted"));
        stmt.setObject(i++, getDetailBoolean(event, "killSwitchActive"));
        stmt.setString(i++, serializeDetails(event));
        stmt.setString(i++, getDetailString(event, "sourceIp"));
        stmt.setString(i++, getDetailString(event, "userAgent"));
    }

    private String getDetailString(AuditEvent event, String key) {
        if (event.details() == null) return null;
        Object value = event.details().get(key);
        return value != null ? value.toString() : null;
    }

    private Long getDetailLong(AuditEvent event, String key) {
        if (event.details() == null) return null;
        Object value = event.details().get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private Integer getDetailInt(AuditEvent event, String key) {
        if (event.details() == null) return null;
        Object value = event.details().get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private Double getDetailDouble(AuditEvent event, String key) {
        if (event.details() == null) return null;
        Object value = event.details().get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private Boolean getDetailBoolean(AuditEvent event, String key) {
        if (event.details() == null) return null;
        Object value = event.details().get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }

    private String serializeDetails(AuditEvent event) {
        if (event.details() == null || event.details().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(event.details());
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    public long getWrittenCount() {
        return writtenCount.get();
    }

    public long getFailedCount() {
        return failedCount.get();
    }

    public int getQueueSize() {
        return eventQueue.size();
    }

    @Override
    public void close() {
        log.info("Closing database audit sink: flushing remaining events");
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Final flush
        flushBatch();

        log.info("Database audit sink closed: written={}, failed={}",
            writtenCount.get(), failedCount.get());
    }
}
