package com.neullabs.regulus.policy.guard;

import com.neullabs.regulus.policy.model.PolicyContext;
import com.neullabs.regulus.policy.model.PolicyResult;
import com.neullabs.regulus.policy.model.PolicyViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Policy guard enforcing Purpose Code requirements for GDPR compliance.
 * Ensures requests have valid purpose codes aligned with lawful basis.
 */
public class PurposeCodePolicyGuard implements PolicyGuard {

    private static final Logger log = LoggerFactory.getLogger(PurposeCodePolicyGuard.class);

    public static final String POLICY_NAME = "require.PurposeCode";

    // Standard purpose codes aligned with GDPR Article 6 lawful bases
    private static final Set<String> VALID_PURPOSE_CODES = Set.of(
        "CONTRACT_PERFORMANCE",      // Art. 6(1)(b) - Performance of contract
        "LEGAL_OBLIGATION",          // Art. 6(1)(c) - Legal obligation
        "VITAL_INTERESTS",           // Art. 6(1)(d) - Vital interests
        "PUBLIC_INTEREST",           // Art. 6(1)(e) - Public interest
        "LEGITIMATE_INTEREST",       // Art. 6(1)(f) - Legitimate interests
        "CONSENT",                   // Art. 6(1)(a) - Consent
        "FRAUD_PREVENTION",          // Legitimate interest - fraud prevention
        "AML_KYC",                   // Legal obligation - AML/KYC
        "CUSTOMER_SERVICE",          // Contract performance
        "ACCOUNT_MANAGEMENT",        // Contract performance
        "PAYMENT_PROCESSING",        // Contract performance
        "REGULATORY_REPORTING"       // Legal obligation
    );

    @Override
    public String getName() {
        return POLICY_NAME;
    }

    @Override
    public PolicyResult evaluate(PolicyContext context) {
        var purposeCode = context.getPurposeCode();

        if (purposeCode.isEmpty()) {
            log.warn("Purpose code policy violation: Purpose code not provided. correlationId={}",
                context.getCorrelationId().orElse("unknown"));

            return PolicyResult.deny(PolicyViolation.builder()
                .policyName(POLICY_NAME)
                .violationType("MISSING_PURPOSE_CODE")
                .message("Purpose code is required for GDPR compliance but not provided")
                .severity(PolicyViolation.Severity.HIGH)
                .correlationId(context.getCorrelationId().orElse(null))
                .build());
        }

        String code = purposeCode.get().toUpperCase();

        if (!VALID_PURPOSE_CODES.contains(code)) {
            log.warn("Purpose code policy violation: Invalid purpose code. code={}, correlationId={}",
                code, context.getCorrelationId().orElse("unknown"));

            return PolicyResult.deny(PolicyViolation.builder()
                .policyName(POLICY_NAME)
                .violationType("INVALID_PURPOSE_CODE")
                .message("Purpose code '" + code + "' is not a recognized valid purpose")
                .severity(PolicyViolation.Severity.HIGH)
                .correlationId(context.getCorrelationId().orElse(null))
                .build());
        }

        // Validate lawful basis alignment if provided
        var lawfulBasis = context.getLawfulBasis();
        if (lawfulBasis.isPresent() && !isLawfulBasisAligned(code, lawfulBasis.get())) {
            log.warn("Purpose code policy violation: Lawful basis mismatch. code={}, lawfulBasis={}, correlationId={}",
                code, lawfulBasis.get(), context.getCorrelationId().orElse("unknown"));

            return PolicyResult.deny(PolicyViolation.builder()
                .policyName(POLICY_NAME)
                .violationType("LAWFUL_BASIS_MISMATCH")
                .message("Purpose code '" + code + "' does not align with lawful basis '" + lawfulBasis.get() + "'")
                .severity(PolicyViolation.Severity.MEDIUM)
                .correlationId(context.getCorrelationId().orElse(null))
                .build());
        }

        log.debug("Purpose code policy passed. code={}, correlationId={}",
            code, context.getCorrelationId().orElse("unknown"));
        return PolicyResult.allow();
    }

    @Override
    public int getPriority() {
        return 90; // High priority
    }

    private boolean isLawfulBasisAligned(String purposeCode, String lawfulBasis) {
        return switch (purposeCode) {
            case "CONTRACT_PERFORMANCE", "CUSTOMER_SERVICE", "ACCOUNT_MANAGEMENT", "PAYMENT_PROCESSING" ->
                "CONTRACT".equalsIgnoreCase(lawfulBasis);
            case "LEGAL_OBLIGATION", "AML_KYC", "REGULATORY_REPORTING" ->
                "LEGAL_OBLIGATION".equalsIgnoreCase(lawfulBasis);
            case "CONSENT" ->
                "CONSENT".equalsIgnoreCase(lawfulBasis);
            case "LEGITIMATE_INTEREST", "FRAUD_PREVENTION" ->
                "LEGITIMATE_INTEREST".equalsIgnoreCase(lawfulBasis);
            case "VITAL_INTERESTS" ->
                "VITAL_INTERESTS".equalsIgnoreCase(lawfulBasis);
            case "PUBLIC_INTEREST" ->
                "PUBLIC_INTEREST".equalsIgnoreCase(lawfulBasis);
            default -> true; // Allow unknown combinations
        };
    }
}
