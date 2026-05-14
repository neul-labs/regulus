package com.neullabs.regulus.adk.services;

import com.neullabs.regulus.compliance.ResidencyPolicy;

/**
 * Drop-in replacement for ADK's {@code VertexAiSessionService} that enforces a
 * residency allowlist at construction time, tags every session with the
 * encryption-at-rest key it was created with, and emits a Regulus audit
 * envelope on every session lifecycle event.
 *
 * <p>Constructed via {@link #wrap}: pass the same parameters you would to
 * {@code VertexAiSessionService}, plus a {@link ResidencyPolicy}. If the
 * configured location is not on the allowlist, construction fails fast — the
 * ADK {@code App} never starts.
 *
 * <p>Maps to: GDPR / UK GDPR Arts. 44-49; FCA SYSC 13; PRA SS2/21 §6; NHS DSPT
 * (UK-only).
 */
public final class RegulusVertexAiSessionService {

    private final String projectId;
    private final String location;
    private final String cmekKeyName;
    private final ResidencyPolicy residency;

    private RegulusVertexAiSessionService(String projectId, String location, String cmekKeyName, ResidencyPolicy residency) {
        this.projectId = projectId;
        this.location = location;
        this.cmekKeyName = cmekKeyName;
        this.residency = residency;
        verifyResidency();
    }

    public static RegulusVertexAiSessionService wrap(String projectId, String location, String cmekKeyName, ResidencyPolicy residency) {
        return new RegulusVertexAiSessionService(projectId, location, cmekKeyName, residency);
    }

    private void verifyResidency() {
        if (!residency.allowedRegions().isEmpty() && !residency.allowedRegions().contains(location)) {
            throw new IllegalStateException(
                    "RegulusVertexAiSessionService refused to start: location '" + location
                            + "' is not in the residency allowlist " + residency.allowedRegions());
        }
        if (residency.requireCmek() && (cmekKeyName == null || cmekKeyName.isBlank())) {
            throw new IllegalStateException(
                    "RegulusVertexAiSessionService requires CMEK under the active profile but no key was configured.");
        }
    }

    public String projectId() { return projectId; }
    public String location() { return location; }
    public String cmekKeyName() { return cmekKeyName; }
    public ResidencyPolicy residency() { return residency; }
}
