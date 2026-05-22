/**
 * Opt-in tamper-evidence for audit events. Each appended event carries the
 * hash of the previous event's canonical form, producing a Merkle-style
 * chain that an auditor can verify offline. Per-event signatures are
 * optional and use the same
 * {@link com.neullabs.regulus.identity.crypto.KeyProvider} as A2A signing.
 *
 * <p>This subsystem is dormant by default — regulated tenants flip
 * {@code regulus.ai.observability.audit.integrity.enabled=true} to switch
 * it on.
 */
package com.neullabs.regulus.observability.audit.integrity;
