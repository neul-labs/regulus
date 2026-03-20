package com.regulus.platform.compliance;

import java.time.Duration;

/**
 * Retention window applied to events by {@code RegulusRetentionEventCompactor}.
 *
 * <p>Regulations have different ideas about how long an audit trail must be
 * kept and how aggressively older events can be summarised. GDPR Art. 5(1)(e)
 * pushes for storage limitation; FCA SYSC 9 and DORA Art. 12 push for long
 * retention of ICT incident records. Each profile picks the longer of any
 * conflicting requirements it owns; the composite picks the longer across
 * profiles.
 *
 * @param fullEventRetention how long to keep raw events before any summarisation.
 * @param summaryRetention   how long to keep summaries after raw events expire.
 * @param erasureSupported   whether per-subject erasure (GDPR Art. 17) is honoured
 *                           during the retention window.
 */
public record EventCompactionPolicy(
        Duration fullEventRetention,
        Duration summaryRetention,
        boolean erasureSupported) {

    /** Sensible default when no regulation has spoken yet. */
    public static EventCompactionPolicy unconstrained() {
        return new EventCompactionPolicy(Duration.ofDays(90), Duration.ofDays(365), true);
    }
}
