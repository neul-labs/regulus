package com.neullabs.regulus.grc;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

/**
 * Canonical, vendor-neutral evidence record. Adapters translate this into
 * their tool's native schema.
 *
 * @param eventId             ULID, same value as the corresponding audit event_id
 * @param occurredAt          UTC instant
 * @param controlFrameworkId  governance framework id, e.g. {@code "nist-ai-rmf"}
 * @param controlId           framework control id, e.g. {@code "GOVERN-1.5"}
 * @param complianceProfileId optional regulation profile id, e.g. {@code "fca-sysc"}
 * @param regulationClause    optional regulation citation, e.g. {@code "SYSC 9"}
 * @param kind                what this evidence represents
 * @param actor               who performed the action
 * @param result              {@code "pass"} | {@code "fail"} | {@code "exception-recorded"}
 * @param attributes          additional fields (mechanism, subject_id, etc.)
 * @param auditEventLink      back-pointer to the source audit event
 */
public record GrcEvidenceEnvelope(
        String eventId,
        Instant occurredAt,
        String controlFrameworkId,
        String controlId,
        String complianceProfileId,
        String regulationClause,
        EvidenceKind kind,
        String actor,
        String result,
        Map<String, Object> attributes,
        URI auditEventLink) {
}
