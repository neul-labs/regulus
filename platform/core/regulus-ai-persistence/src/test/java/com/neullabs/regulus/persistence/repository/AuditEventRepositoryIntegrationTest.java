package com.neullabs.regulus.persistence.repository;

import com.neullabs.regulus.persistence.entity.AuditEventEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AuditEventRepository using Testcontainers with PostgreSQL.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuditEventRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("regulus_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private AuditEventRepository repository;

    private AuditEventEntity testEvent;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        testEvent = AuditEventEntity.builder()
            .eventType("LLM_INVOCATION")
            .correlationId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .userId("user-123")
            .agentId("financial-advisor")
            .modelId("gpt-4o")
            .tokenCount(150)
            .durationMs(1500L)
            .outcome("SUCCESS")
            .metadataJson("{\"purpose\":\"account_inquiry\"}")
            .build();
    }

    @Test
    void shouldSaveAndRetrieveAuditEvent() {
        // When
        AuditEventEntity saved = repository.save(testEvent);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEventType()).isEqualTo("LLM_INVOCATION");
        assertThat(saved.getUserId()).isEqualTo("user-123");
    }

    @Test
    void shouldFindByCorrelationId() {
        // Given
        String correlationId = testEvent.getCorrelationId();
        repository.save(testEvent);

        // When
        List<AuditEventEntity> found = repository.findByCorrelationId(correlationId);

        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getCorrelationId()).isEqualTo(correlationId);
    }

    @Test
    void shouldFindByUserIdAndTimeRange() {
        // Given
        repository.save(testEvent);

        Instant startTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant endTime = Instant.now().plus(1, ChronoUnit.HOURS);

        // When
        List<AuditEventEntity> found = repository.findByUserIdAndTimestampBetween(
            "user-123", startTime, endTime);

        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getUserId()).isEqualTo("user-123");
    }

    @Test
    void shouldFindByAgentId() {
        // Given
        repository.save(testEvent);

        // When
        List<AuditEventEntity> found = repository.findByAgentId("financial-advisor");

        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getAgentId()).isEqualTo("financial-advisor");
    }

    @Test
    void shouldFindByEventType() {
        // Given
        repository.save(testEvent);

        // Create another event with different type
        AuditEventEntity policyEvent = AuditEventEntity.builder()
            .eventType("POLICY_VIOLATION")
            .correlationId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .userId("user-456")
            .outcome("BLOCKED")
            .build();
        repository.save(policyEvent);

        // When
        List<AuditEventEntity> llmEvents = repository.findByEventType("LLM_INVOCATION");
        List<AuditEventEntity> policyEvents = repository.findByEventType("POLICY_VIOLATION");

        // Then
        assertThat(llmEvents).hasSize(1);
        assertThat(policyEvents).hasSize(1);
    }

    @Test
    void shouldCountByEventTypeAndTimeRange() {
        // Given
        repository.save(testEvent);

        // Create more events
        for (int i = 0; i < 5; i++) {
            AuditEventEntity event = AuditEventEntity.builder()
                .eventType("LLM_INVOCATION")
                .correlationId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .userId("user-" + i)
                .outcome("SUCCESS")
                .build();
            repository.save(event);
        }

        Instant startTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant endTime = Instant.now().plus(1, ChronoUnit.HOURS);

        // When
        long count = repository.countByEventTypeAndTimestampBetween(
            "LLM_INVOCATION", startTime, endTime);

        // Then
        assertThat(count).isEqualTo(6); // 1 original + 5 new
    }

    @Test
    void shouldStoreAndRetrieveMetadata() {
        // Given
        String metadataJson = "{\"purpose\":\"account_inquiry\",\"lei\":\"549300EXAMPLE123456\"}";

        AuditEventEntity eventWithMetadata = AuditEventEntity.builder()
            .eventType("LLM_INVOCATION")
            .correlationId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .userId("user-789")
            .metadataJson(metadataJson)
            .outcome("SUCCESS")
            .build();

        // When
        AuditEventEntity saved = repository.save(eventWithMetadata);
        AuditEventEntity found = repository.findById(saved.getId()).orElseThrow();

        // Then
        assertThat(found.getMetadataJson()).isNotNull();
        assertThat(found.getMetadataJson()).contains("account_inquiry");
    }

    @Test
    void shouldFindByOutcome() {
        // Given
        AuditEventEntity failedEvent = AuditEventEntity.builder()
            .eventType("LLM_INVOCATION")
            .correlationId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .userId("user-error")
            .outcome("FAILURE")
            .policyViolations("Rate limit exceeded")
            .build();

        // When
        repository.save(failedEvent);
        List<AuditEventEntity> allEvents = repository.findAll();

        // Then
        assertThat(allEvents.stream()
            .filter(e -> "FAILURE".equals(e.getOutcome()))
            .count()).isEqualTo(1);
    }
}
