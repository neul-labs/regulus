package com.neullabs.regulus.killswitch.interceptor;

import com.neullabs.regulus.killswitch.model.KillSwitchState;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AOP Interceptor that checks kill switch state before AI operations.
 * Blocks execution and throws KillSwitchException if kill switch is active.
 */
@Aspect
public class KillSwitchInterceptor {

    private static final Logger log = LoggerFactory.getLogger(KillSwitchInterceptor.class);

    private final KillSwitchManager killSwitchManager;
    private final KillSwitchContextExtractor contextExtractor;

    public KillSwitchInterceptor(KillSwitchManager killSwitchManager,
                                  KillSwitchContextExtractor contextExtractor) {
        this.killSwitchManager = killSwitchManager;
        this.contextExtractor = contextExtractor;
    }

    /**
     * Intercept methods annotated with @AiOperation.
     */
    @Around("@annotation(com.neullabs.regulus.killswitch.interceptor.AiOperation)")
    public Object interceptAiOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        return checkKillSwitchAndProceed(joinPoint);
    }

    /**
     * Intercept all methods in classes annotated with @AiAgent.
     */
    @Around("@within(com.neullabs.regulus.killswitch.interceptor.AiAgent)")
    public Object interceptAiAgent(ProceedingJoinPoint joinPoint) throws Throwable {
        return checkKillSwitchAndProceed(joinPoint);
    }

    private Object checkKillSwitchAndProceed(ProceedingJoinPoint joinPoint) throws Throwable {
        // Extract context from the method/class/arguments
        KillSwitchContext context = contextExtractor.extract(joinPoint);

        // Check if blocked
        KillSwitchState blockingState = killSwitchManager.getBlockingState(
            context.agentId(),
            context.modelId(),
            context.toolId()
        );

        if (blockingState != null && blockingState.isActive()) {
            log.warn("AI operation blocked by kill switch. method={}, scope={}, reason='{}'",
                joinPoint.getSignature().toShortString(),
                blockingState.scope(),
                blockingState.reason());

            throw new KillSwitchException(blockingState);
        }

        log.trace("Kill switch check passed for method: {}",
            joinPoint.getSignature().toShortString());

        return joinPoint.proceed();
    }

    /**
     * Context extracted from the intercepted method.
     */
    public record KillSwitchContext(
        String agentId,
        String modelId,
        String toolId
    ) {
        public static KillSwitchContext empty() {
            return new KillSwitchContext(null, null, null);
        }
    }

    /**
     * Strategy for extracting kill switch context from join points.
     */
    public interface KillSwitchContextExtractor {
        KillSwitchContext extract(ProceedingJoinPoint joinPoint);
    }
}
