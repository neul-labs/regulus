package com.regulus.platform.governance.autoconfigure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that require governance policy enforcement.
 * Methods annotated with @Governed will have policy checks applied via AOP.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Governed {

    /**
     * Whether LEI validation is required.
     */
    boolean requireLei() default true;

    /**
     * Whether consent verification is required.
     */
    boolean requireConsent() default true;

    /**
     * Whether purpose code validation is required.
     */
    boolean requirePurposeCode() default true;

    /**
     * Custom policy names to apply (in addition to default guards).
     */
    String[] policies() default {};

    /**
     * Description for audit logging.
     */
    String description() default "";
}
