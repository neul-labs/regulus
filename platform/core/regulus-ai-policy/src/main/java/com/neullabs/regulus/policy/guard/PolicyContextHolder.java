package com.neullabs.regulus.policy.guard;

import com.neullabs.regulus.policy.model.PolicyContext;

/**
 * Thread-local holder for PolicyContext, enabling context propagation
 * through the request lifecycle without explicit parameter passing.
 */
public final class PolicyContextHolder {

    private static final ThreadLocal<PolicyContext> contextHolder = new ThreadLocal<>();

    private PolicyContextHolder() {
        // Utility class
    }

    /**
     * Set the current policy context for this thread.
     */
    public static void setContext(PolicyContext context) {
        contextHolder.set(context);
    }

    /**
     * Get the current policy context for this thread.
     *
     * @return the current context, or a new empty context if none set
     */
    public static PolicyContext getContext() {
        PolicyContext context = contextHolder.get();
        if (context == null) {
            context = new PolicyContext();
            contextHolder.set(context);
        }
        return context;
    }

    /**
     * Check if a context has been explicitly set.
     */
    public static boolean hasContext() {
        return contextHolder.get() != null;
    }

    /**
     * Clear the context for this thread.
     * Should be called at request completion to prevent memory leaks.
     */
    public static void clear() {
        contextHolder.remove();
    }

    /**
     * Execute a runnable with a specific context, restoring the previous context afterward.
     */
    public static void withContext(PolicyContext context, Runnable action) {
        PolicyContext previous = contextHolder.get();
        try {
            setContext(context);
            action.run();
        } finally {
            if (previous != null) {
                setContext(previous);
            } else {
                clear();
            }
        }
    }
}
