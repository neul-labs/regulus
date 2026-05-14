package com.neullabs.regulus.persistence.repository;

import com.neullabs.regulus.persistence.entity.AuditEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for audit events.
 * Provides queries for compliance reporting and audit trail access.
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, String> {

    /**
     * Find all events for a correlation ID (full request trace).
     */
    List<AuditEventEntity> findByCorrelationIdOrderByTimestampAsc(String correlationId);

    /**
     * Find events by correlation ID.
     */
    List<AuditEventEntity> findByCorrelationId(String correlationId);

    /**
     * Find events by user ID and time range.
     */
    List<AuditEventEntity> findByUserIdAndTimestampBetween(String userId, Instant start, Instant end);

    /**
     * Find events by agent ID.
     */
    List<AuditEventEntity> findByAgentId(String agentId);

    /**
     * Find events by event type.
     */
    List<AuditEventEntity> findByEventType(String eventType);

    /**
     * Count events by event type and time range.
     */
    long countByEventTypeAndTimestampBetween(String eventType, Instant start, Instant end);

    /**
     * Find events by outcome (SUCCESS, FAILURE, BLOCKED, etc.).
     */
    List<AuditEventEntity> findByOutcome(String outcome);

    /**
     * Find events by user ID with pagination.
     */
    Page<AuditEventEntity> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    /**
     * Find events by agent ID with pagination.
     */
    Page<AuditEventEntity> findByAgentIdOrderByTimestampDesc(String agentId, Pageable pageable);

    /**
     * Find events by event type with pagination.
     */
    Page<AuditEventEntity> findByEventTypeOrderByTimestampDesc(String eventType, Pageable pageable);

    /**
     * Find events in a time range.
     */
    @Query("SELECT e FROM AuditEventEntity e WHERE e.timestamp BETWEEN :start AND :end ORDER BY e.timestamp DESC")
    Page<AuditEventEntity> findByTimestampBetween(
        @Param("start") Instant start,
        @Param("end") Instant end,
        Pageable pageable
    );

    /**
     * Find events by LEI (Legal Entity Identifier) for regulatory reporting.
     */
    Page<AuditEventEntity> findByLeiOrderByTimestampDesc(String lei, Pageable pageable);

    /**
     * Find events with policy violations.
     */
    @Query("SELECT e FROM AuditEventEntity e WHERE e.policyViolations IS NOT NULL AND e.policyViolations <> '' ORDER BY e.timestamp DESC")
    Page<AuditEventEntity> findWithPolicyViolations(Pageable pageable);

    /**
     * Find events where kill switch was active.
     */
    Page<AuditEventEntity> findByKillSwitchActiveTrue(Pageable pageable);

    /**
     * Count events by type in a time range (for metrics).
     */
    @Query("SELECT e.eventType, COUNT(e) FROM AuditEventEntity e WHERE e.timestamp BETWEEN :start AND :end GROUP BY e.eventType")
    List<Object[]> countByEventTypeInTimeRange(
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    /**
     * Get total token usage in a time range.
     */
    @Query("SELECT SUM(e.tokenCount) FROM AuditEventEntity e WHERE e.timestamp BETWEEN :start AND :end")
    Long sumTokenCountInTimeRange(
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    /**
     * Get total estimated cost in a time range.
     */
    @Query("SELECT SUM(e.estimatedCost) FROM AuditEventEntity e WHERE e.timestamp BETWEEN :start AND :end")
    Double sumEstimatedCostInTimeRange(
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    /**
     * Search events by multiple criteria.
     */
    @Query("""
        SELECT e FROM AuditEventEntity e
        WHERE (:eventType IS NULL OR e.eventType = :eventType)
          AND (:userId IS NULL OR e.userId = :userId)
          AND (:agentId IS NULL OR e.agentId = :agentId)
          AND (:outcome IS NULL OR e.outcome = :outcome)
          AND e.timestamp BETWEEN :start AND :end
        ORDER BY e.timestamp DESC
        """)
    Page<AuditEventEntity> search(
        @Param("eventType") String eventType,
        @Param("userId") String userId,
        @Param("agentId") String agentId,
        @Param("outcome") String outcome,
        @Param("start") Instant start,
        @Param("end") Instant end,
        Pageable pageable
    );
}
