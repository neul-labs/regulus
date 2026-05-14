/**
 * Pluggable adapters that fan Regulus evidence into vendor GRC tools.
 *
 * <p>The adapter pattern keeps the runtime path agnostic: Regulus emits one
 * canonical {@link com.regulus.platform.grc.GrcEvidenceEnvelope} per event;
 * each enabled adapter translates it to the vendor's native schema and
 * posts. Vendor-specific tenant overrides are exposed via per-adapter
 * {@code fieldMappings} maps so production deployments can bind to
 * customised GRC schemas without forking.
 *
 * <p>Adapters are <strong>opt-in</strong>. No adapter is wired by default.
 * See the Spring auto-config under
 * {@code com.regulus.platform.adk.spring} and ADR-011 for the rationale.
 */
package com.regulus.platform.grc;
