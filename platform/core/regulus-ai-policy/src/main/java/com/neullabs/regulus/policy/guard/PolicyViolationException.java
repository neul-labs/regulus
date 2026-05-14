package com.neullabs.regulus.policy.guard;

import com.neullabs.regulus.policy.model.PolicyViolation;

import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when policy enforcement fails.
 */
public class PolicyViolationException extends RuntimeException {

    private final List<PolicyViolation> violations;

    public PolicyViolationException(PolicyViolation violation) {
        super(formatMessage(List.of(violation)));
        this.violations = List.of(violation);
    }

    public PolicyViolationException(List<PolicyViolation> violations) {
        super(formatMessage(violations));
        this.violations = Collections.unmodifiableList(violations);
    }

    public List<PolicyViolation> getViolations() {
        return violations;
    }

    public PolicyViolation getFirstViolation() {
        return violations.isEmpty() ? null : violations.get(0);
    }

    private static String formatMessage(List<PolicyViolation> violations) {
        if (violations.isEmpty()) {
            return "Policy violation occurred";
        }
        if (violations.size() == 1) {
            PolicyViolation v = violations.get(0);
            return String.format("Policy '%s' violated: %s", v.policyName(), v.message());
        }
        return String.format("%d policy violations: %s",
            violations.size(),
            violations.stream()
                .map(v -> v.policyName() + ": " + v.message())
                .toList());
    }
}
