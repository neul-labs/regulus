package com.regulus.platform.adk.services;

import com.regulus.platform.compliance.ComplianceProfile;
import com.regulus.platform.compliance.EventCompactionPolicy;

import java.time.Duration;

/**
 * Implements ADK's {@code EventCompactor} contract with a retention window
 * derived from the active {@link ComplianceProfile}.
 *
 * <p>Older events are summarised via ADK's {@code BaseEventSummarizer}
 * infrastructure rather than dropped, so an audit reconstruction is still
 * possible — but the context window stays bounded.
 *
 * <p>Maps to: GDPR Art. 5(1)(e) (storage limitation); EU AI Act Art. 19 (log
 * retention); FCA SYSC 9 (record retention); DORA Art. 12; NHS records
 * management code.
 */
public final class RegulusRetentionEventCompactor {

    private final ComplianceProfile profile;
    private final EventCompactionPolicy retention;

    public RegulusRetentionEventCompactor(ComplianceProfile profile) {
        this.profile = profile;
        this.retention = profile.retention();
    }

    public Duration fullRetention() { return retention.fullEventRetention(); }
    public Duration summaryRetention() { return retention.summaryRetention(); }

    public boolean shouldDrop(Duration eventAge) {
        return eventAge.compareTo(retention.summaryRetention()) > 0;
    }

    public boolean shouldSummarise(Duration eventAge) {
        return eventAge.compareTo(retention.fullEventRetention()) > 0
                && eventAge.compareTo(retention.summaryRetention()) <= 0;
    }

    public ComplianceProfile profile() { return profile; }
}
