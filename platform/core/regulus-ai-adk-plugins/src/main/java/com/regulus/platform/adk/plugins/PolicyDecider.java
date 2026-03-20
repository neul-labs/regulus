package com.regulus.platform.adk.plugins;

import com.regulus.platform.compliance.ComplianceProfile;

/**
 * Decides whether a model call or tool call satisfies the active profile.
 *
 * <p>Default implementation reads {@link ComplianceProfile#controls()} and
 * enforces the mechanisms whose {@code mechanism} string matches a known
 * policy guard ({@code purpose-binding}, {@code consent-required},
 * {@code lei-format}, {@code consumer-duty-good-outcomes},
 * {@code vulnerable-customer-handling}, etc.).
 */
@FunctionalInterface
public interface PolicyDecider {

    PolicyDecision decide(PolicyContext context);

    static PolicyDecider fromProfile(ComplianceProfile profile) {
        return context -> DefaultPolicyEngine.evaluate(profile, context);
    }
}
