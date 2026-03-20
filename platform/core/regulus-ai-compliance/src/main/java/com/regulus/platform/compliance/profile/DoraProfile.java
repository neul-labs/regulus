package com.regulus.platform.compliance.profile;

import com.regulus.platform.compliance.AuditSchema;
import com.regulus.platform.compliance.ComplianceProfile;
import com.regulus.platform.compliance.ControlBinding;
import com.regulus.platform.compliance.EventCompactionPolicy;
import com.regulus.platform.compliance.Jurisdiction;
import com.regulus.platform.compliance.ResidencyPolicy;

import java.time.Duration;
import java.util.Set;

public final class DoraProfile implements ComplianceProfile {

    @Override public String id() { return "dora"; }
    @Override public String displayName() { return "DORA"; }
    @Override public Jurisdiction jurisdiction() { return Jurisdiction.EU; }
    @Override public String citation() { return "EU 2022/2554"; }

    @Override public Set<ControlBinding> controls() {
        return Set.of(
                new ControlBinding("ict-risk-management", "Arts. 5-10",
                        "ICT risk management framework — Regulus policy guards + kill switch + audit are the technical floor."),
                new ControlBinding("incident-classification", "Arts. 17-23",
                        "ICT-related incident management — Regulus audit schema includes incident severity tagging and 72h notification timer."),
                new ControlBinding("third-party-risk", "Arts. 28-30",
                        "ICT third-party risk — Regulus model registry tracks LLM provider as a critical third-party, with sub-outsourcing exposed."),
                new ControlBinding("threat-led-pen-testing", "Arts. 26-27",
                        "Advanced testing of ICT tools — Regulus does not perform this but its audit log forms the in-scope evidence base.")
        );
    }

    @Override public EventCompactionPolicy retention() {
        // DORA Art. 12: ICT-related incident records retained ≥ 5 years for significant incidents.
        return new EventCompactionPolicy(Duration.ofDays(365 * 5), Duration.ofDays(365 * 7), true);
    }

    @Override public ResidencyPolicy residency() {
        return new ResidencyPolicy(
                Set.of("europe-west1", "europe-west2", "europe-west3", "europe-west4",
                        "europe-west6", "europe-west8", "europe-west9", "europe-west12"),
                true, // CMEK strongly expected for critical financial workloads
                ResidencyPolicy.CrossBorderTransfer.ALLOWED_WITH_SCC);
    }

    @Override public AuditSchema auditSchema() {
        return new AuditSchema(
                Set.of("event_id", "occurred_at", "actor", "action", "result",
                        "incident_severity", "ict_third_party", "rto_seconds", "rpo_seconds"),
                AuditSchema.Immutability.SIGNED,
                true);
    }
}
