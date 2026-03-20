package com.regulus.platform.adk.plugins;

import com.regulus.platform.compliance.ComplianceProfile;
import com.regulus.platform.compliance.ControlBinding;

final class DefaultPolicyEngine {

    private DefaultPolicyEngine() {}

    static PolicyDecision evaluate(ComplianceProfile profile, PolicyContext context) {
        for (ControlBinding binding : profile.controls()) {
            switch (binding.mechanism()) {
                case "purpose-binding" -> {
                    if (context.purposeCode() == null || context.purposeCode().isBlank()) {
                        return new PolicyDecision.Block(
                                "missing_purpose",
                                "Purpose code is required under " + profile.displayName(),
                                binding.clause());
                    }
                }
                case "automated-decisions-safeguards" -> {
                    if ("model".equals(context.targetKind())
                            && "true".equals(context.attributes().getOrDefault("automated_legal_effect", "false"))) {
                        return new PolicyDecision.RequireConfirmation(
                                "art_22_safeguard",
                                "Automated decision with legal effect requires human intervention (" + binding.clause() + ")");
                    }
                }
                case "vulnerable-customer-handling" -> {
                    if ("true".equals(context.attributes().getOrDefault("vulnerable_customer", "false"))) {
                        return new PolicyDecision.RequireConfirmation(
                                "vulnerable_customer",
                                "Enhanced HITL required for vulnerable customer interactions (" + binding.clause() + ")");
                    }
                }
                default -> { /* binding is informational or enforced elsewhere */ }
            }
        }
        return PolicyDecision.ALLOW;
    }
}
