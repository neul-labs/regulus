/**
 * Cross-org A2A request signing. Outbound calls produced by
 * {@code RegulusRemoteA2AAgent} can be signed with RFC 9421 HTTP Message
 * Signatures so the receiving organisation can verify the caller's
 * {@link com.neullabs.regulus.identity.Identity}; inbound calls reaching
 * {@code RegulusAgentExecutor} are verified through the same SPI before any
 * policy guard runs.
 *
 * <p>{@link com.neullabs.regulus.adk.a2a.signing.A2ARequestSigner} is the
 * single SPI seam. Key material is supplied via
 * {@link com.neullabs.regulus.identity.crypto.KeyProvider} (shared with
 * audit chain signing — tenants configure key management once).
 */
package com.neullabs.regulus.adk.a2a.signing;
