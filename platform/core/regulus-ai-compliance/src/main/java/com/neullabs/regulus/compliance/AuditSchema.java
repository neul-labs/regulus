package com.neullabs.regulus.compliance;

import java.util.Set;

/**
 * Required fields and properties of audit events emitted by
 * {@code RegulusAuditPlugin} under this profile.
 *
 * <p>Auditors typically ask for: who/what/when/why/result, immutability, and
 * a clear trail from a request to every downstream action it triggered. This
 * record encodes those requirements so plugins can refuse to emit incomplete
 * events.
 *
 * @param requiredFields     event fields that must be present, e.g.
 *                           {@code subject_id, purpose_code, model_id, region}.
 * @param immutabilityHint   {@code MONOTONIC} for append-only sinks (Kafka),
 *                           {@code SIGNED} when each event must carry a
 *                           cryptographic signature.
 * @param subjectLinking     whether events must include a stable subject
 *                           identifier to support GDPR Art. 15 subject access
 *                           and Art. 17 erasure requests.
 */
public record AuditSchema(
        Set<String> requiredFields,
        Immutability immutabilityHint,
        boolean subjectLinking) {

    public enum Immutability {
        BEST_EFFORT,
        MONOTONIC,
        SIGNED
    }

    public static AuditSchema basic() {
        return new AuditSchema(
                Set.of("event_id", "occurred_at", "actor", "action", "result"),
                Immutability.MONOTONIC,
                false);
    }
}
