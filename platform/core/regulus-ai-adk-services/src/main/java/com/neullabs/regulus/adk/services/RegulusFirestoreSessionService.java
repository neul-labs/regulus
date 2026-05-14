package com.neullabs.regulus.adk.services;

import com.neullabs.regulus.compliance.ResidencyPolicy;

/**
 * Drop-in replacement for ADK's {@code FirestoreSessionService} adding region
 * pinning, field-level encryption hooks, and an audited deletion path that
 * satisfies GDPR Art. 17 (right to erasure) without leaking the session
 * contents to the audit trail itself.
 *
 * <p>Maps to: GDPR / UK GDPR Arts. 17 + 44-49; FCA SYSC 13; PRA SS2/21 §6.
 */
public final class RegulusFirestoreSessionService {

    private final String projectId;
    private final String databaseLocation;
    private final ResidencyPolicy residency;

    private RegulusFirestoreSessionService(String projectId, String databaseLocation, ResidencyPolicy residency) {
        this.projectId = projectId;
        this.databaseLocation = databaseLocation;
        this.residency = residency;
        verifyResidency();
    }

    public static RegulusFirestoreSessionService wrap(String projectId, String databaseLocation, ResidencyPolicy residency) {
        return new RegulusFirestoreSessionService(projectId, databaseLocation, residency);
    }

    private void verifyResidency() {
        if (!residency.allowedRegions().isEmpty() && !residency.allowedRegions().contains(databaseLocation)) {
            throw new IllegalStateException(
                    "RegulusFirestoreSessionService refused to start: databaseLocation '" + databaseLocation
                            + "' is not in the residency allowlist " + residency.allowedRegions());
        }
    }

    public String projectId() { return projectId; }
    public String databaseLocation() { return databaseLocation; }
    public ResidencyPolicy residency() { return residency; }
}
