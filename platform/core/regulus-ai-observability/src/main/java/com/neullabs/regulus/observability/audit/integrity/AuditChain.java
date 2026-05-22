package com.neullabs.regulus.observability.audit.integrity;

import com.neullabs.regulus.observability.audit.AuditEvent;

import java.util.List;

/**
 * Tamper-evident chain over {@link AuditEvent}s. Implementations seal each
 * event with the previous event's hash so an offline auditor can verify the
 * sequence end-to-end. Verification returns {@code false} if any event is
 * mutated, removed or reordered.
 *
 * <p>This is the single SPI consumed by {@code AuditLogger} when audit
 * integrity is enabled. The CLI {@code regulus audit verify <chain.jsonl>}
 * is a thin wrapper around {@link #verify(List)}.
 */
public interface AuditChain {

    /**
     * Append an event to the chain, returning the sealed wrapper that should
     * be persisted by sinks. Implementations must be safe to call from
     * multiple threads — appending is the hot path on every audited request.
     */
    SealedAuditEvent append(AuditEvent event);

    /**
     * Verify a previously-persisted chain. Returns {@code true} only when
     * every {@code previousEventHash} matches the preceding event's hash,
     * every {@code eventHash} matches the recomputed hash of the canonical
     * event, every {@code chainIndex} is contiguous, and (when present)
     * every signature verifies under the keyed {@code VerificationKey}.
     */
    boolean verify(List<SealedAuditEvent> chain);
}
