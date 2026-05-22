package com.neullabs.regulus.observability.audit.integrity;

import com.neullabs.regulus.observability.audit.AuditEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HashChainAuditChainTest {

    private static AuditEvent event(String message) {
        return AuditEvent.builder()
                .type(AuditEvent.EventType.LLM_CALL)
                .timestamp(Instant.parse("2026-05-22T10:00:00Z"))
                .correlationId("corr-" + message)
                .userId("u-1")
                .operation("llm.call")
                .resource("gemini-1.5")
                .outcome(AuditEvent.Outcome.SUCCESS)
                .message(message)
                .build();
    }

    @Test
    void appendBuildsContiguousChain() {
        AuditChain chain = new HashChainAuditChain();
        List<SealedAuditEvent> sealed = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            sealed.add(chain.append(event("e" + i)));
        }

        assertThat(sealed).hasSize(5);
        assertThat(sealed.get(0).previousEventHash()).isEqualTo(HashChainAuditChain.GENESIS_HASH);
        for (int i = 0; i < sealed.size(); i++) {
            assertThat(sealed.get(i).chainIndex()).isEqualTo(i);
        }
        for (int i = 1; i < sealed.size(); i++) {
            assertThat(sealed.get(i).previousEventHash())
                    .isEqualTo(sealed.get(i - 1).eventHash());
        }
    }

    @Test
    void verifyAcceptsIntactChain() {
        AuditChain chain = new HashChainAuditChain();
        List<SealedAuditEvent> sealed = List.of(
                chain.append(event("a")),
                chain.append(event("b")),
                chain.append(event("c")));

        assertThat(new HashChainAuditChain().verify(sealed)).isTrue();
    }

    @Test
    void verifyRejectsTamperedEvent() {
        AuditChain chain = new HashChainAuditChain();
        List<SealedAuditEvent> sealed = new ArrayList<>(List.of(
                chain.append(event("a")),
                chain.append(event("b")),
                chain.append(event("c"))));

        // Mutate the middle event's message — chain integrity must fail
        SealedAuditEvent tampered = sealed.get(1);
        AuditEvent mutated = AuditEvent.builder()
                .eventId(tampered.event().eventId())
                .type(tampered.event().type())
                .timestamp(tampered.event().timestamp())
                .correlationId(tampered.event().correlationId())
                .userId(tampered.event().userId())
                .operation(tampered.event().operation())
                .resource(tampered.event().resource())
                .outcome(tampered.event().outcome())
                .message("HACKED")
                .details(tampered.event().details() == null ? Map.of() : tampered.event().details())
                .metadata(tampered.event().metadata() == null ? Map.of() : tampered.event().metadata())
                .build();
        sealed.set(1, new SealedAuditEvent(
                mutated,
                tampered.chainIndex(),
                tampered.previousEventHash(),
                tampered.eventHash(),
                Optional.empty(),
                tampered.keyId()));

        assertThat(new HashChainAuditChain().verify(sealed)).isFalse();
    }

    @Test
    void verifyRejectsReordering() {
        AuditChain chain = new HashChainAuditChain();
        List<SealedAuditEvent> sealed = new ArrayList<>(List.of(
                chain.append(event("a")),
                chain.append(event("b")),
                chain.append(event("c"))));

        // Swap last two — chain indexes and previous-hashes break
        SealedAuditEvent two = sealed.get(2);
        sealed.set(2, sealed.get(1));
        sealed.set(1, two);

        assertThat(new HashChainAuditChain().verify(sealed)).isFalse();
    }

    @Test
    void emptyChainVerifiesTrue() {
        assertThat(new HashChainAuditChain().verify(List.of())).isTrue();
    }
}
