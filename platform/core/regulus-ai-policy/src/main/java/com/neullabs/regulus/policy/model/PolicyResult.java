package com.neullabs.regulus.policy.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of policy evaluation containing any violations found.
 */
public class PolicyResult {

    private final boolean allowed;
    private final List<PolicyViolation> violations;

    private PolicyResult(boolean allowed, List<PolicyViolation> violations) {
        this.allowed = allowed;
        this.violations = Collections.unmodifiableList(violations);
    }

    public static PolicyResult allow() {
        return new PolicyResult(true, Collections.emptyList());
    }

    public static PolicyResult deny(PolicyViolation violation) {
        return new PolicyResult(false, List.of(violation));
    }

    public static PolicyResult deny(List<PolicyViolation> violations) {
        return new PolicyResult(false, new ArrayList<>(violations));
    }

    public boolean isAllowed() {
        return allowed;
    }

    public boolean isDenied() {
        return !allowed;
    }

    public List<PolicyViolation> getViolations() {
        return violations;
    }

    public PolicyResult merge(PolicyResult other) {
        if (this.allowed && other.allowed) {
            return allow();
        }
        List<PolicyViolation> combined = new ArrayList<>(this.violations);
        combined.addAll(other.violations);
        return deny(combined);
    }
}
