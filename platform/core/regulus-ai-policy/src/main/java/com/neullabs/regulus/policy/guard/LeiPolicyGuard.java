package com.neullabs.regulus.policy.guard;

import com.neullabs.regulus.policy.model.PolicyContext;
import com.neullabs.regulus.policy.model.PolicyResult;
import com.neullabs.regulus.policy.model.PolicyViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Policy guard enforcing Legal Entity Identifier (LEI) requirements.
 * Validates presence and format of LEI according to ISO 17442 standard.
 */
public class LeiPolicyGuard implements PolicyGuard {

    private static final Logger log = LoggerFactory.getLogger(LeiPolicyGuard.class);

    public static final String POLICY_NAME = "require.LEI";

    // ISO 17442 LEI format: 20 alphanumeric characters
    private static final Pattern LEI_PATTERN = Pattern.compile("^[A-Z0-9]{18}[0-9]{2}$");

    @Override
    public String getName() {
        return POLICY_NAME;
    }

    @Override
    public PolicyResult evaluate(PolicyContext context) {
        var lei = context.getLegalEntityIdentifier();

        if (lei.isEmpty()) {
            log.warn("LEI policy violation: LEI not provided. correlationId={}",
                context.getCorrelationId().orElse("unknown"));

            return PolicyResult.deny(PolicyViolation.builder()
                .policyName(POLICY_NAME)
                .violationType("MISSING_LEI")
                .message("Legal Entity Identifier (LEI) is required but not provided")
                .severity(PolicyViolation.Severity.HIGH)
                .correlationId(context.getCorrelationId().orElse(null))
                .build());
        }

        String leiValue = lei.get();

        if (!isValidLeiFormat(leiValue)) {
            log.warn("LEI policy violation: Invalid LEI format. lei={}, correlationId={}",
                maskLei(leiValue), context.getCorrelationId().orElse("unknown"));

            return PolicyResult.deny(PolicyViolation.builder()
                .policyName(POLICY_NAME)
                .violationType("INVALID_LEI_FORMAT")
                .message("Legal Entity Identifier (LEI) format is invalid per ISO 17442")
                .severity(PolicyViolation.Severity.HIGH)
                .correlationId(context.getCorrelationId().orElse(null))
                .build());
        }

        log.debug("LEI policy passed. correlationId={}", context.getCorrelationId().orElse("unknown"));
        return PolicyResult.allow();
    }

    @Override
    public int getPriority() {
        return 100; // High priority - validate early
    }

    private boolean isValidLeiFormat(String lei) {
        if (lei == null || lei.length() != 20) {
            return false;
        }
        return LEI_PATTERN.matcher(lei.toUpperCase()).matches();
    }

    private String maskLei(String lei) {
        if (lei == null || lei.length() < 8) {
            return "****";
        }
        return lei.substring(0, 4) + "****" + lei.substring(lei.length() - 4);
    }
}
