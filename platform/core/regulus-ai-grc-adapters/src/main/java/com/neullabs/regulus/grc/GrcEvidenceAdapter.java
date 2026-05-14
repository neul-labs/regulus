package com.neullabs.regulus.grc;

/**
 * Pluggable emitter that delivers Regulus evidence to a specific GRC tool.
 *
 * <p>Implementations are <strong>opt-in</strong> — no adapter is wired by
 * default. Failures inside an adapter must not break the agent invocation
 * loop; the {@link RegulusGovernanceEvidencePlugin} catches and surfaces
 * adapter errors as their own audit events.
 *
 * <p>Adapters should be safe to call concurrently. Use Resilience4j retry
 * + circuit breaker on the wire side.
 */
public interface GrcEvidenceAdapter {

    /** Stable identifier, e.g. {@code "servicenow-irm"}, {@code "onetrust-ai-gov"}. */
    String vendorId();

    /** Deliver one envelope. Throws on irrecoverable failure. */
    void emit(GrcEvidenceEnvelope envelope);

    /** Reachability + credential validation; called at startup. */
    default void healthCheck() {
        // default: trust the adapter unless overridden.
    }
}
