package com.neullabs.regulus.governance;

import java.util.Set;

/**
 * A voluntary framework that describes what AI-governance controls *should*
 * exist in a mature operator. Sister concept to
 * {@link com.neullabs.regulus.compliance.ComplianceProfile}, which describes
 * what regulators *require*.
 *
 * <p>Frameworks compose multiplicatively with profiles: a tenant typically
 * runs (e.g.) {@code uk-gdpr + fca-sysc + eu-ai-act} as compliance profiles
 * AND {@code nist-ai-rmf + iso-42001} as governance frameworks. Regulus'
 * audit and evidence streams carry both the regulation citation and the
 * framework control id, so an auditor or a GRC tool can navigate the same
 * Regulus mechanism from either direction.
 *
 * <p>Implementations must be immutable and safe to share across threads.
 */
public interface GovernanceFramework {

    /** Stable identifier, e.g. {@code "nist-ai-rmf"}, {@code "iso-42001"}. */
    String id();

    /** Human-readable name. */
    String displayName();

    /** Version / edition identifier, e.g. {@code "1.0 + 600-1 GenAI Profile (2024)"}. */
    String version();

    /** Whether this framework is voluntary, certifiable, or a published standard. */
    FrameworkKind kind();

    /** Full inventory of controls or control categories the framework defines. */
    Set<FrameworkControl> controls();

    /** Regulus mechanism → framework control id bindings. */
    Set<FrameworkBinding> bindings();

    /** Publication / authority URL (NIST, ISO, etc.). */
    String authorityUrl();
}
