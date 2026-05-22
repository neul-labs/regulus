package com.neullabs.regulus.adk.plugins;

import java.util.Map;

/**
 * Inputs the {@link PolicyDecider} sees: the request's purpose code, subject
 * identifier, the tool or model being invoked, and any tenant-supplied
 * attributes attached to the ADK invocation context.
 *
 * @deprecated New code should consume
 *     {@link com.neullabs.regulus.policy.model.PolicyContext} (the
 *     long-term home for policy inputs) and build it from a canonical
 *     {@link com.neullabs.regulus.identity.Identity} via
 *     {@code com.neullabs.regulus.identity.bridge.PolicyContextBridge}.
 *     This type will be removed once internal call sites migrate.
 */
@Deprecated
public record PolicyContext(
        String purposeCode,
        String subjectId,
        String actor,
        String targetKind,        // "model" | "tool" | "computer"
        String targetId,
        Map<String, String> attributes) {
}
