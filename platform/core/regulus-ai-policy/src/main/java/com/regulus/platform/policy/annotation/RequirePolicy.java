package com.regulus.platform.policy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enforce policy checks on methods.
 * Can be applied to methods or classes to require specific policies.
 *
 * <p>Example usage:</p>
 * <pre>
 * {@literal @}RequirePolicy(policies = {"require.LEI", "require.PurposeCode"})
 * public PaymentResult processPayment(PaymentRequest request) {
 *     // ...
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePolicy {

    /**
     * List of policy names to enforce.
     * Common policies: "require.LEI", "require.PurposeCode", "require.Consent"
     */
    String[] policies() default {};

    /**
     * Whether to require all policies to pass (AND) or any policy to pass (OR).
     */
    PolicyMode mode() default PolicyMode.ALL;

    /**
     * Optional description for audit logging.
     */
    String description() default "";

    enum PolicyMode {
        ALL,  // All policies must pass
        ANY   // At least one policy must pass
    }
}
