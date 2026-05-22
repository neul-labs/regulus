/**
 * Bridges the canonical {@link com.neullabs.regulus.identity.Identity} into the
 * two legacy {@code PolicyContext} shapes that policy guards and ADK plugins
 * currently consume.
 *
 * <p>Long-term direction: {@link com.neullabs.regulus.policy.model.PolicyContext}
 * is the surviving type; {@link com.neullabs.regulus.adk.plugins.PolicyContext}
 * is {@code @Deprecated} and will be removed once callers migrate. This module
 * is the one place that knows both shapes — keeping the dependency from leaking
 * into every consumer of {@code regulus-ai-identity}.
 */
package com.neullabs.regulus.identity.bridge;
