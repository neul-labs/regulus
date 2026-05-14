package com.neullabs.regulus.policy.guard;

import com.neullabs.regulus.policy.annotation.RequirePolicy;
import com.neullabs.regulus.policy.model.PolicyContext;
import com.neullabs.regulus.policy.model.PolicyResult;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * AOP Aspect that intercepts methods annotated with @RequirePolicy
 * and enforces the specified policies before allowing execution.
 */
@Aspect
public class PolicyGuardAspect {

    private static final Logger log = LoggerFactory.getLogger(PolicyGuardAspect.class);

    private final PolicyEnforcer policyEnforcer;

    public PolicyGuardAspect(PolicyEnforcer policyEnforcer) {
        this.policyEnforcer = policyEnforcer;
    }

    @Around("@annotation(com.neullabs.regulus.policy.annotation.RequirePolicy)")
    public Object enforceMethodPolicy(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePolicy annotation = method.getAnnotation(RequirePolicy.class);

        return enforcePolicy(joinPoint, annotation, method.getName());
    }

    @Around("@within(com.neullabs.regulus.policy.annotation.RequirePolicy)")
    public Object enforceClassPolicy(ProceedingJoinPoint joinPoint) throws Throwable {
        Class<?> targetClass = joinPoint.getTarget().getClass();
        RequirePolicy annotation = targetClass.getAnnotation(RequirePolicy.class);

        if (annotation == null) {
            // Check superclass and interfaces
            annotation = findAnnotationInHierarchy(targetClass);
        }

        if (annotation == null) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return enforcePolicy(joinPoint, annotation, targetClass.getSimpleName() + "." + signature.getName());
    }

    private Object enforcePolicy(ProceedingJoinPoint joinPoint, RequirePolicy annotation, String methodName)
            throws Throwable {

        String[] policies = annotation.policies();
        if (policies.length == 0) {
            log.debug("No specific policies required for method '{}', proceeding", methodName);
            return joinPoint.proceed();
        }

        PolicyContext context = PolicyContextHolder.getContext();
        String correlationId = context.getCorrelationId().orElse("unknown");

        log.debug("Enforcing {} policies for method '{}'. mode={}, correlationId={}",
            policies.length, methodName, annotation.mode(), correlationId);

        PolicyResult result;
        if (annotation.mode() == RequirePolicy.PolicyMode.ANY) {
            result = policyEnforcer.enforceAny(context, policies);
        } else {
            result = policyEnforcer.enforce(context, policies);
        }

        if (result.isDenied()) {
            log.warn("Policy enforcement failed for method '{}'. violations={}, correlationId={}",
                methodName, result.getViolations().size(), correlationId);

            throw new PolicyViolationException(result.getViolations());
        }

        log.debug("Policy enforcement passed for method '{}'. correlationId={}",
            methodName, correlationId);

        return joinPoint.proceed();
    }

    private RequirePolicy findAnnotationInHierarchy(Class<?> clazz) {
        // Check superclass
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            RequirePolicy annotation = superclass.getAnnotation(RequirePolicy.class);
            if (annotation != null) {
                return annotation;
            }
        }

        // Check interfaces
        for (Class<?> iface : clazz.getInterfaces()) {
            RequirePolicy annotation = iface.getAnnotation(RequirePolicy.class);
            if (annotation != null) {
                return annotation;
            }
        }

        return null;
    }
}
