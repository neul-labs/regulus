package com.neullabs.regulus.compliance.profile;

import com.neullabs.regulus.compliance.AuditSchema;
import com.neullabs.regulus.compliance.ComplianceProfile;
import com.neullabs.regulus.compliance.ControlBinding;
import com.neullabs.regulus.compliance.EventCompactionPolicy;
import com.neullabs.regulus.compliance.Jurisdiction;
import com.neullabs.regulus.compliance.ResidencyPolicy;

import java.time.Duration;
import java.util.Set;

public final class Nis2Profile implements ComplianceProfile {

    @Override public String id() { return "nis2"; }
    @Override public String displayName() { return "NIS2 Directive"; }
    @Override public Jurisdiction jurisdiction() { return Jurisdiction.EU; }
    @Override public String citation() { return "EU 2022/2555"; }

    @Override public Set<ControlBinding> controls() {
        return Set.of(
                new ControlBinding("cyber-risk-management", "Art. 21",
                        "Cybersecurity risk-management measures — Regulus controls plane contributes the application-level slice."),
                new ControlBinding("incident-reporting", "Art. 23",
                        "24-hour early warning + 72-hour notification — Regulus audit pipeline feeds the incident timer."),
                new ControlBinding("supply-chain-security", "Art. 21(2)(d)",
                        "Supply chain security — Regulus model registry tracks the LLM provider chain.")
        );
    }

    @Override public EventCompactionPolicy retention() {
        return new EventCompactionPolicy(Duration.ofDays(365 * 2), Duration.ofDays(365 * 5), true);
    }

    @Override public ResidencyPolicy residency() {
        return new ResidencyPolicy(
                Set.of("europe-west1", "europe-west2", "europe-west3", "europe-west4",
                        "europe-west6", "europe-west8", "europe-west9", "europe-west12"),
                false,
                ResidencyPolicy.CrossBorderTransfer.ALLOWED_WITH_SCC);
    }

    @Override public AuditSchema auditSchema() {
        return new AuditSchema(
                Set.of("event_id", "occurred_at", "actor", "action", "result",
                        "incident_severity", "essential_entity_indicator"),
                AuditSchema.Immutability.MONOTONIC,
                false);
    }
}
