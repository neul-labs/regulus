package com.regulus.platform.killswitch.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an AI Agent, making all its methods subject to kill switch checks.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AiAgent {

    /**
     * Unique identifier for this agent.
     */
    String id();

    /**
     * Human-readable name for the agent.
     */
    String name() default "";

    /**
     * Description of the agent's purpose.
     */
    String description() default "";

    /**
     * Default model ID used by this agent.
     */
    String defaultModel() default "";
}
