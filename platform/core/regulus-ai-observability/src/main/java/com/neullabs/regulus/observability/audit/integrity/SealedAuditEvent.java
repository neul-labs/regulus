package com.neullabs.regulus.observability.audit.integrity;

import com.neullabs.regulus.observability.audit.AuditEvent;

import java.util.Objects;
import java.util.Optional;

/**
 * An {@link AuditEvent} wrapped with the chain artefacts that make it
 * tamper-evident: the SHA-256 hash of the canonical JSON of the previous
 * event, the hash of this event, and (optionally) a detached signature over
 * this event's hash.
 *
 * <p>{@code chainIndex} is the zero-based position of the event in the chain
 * — useful for offline verifiers that want to detect gaps without scanning
 * the whole sink.
 */
public record SealedAuditEvent(
        AuditEvent event,
        long chainIndex,
        String previousEventHash,
        String eventHash,
        Optional<String> signature,
        String keyId) {

    public SealedAuditEvent {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(previousEventHash, "previousEventHash");
        Objects.requireNonNull(eventHash, "eventHash");
        Objects.requireNonNull(signature, "signature");
        Objects.requireNonNull(keyId, "keyId");
    }
}
