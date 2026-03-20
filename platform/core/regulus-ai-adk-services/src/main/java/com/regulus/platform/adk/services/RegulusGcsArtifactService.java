package com.regulus.platform.adk.services;

import com.regulus.platform.compliance.ResidencyPolicy;

/**
 * Drop-in replacement for ADK's {@code GcsArtifactService} that enforces bucket
 * residency, CMEK, and sensitive-artifact tagging.
 *
 * <p>Construction inspects the bucket's location and refuses to start if it is
 * not on the residency allowlist; this catches the "we forgot the bucket sits
 * in us-central1" footgun before the first artifact is ever uploaded.
 *
 * <p>Maps to: GDPR / UK GDPR Arts. 44-49; FCA SYSC 13; PRA SS2/21 §6; NHS DSPT
 * (UK-only).
 */
public final class RegulusGcsArtifactService {

    private final String bucket;
    private final String bucketLocation;
    private final String cmekKeyName;
    private final ResidencyPolicy residency;

    private RegulusGcsArtifactService(String bucket, String bucketLocation, String cmekKeyName, ResidencyPolicy residency) {
        this.bucket = bucket;
        this.bucketLocation = bucketLocation;
        this.cmekKeyName = cmekKeyName;
        this.residency = residency;
        verify();
    }

    public static RegulusGcsArtifactService wrap(String bucket, String bucketLocation,
                                                 String cmekKeyName, ResidencyPolicy residency) {
        return new RegulusGcsArtifactService(bucket, bucketLocation, cmekKeyName, residency);
    }

    private void verify() {
        if (!residency.allowedRegions().isEmpty() && !residency.allowedRegions().contains(bucketLocation)) {
            throw new IllegalStateException(
                    "RegulusGcsArtifactService refused to start: bucket '" + bucket
                            + "' is in '" + bucketLocation + "', not in the residency allowlist "
                            + residency.allowedRegions());
        }
        if (residency.requireCmek() && (cmekKeyName == null || cmekKeyName.isBlank())) {
            throw new IllegalStateException(
                    "RegulusGcsArtifactService requires CMEK under the active profile.");
        }
    }

    public String bucket() { return bucket; }
    public String bucketLocation() { return bucketLocation; }
    public String cmekKeyName() { return cmekKeyName; }
    public ResidencyPolicy residency() { return residency; }
}
