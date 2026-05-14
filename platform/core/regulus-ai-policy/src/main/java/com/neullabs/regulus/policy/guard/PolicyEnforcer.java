package com.neullabs.regulus.policy.guard;

import com.neullabs.regulus.policy.model.PolicyContext;
import com.neullabs.regulus.policy.model.PolicyResult;
import com.neullabs.regulus.policy.model.PolicyViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Central service for enforcing policies via registered PolicyGuards.
 * Orchestrates policy evaluation and aggregates results.
 */
public class PolicyEnforcer {

    private static final Logger log = LoggerFactory.getLogger(PolicyEnforcer.class);

    private final List<PolicyGuard> guards;

    public PolicyEnforcer(List<PolicyGuard> guards) {
        this.guards = guards.stream()
            .sorted(Comparator.comparingInt(PolicyGuard::getPriority).reversed())
            .toList();
        log.info("PolicyEnforcer initialized with {} guards: {}",
            guards.size(),
            guards.stream().map(PolicyGuard::getName).toList());
    }

    /**
     * Enforce all registered policies against the given context.
     */
    public PolicyResult enforceAll(PolicyContext context) {
        log.debug("Enforcing all {} policies. correlationId={}",
            guards.size(), context.getCorrelationId().orElse("unknown"));

        List<PolicyViolation> violations = new ArrayList<>();

        for (PolicyGuard guard : guards) {
            PolicyResult result = guard.evaluate(context);
            if (result.isDenied()) {
                violations.addAll(result.getViolations());
            }
        }

        if (!violations.isEmpty()) {
            log.warn("Policy enforcement failed with {} violations. correlationId={}",
                violations.size(), context.getCorrelationId().orElse("unknown"));
            return PolicyResult.deny(violations);
        }

        log.debug("All policies passed. correlationId={}", context.getCorrelationId().orElse("unknown"));
        return PolicyResult.allow();
    }

    /**
     * Enforce specific policies by name.
     */
    public PolicyResult enforce(PolicyContext context, String... policyNames) {
        log.debug("Enforcing {} specific policies. correlationId={}",
            policyNames.length, context.getCorrelationId().orElse("unknown"));

        List<PolicyViolation> violations = new ArrayList<>();

        for (String policyName : policyNames) {
            PolicyGuard guard = findGuard(policyName);
            if (guard == null) {
                log.warn("Unknown policy '{}' requested. correlationId={}",
                    policyName, context.getCorrelationId().orElse("unknown"));
                violations.add(PolicyViolation.builder()
                    .policyName(policyName)
                    .violationType("UNKNOWN_POLICY")
                    .message("Policy '" + policyName + "' is not registered")
                    .severity(PolicyViolation.Severity.HIGH)
                    .correlationId(context.getCorrelationId().orElse(null))
                    .build());
                continue;
            }

            PolicyResult result = guard.evaluate(context);
            if (result.isDenied()) {
                violations.addAll(result.getViolations());
            }
        }

        if (!violations.isEmpty()) {
            return PolicyResult.deny(violations);
        }
        return PolicyResult.allow();
    }

    /**
     * Enforce policies where at least one must pass.
     */
    public PolicyResult enforceAny(PolicyContext context, String... policyNames) {
        log.debug("Enforcing ANY of {} policies. correlationId={}",
            policyNames.length, context.getCorrelationId().orElse("unknown"));

        List<PolicyViolation> allViolations = new ArrayList<>();

        for (String policyName : policyNames) {
            PolicyGuard guard = findGuard(policyName);
            if (guard == null) {
                continue;
            }

            PolicyResult result = guard.evaluate(context);
            if (result.isAllowed()) {
                log.debug("Policy '{}' passed (ANY mode). correlationId={}",
                    policyName, context.getCorrelationId().orElse("unknown"));
                return PolicyResult.allow();
            }
            allViolations.addAll(result.getViolations());
        }

        log.warn("No policies passed in ANY mode. correlationId={}",
            context.getCorrelationId().orElse("unknown"));
        return PolicyResult.deny(allViolations);
    }

    private PolicyGuard findGuard(String policyName) {
        return guards.stream()
            .filter(g -> g.handles(policyName))
            .findFirst()
            .orElse(null);
    }

    public List<String> getRegisteredPolicies() {
        return guards.stream().map(PolicyGuard::getName).toList();
    }
}
