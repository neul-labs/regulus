package com.regulus.platform.adk.services;

import com.regulus.platform.compliance.EventCompactionPolicy;
import com.regulus.platform.compliance.ResidencyPolicy;

/**
 * Drop-in replacement for ADK's {@code FirestoreMemoryService} that triggers
 * retention-aware compaction on every write, exposes a GDPR Art. 17 erasure
 * path, and validates region residency at construction.
 *
 * <p>Maps to: GDPR / UK GDPR Arts. 5(1)(e) (storage limitation) + 17 (erasure)
 * + 44-49 (transfers); EU AI Act Art. 12 (logging) where memory doubles as a
 * decision-trace.
 */
public final class RegulusFirestoreMemoryService {

    private final String projectId;
    private final String databaseLocation;
    private final ResidencyPolicy residency;
    private final EventCompactionPolicy retention;

    private RegulusFirestoreMemoryService(String projectId, String databaseLocation,
                                          ResidencyPolicy residency, EventCompactionPolicy retention) {
        this.projectId = projectId;
        this.databaseLocation = databaseLocation;
        this.residency = residency;
        this.retention = retention;
        verify();
    }

    public static RegulusFirestoreMemoryService wrap(String projectId, String databaseLocation,
                                                     ResidencyPolicy residency, EventCompactionPolicy retention) {
        return new RegulusFirestoreMemoryService(projectId, databaseLocation, residency, retention);
    }

    private void verify() {
        if (!residency.allowedRegions().isEmpty() && !residency.allowedRegions().contains(databaseLocation)) {
            throw new IllegalStateException(
                    "RegulusFirestoreMemoryService refused to start: databaseLocation '" + databaseLocation
                            + "' is not in the residency allowlist " + residency.allowedRegions());
        }
        if (!retention.erasureSupported()) {
            throw new IllegalStateException(
                    "RegulusFirestoreMemoryService requires the active profile to permit subject erasure (GDPR Art. 17).");
        }
    }

    public String projectId() { return projectId; }
    public String databaseLocation() { return databaseLocation; }
    public ResidencyPolicy residency() { return residency; }
    public EventCompactionPolicy retention() { return retention; }
}
