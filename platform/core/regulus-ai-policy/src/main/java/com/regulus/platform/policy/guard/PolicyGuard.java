package com.regulus.platform.policy.guard;

import com.regulus.platform.policy.model.PolicyContext;
import com.regulus.platform.policy.model.PolicyResult;

/**
 * Base interface for policy guards that enforce governance rules.
 * Implementations evaluate requests against specific policies.
 */
public interface PolicyGuard {

    /**
     * Unique name identifying this policy guard.
     */
    String getName();

    /**
     * Evaluate the given context against this policy.
     *
     * @param context the policy context containing request metadata
     * @return result indicating whether the policy passed or failed
     */
    PolicyResult evaluate(PolicyContext context);

    /**
     * Check if this guard handles the specified policy name.
     */
    default boolean handles(String policyName) {
        return getName().equals(policyName);
    }

    /**
     * Priority for ordering guards (higher = earlier evaluation).
     */
    default int getPriority() {
        return 0;
    }
}
