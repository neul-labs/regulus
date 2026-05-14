package com.neullabs.regulus.killswitch.config;

import com.neullabs.regulus.killswitch.interceptor.InMemoryKillSwitchStateProvider;
import com.neullabs.regulus.killswitch.interceptor.KillSwitchInterceptor;
import com.neullabs.regulus.killswitch.interceptor.KillSwitchManager;
import com.neullabs.regulus.killswitch.interceptor.KillSwitchStateProvider;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Auto-configuration for kill switch components.
 */
@AutoConfiguration
@EnableAspectJAutoProxy
@EnableScheduling
@EnableConfigurationProperties(KillSwitchProperties.class)
@ConditionalOnProperty(name = "regulus.ai.kill-switch.enabled", havingValue = "true", matchIfMissing = true)
public class KillSwitchAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(KillSwitchAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public KillSwitchStateProvider killSwitchStateProvider(KillSwitchProperties properties) {
        String providerType = properties.getProvider().getType();

        log.info("Creating KillSwitchStateProvider of type: {}", providerType);

        // Default to in-memory for development
        // Production should configure Vault or ConfigHub
        return new InMemoryKillSwitchStateProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public KillSwitchManager killSwitchManager(ApplicationEventPublisher eventPublisher,
                                                KillSwitchStateProvider stateProvider) {
        log.info("Creating KillSwitchManager");
        return new KillSwitchManager(eventPublisher, stateProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public KillSwitchInterceptor.KillSwitchContextExtractor killSwitchContextExtractor() {
        return new DefaultKillSwitchContextExtractor();
    }

    @Bean
    @ConditionalOnMissingBean
    public KillSwitchInterceptor killSwitchInterceptor(
            KillSwitchManager killSwitchManager,
            KillSwitchInterceptor.KillSwitchContextExtractor contextExtractor) {
        log.info("Creating KillSwitchInterceptor");
        return new KillSwitchInterceptor(killSwitchManager, contextExtractor);
    }

    @Bean
    public KillSwitchRefreshTask killSwitchRefreshTask(KillSwitchManager manager,
                                                        KillSwitchProperties properties) {
        return new KillSwitchRefreshTask(manager, properties);
    }

    /**
     * Default context extractor that looks at annotations and method signatures.
     */
    static class DefaultKillSwitchContextExtractor
            implements KillSwitchInterceptor.KillSwitchContextExtractor {

        @Override
        public KillSwitchInterceptor.KillSwitchContext extract(ProceedingJoinPoint joinPoint) {
            String agentId = null;
            String modelId = null;
            String toolId = null;

            // Check for @AiAgent on the class
            Class<?> targetClass = joinPoint.getTarget().getClass();
            var agentAnnotation = targetClass.getAnnotation(
                com.neullabs.regulus.killswitch.interceptor.AiAgent.class);
            if (agentAnnotation != null) {
                agentId = agentAnnotation.id();
                modelId = agentAnnotation.defaultModel();
            }

            // Check for @AiOperation on the method
            if (joinPoint.getSignature() instanceof MethodSignature methodSig) {
                var opAnnotation = methodSig.getMethod().getAnnotation(
                    com.neullabs.regulus.killswitch.interceptor.AiOperation.class);
                if (opAnnotation != null) {
                    if (!opAnnotation.agentId().isEmpty()) {
                        agentId = opAnnotation.agentId();
                    }
                    if (!opAnnotation.modelId().isEmpty()) {
                        modelId = opAnnotation.modelId();
                    }
                    if (!opAnnotation.toolId().isEmpty()) {
                        toolId = opAnnotation.toolId();
                    }
                }
            }

            return new KillSwitchInterceptor.KillSwitchContext(agentId, modelId, toolId);
        }
    }

    /**
     * Scheduled task to refresh kill switch state from external provider.
     */
    static class KillSwitchRefreshTask {
        private final KillSwitchManager manager;
        private final KillSwitchProperties properties;

        KillSwitchRefreshTask(KillSwitchManager manager, KillSwitchProperties properties) {
            this.manager = manager;
            this.properties = properties;
        }

        @Scheduled(fixedDelayString = "${regulus.ai.kill-switch.refresh-interval:30000}")
        public void refresh() {
            manager.refreshState();
        }
    }
}
