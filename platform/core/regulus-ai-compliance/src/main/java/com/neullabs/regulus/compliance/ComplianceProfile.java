package com.neullabs.regulus.compliance;

import java.util.Set;

/**
 * A declarative bundle that maps Regulus mechanisms (policy guards, redaction,
 * audit, retention, residency) to a specific regulation.
 *
 * <p>Profiles are pure data: they describe what a regulation requires, not
 * how Regulus implements it. The implementations live in the
 * {@code regulus-ai-adk-plugins} and {@code regulus-ai-adk-services} modules
 * and consume {@link ComplianceProfile} instances to configure themselves.
 *
 * <p>Multiple profiles compose into a {@link CompositeComplianceProfile} so a
 * tenant can opt in to, for example, {@code eu-ai-act + uk-gdpr + fca-sysc}
 * simultaneously.
 *
 * <p>Implementations must be immutable and safe to share across threads.
 */
public interface ComplianceProfile {

    /** Stable identifier, e.g. {@code "eu-ai-act"}. Used in YAML config and audit events. */
    String id();

    /** Human-readable name for the regulation, e.g. {@code "EU AI Act"}. */
    String displayName();

    /** Jurisdiction this profile covers. */
    Jurisdiction jurisdiction();

    /** Citation anchor for the regulation, e.g. {@code "EU 2024/1689"}. */
    String citation();

    /** Bindings of Regulus mechanisms to specific articles / paragraphs / sections. */
    Set<ControlBinding> controls();

    /** Retention windows applied by {@code RegulusRetentionEventCompactor}. */
    EventCompactionPolicy retention();

    /** Residency allowlist applied by {@code RegulusDataResidencyPlugin}. */
    ResidencyPolicy residency();

    /** Audit-event schema fields required by this profile. */
    AuditSchema auditSchema();
}
