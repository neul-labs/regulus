package com.neullabs.regulus.observability.config;

import com.neullabs.regulus.identity.crypto.KeyProvider;
import com.neullabs.regulus.observability.audit.AuditLogger;
import com.neullabs.regulus.observability.audit.integrity.AuditChain;
import com.neullabs.regulus.observability.audit.integrity.HashChainAuditChain;
import com.neullabs.regulus.observability.metrics.AiMetrics;
import com.neullabs.regulus.observability.tracing.AiTracing;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for observability components.
 */
@AutoConfiguration
@EnableConfigurationProperties(ObservabilityProperties.class)
@ConditionalOnProperty(name = "regulus.ai.observability.enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnProperty(name = "regulus.ai.observability.metrics.enabled", havingValue = "true", matchIfMissing = true)
    public AiMetrics aiMetrics(MeterRegistry registry) {
        log.info("Creating AiMetrics with Micrometer registry");
        return new AiMetrics(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "io.opentelemetry.api.trace.Tracer")
    @ConditionalOnProperty(name = "regulus.ai.observability.tracing.enabled", havingValue = "true", matchIfMissing = true)
    public AiTracing aiTracing() {
        log.info("Creating AiTracing with OpenTelemetry");
        return new AiTracing();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "regulus.ai.observability.audit.integrity.enabled", havingValue = "true")
    public AuditChain auditChain(ObjectProvider<KeyProvider> keyProviders) {
        KeyProvider provider = keyProviders.getIfAvailable();
        log.info("Audit integrity enabled — using HashChainAuditChain (signing {})",
                provider == null ? "disabled" : "enabled");
        return new HashChainAuditChain(provider);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "regulus.ai.observability.audit.enabled", havingValue = "true", matchIfMissing = true)
    public AuditLogger auditLogger(ObservabilityProperties properties, ObjectProvider<AuditChain> chains) {
        log.info("Creating AuditLogger with sink: {}", properties.getAudit().getSink());
        AuditChain chain = chains.getIfAvailable();
        AuditLogger logger = new AuditLogger(chain);

        // Add Kafka sink if configured
        if ("kafka".equals(properties.getAudit().getSink()) ||
            "both".equals(properties.getAudit().getSink())) {
            log.info("Kafka audit sink configured - will be initialized when Kafka is available");
        }

        return logger;
    }
}
