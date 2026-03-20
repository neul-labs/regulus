package com.regulus.platform.compliance.profile;

import com.regulus.platform.compliance.AuditSchema;
import com.regulus.platform.compliance.ComplianceProfile;
import com.regulus.platform.compliance.ControlBinding;
import com.regulus.platform.compliance.EventCompactionPolicy;
import com.regulus.platform.compliance.Jurisdiction;
import com.regulus.platform.compliance.ResidencyPolicy;

import java.time.Duration;
import java.util.Set;

public final class EhdsProfile implements ComplianceProfile {

    @Override public String id() { return "ehds"; }
    @Override public String displayName() { return "European Health Data Space"; }
    @Override public Jurisdiction jurisdiction() { return Jurisdiction.EU; }
    @Override public String citation() { return "EU 2025/327"; }

    @Override public Set<ControlBinding> controls() {
        return Set.of(
                new ControlBinding("primary-use-electronic-health-data", "Chapter II",
                        "Patient access and control over electronic health data — Regulus audit attribution + erasure plumbing."),
                new ControlBinding("secondary-use-permit", "Chapter IV",
                        "Secondary use of health data requires a permit from a Health Data Access Body — Regulus model registry stores the permit reference."),
                new ControlBinding("data-quality-labels", "Art. 56",
                        "Data quality and utility labels — Regulus audit emits provenance and quality tags on each AI-assisted record."),
                new ControlBinding("interoperability", "Chapter III",
                        "Interoperability of EHR systems — Regulus does not implement HL7/FHIR but its audit schema is mappable to FHIR AuditEvent.")
        );
    }

    @Override public EventCompactionPolicy retention() {
        return new EventCompactionPolicy(Duration.ofDays(365 * 10), Duration.ofDays(365 * 30), true);
    }

    @Override public ResidencyPolicy residency() {
        return new ResidencyPolicy(
                Set.of("europe-west1", "europe-west2", "europe-west3", "europe-west4",
                        "europe-west6", "europe-west8", "europe-west9", "europe-west12"),
                true,
                ResidencyPolicy.CrossBorderTransfer.ALLOWED_WITH_ADEQUACY_DECISION);
    }

    @Override public AuditSchema auditSchema() {
        return new AuditSchema(
                Set.of("event_id", "occurred_at", "actor", "action", "result",
                        "patient_pseudonym", "permit_ref", "data_quality_label", "care_setting"),
                AuditSchema.Immutability.SIGNED,
                true);
    }
}
