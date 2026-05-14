package com.neullabs.regulus.killswitch.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an AI operation that should be subject to kill switch checks.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AiOperation {

    /**
     * Agent ID for scoped kill switch checks.
     */
    String agentId() default "";

    /**
     * Model ID for scoped kill switch checks.
     */
    String modelId() default "";

    /**
     * Tool ID for scoped kill switch checks.
     */
    String toolId() default "";

    /**
     * Description of the operation for logging.
     */
    String description() default "";
}
