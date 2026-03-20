package com.regulus.platform.adk.plugins;

import java.util.Map;

/**
 * Inputs the {@link PolicyDecider} sees: the request's purpose code, subject
 * identifier, the tool or model being invoked, and any tenant-supplied
 * attributes attached to the ADK invocation context.
 */
public record PolicyContext(
        String purposeCode,
        String subjectId,
        String actor,
        String targetKind,        // "model" | "tool" | "computer"
        String targetId,
        Map<String, String> attributes) {
}
