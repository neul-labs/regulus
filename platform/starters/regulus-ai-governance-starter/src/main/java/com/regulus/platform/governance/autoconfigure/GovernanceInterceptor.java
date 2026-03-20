package com.regulus.platform.governance.autoconfigure;

import com.regulus.platform.observability.audit.AuditLogger;
import com.regulus.platform.policy.guard.PolicyViolationException;
import com.regulus.platform.policy.guard.PolicyEnforcer;
import com.regulus.platform.policy.model.PolicyContext;
import com.regulus.platform.policy.model.PolicyResult;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * AOP interceptor for governance policy enforcement.
 * Wraps annotated methods to apply policy checks before execution.
 */
@Aspect
public class GovernanceInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GovernanceInterceptor.class);

    private final PolicyEnforcer policyEnforcer;
    private final AuditLogger auditLogger;
    private final boolean enforceMode;

    public GovernanceInterceptor(PolicyEnforcer policyEnforcer, AuditLogger auditLogger, boolean enforceMode) {
        this.policyEnforcer = policyEnforcer;
        this.auditLogger = auditLogger;
        this.enforceMode = enforceMode;
    }

    /**
     * Intercept methods annotated with @Governed.
     */
    @Around("@annotation(governed)")
    public Object enforcePolicy(ProceedingJoinPoint joinPoint, Governed governed) throws Throwable {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }

        // Extract policy context from method arguments or annotation
        PolicyContext context = extractPolicyContext(joinPoint, governed);

        if (context != null) {
            PolicyResult result = policyEnforcer.enforceAll(context);

            if (!result.isAllowed()) {
                log.warn("Policy violation detected: {}", result.getViolations());

                // Log violation
                for (var violation : result.getViolations()) {
                    auditLogger.logPolicyViolation(
                        correlationId,
                        context.getUserId().orElse("unknown"),
                        violation.policyName(),
                        violation.violationType(),
                        violation.message()
                    );
                }

                if (enforceMode) {
                    throw new PolicyViolationException(result.getViolations());
                }
            }
        }

        return joinPoint.proceed();
    }

    private PolicyContext extractPolicyContext(ProceedingJoinPoint joinPoint, Governed governed) {
        // Look for PolicyContext in method arguments
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof PolicyContext) {
                return (PolicyContext) arg;
            }
        }

        // If annotation specifies required policies but no context found, create empty context
        if (governed.requireLei() || governed.requireConsent() || governed.requirePurposeCode()) {
            return PolicyContext.builder()
                .userId("unknown")
                .build();
        }

        return null;
    }
}
