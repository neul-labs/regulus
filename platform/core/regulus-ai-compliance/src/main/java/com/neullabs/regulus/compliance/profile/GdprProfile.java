package com.neullabs.regulus.compliance.profile;

import com.neullabs.regulus.compliance.AuditSchema;
import com.neullabs.regulus.compliance.ComplianceProfile;
import com.neullabs.regulus.compliance.ControlBinding;
import com.neullabs.regulus.compliance.EventCompactionPolicy;
import com.neullabs.regulus.compliance.ResidencyPolicy;
import com.neullabs.regulus.identity.Jurisdiction;

import java.time.Duration;
import java.util.Set;

public final class GdprProfile implements ComplianceProfile {

    @Override public String id() { return "gdpr"; }
    @Override public String displayName() { return "GDPR"; }
    @Override public Jurisdiction jurisdiction() { return Jurisdiction.EU; }
    @Override public String citation() { return "EU 2016/679"; }

    @Override public Set<ControlBinding> controls() {
        return Set.of(
                new ControlBinding("purpose-binding", "Art. 5(1)(b)",
                        "Purpose limitation — Regulus enforces purpose codes on every agent invocation."),
                new ControlBinding("storage-limitation", "Art. 5(1)(e)",
                        "Storage limitation — Regulus retention compactor expires events past the policy window."),
                new ControlBinding("automated-decisions-safeguards", "Art. 22",
                        "Right not to be subject to automated decisions — Regulus dual-control human-in-the-loop satisfies the 'human intervention' safeguard."),
                new ControlBinding("privacy-by-design", "Art. 25",
                        "Data protection by design and by default — Regulus PII redaction runs before LLM call."),
                new ControlBinding("records-of-processing", "Art. 30",
                        "Records of processing activities — Regulus audit trail provides the per-tenant ROPA evidence."),
                new ControlBinding("dpia-evidence", "Art. 35",
                        "Data protection impact assessment — Regulus exports DPIA-shaped evidence packs from the audit log."),
                new ControlBinding("cross-border-residency", "Arts. 44-49",
                        "Transfers to third countries — Regulus residency plugin pins regions and enforces SCC/adequacy where configured.")
        );
    }

    @Override public EventCompactionPolicy retention() {
        // GDPR pushes for storage limitation; default 1 year, summary 2 years, subject erasure required.
        return new EventCompactionPolicy(Duration.ofDays(365), Duration.ofDays(365 * 2), true);
    }

    @Override public ResidencyPolicy residency() {
        // EU/EEA regions; SCCs allowed for transfers outside.
        return new ResidencyPolicy(
                Set.of("europe-west1", "europe-west2", "europe-west3", "europe-west4",
                        "europe-west6", "europe-west8", "europe-west9", "europe-west12",
                        "europe-north1", "europe-southwest1", "europe-central2"),
                false,
                ResidencyPolicy.CrossBorderTransfer.ALLOWED_WITH_SCC);
    }

    @Override public AuditSchema auditSchema() {
        return new AuditSchema(
                Set.of("event_id", "occurred_at", "actor", "action", "result",
                        "subject_id", "purpose_code", "lawful_basis", "data_categories"),
                AuditSchema.Immutability.MONOTONIC,
                true);
    }
}
