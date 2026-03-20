package com.regulus.platform.policy.guard;

import com.regulus.platform.policy.model.PolicyContext;
import com.regulus.platform.policy.model.PolicyResult;
import com.regulus.platform.policy.model.PolicyViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Policy guard enforcing consent requirements.
 * Ensures explicit consent is granted when required by purpose or lawful basis.
 */
public class ConsentPolicyGuard implements PolicyGuard {

    private static final Logger log = LoggerFactory.getLogger(ConsentPolicyGuard.class);

    public static final String POLICY_NAME = "require.Consent";

    @Override
    public String getName() {
        return POLICY_NAME;
    }

    @Override
    public PolicyResult evaluate(PolicyContext context) {
        // Check if consent-based processing is required
        boolean consentRequired = isConsentRequired(context);

        if (consentRequired && !context.isConsentGranted()) {
            log.warn("Consent policy violation: Consent required but not granted. " +
                    "purposeCode={}, lawfulBasis={}, correlationId={}",
                context.getPurposeCode().orElse("unknown"),
                context.getLawfulBasis().orElse("unknown"),
                context.getCorrelationId().orElse("unknown"));

            return PolicyResult.deny(PolicyViolation.builder()
                .policyName(POLICY_NAME)
                .violationType("CONSENT_NOT_GRANTED")
                .message("Explicit consent is required for this operation but has not been granted")
                .severity(PolicyViolation.Severity.CRITICAL)
                .correlationId(context.getCorrelationId().orElse(null))
                .build());
        }

        // Validate consent is not withdrawn
        var consentStatus = context.getAttribute("consentStatus", String.class);
        if (consentStatus.isPresent() && "WITHDRAWN".equalsIgnoreCase(consentStatus.get())) {
            log.warn("Consent policy violation: Consent has been withdrawn. correlationId={}",
                context.getCorrelationId().orElse("unknown"));

            return PolicyResult.deny(PolicyViolation.builder()
                .policyName(POLICY_NAME)
                .violationType("CONSENT_WITHDRAWN")
                .message("Consent for this operation has been withdrawn")
                .severity(PolicyViolation.Severity.CRITICAL)
                .correlationId(context.getCorrelationId().orElse(null))
                .build());
        }

        log.debug("Consent policy passed. correlationId={}", context.getCorrelationId().orElse("unknown"));
        return PolicyResult.allow();
    }

    @Override
    public int getPriority() {
        return 95; // High priority - check consent early
    }

    private boolean isConsentRequired(PolicyContext context) {
        // Consent is required when:
        // 1. Lawful basis is explicitly "CONSENT"
        var lawfulBasis = context.getLawfulBasis();
        if (lawfulBasis.isPresent() && "CONSENT".equalsIgnoreCase(lawfulBasis.get())) {
            return true;
        }

        // 2. Purpose code is "CONSENT"
        var purposeCode = context.getPurposeCode();
        if (purposeCode.isPresent() && "CONSENT".equalsIgnoreCase(purposeCode.get())) {
            return true;
        }

        // 3. Explicit attribute indicates consent is required
        var requiresConsent = context.getAttribute("requiresConsent", Boolean.class);
        return requiresConsent.orElse(false);
    }
}
