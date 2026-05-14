/**
 * Regulus envelope for ADK's A2A (Agent-to-Agent) protocol primitives.
 *
 * <p>{@link com.neullabs.regulus.adk.a2a.RegulusAgentExecutor} guards inbound
 * JSON-RPC calls; {@link com.neullabs.regulus.adk.a2a.RegulusRemoteA2AAgent}
 * guards outbound calls. Together they ensure that every agent hop — local or
 * cross-organisation — passes through the same policy, privacy, audit, and
 * kill-switch envelope as a direct invocation.
 */
package com.neullabs.regulus.adk.a2a;
