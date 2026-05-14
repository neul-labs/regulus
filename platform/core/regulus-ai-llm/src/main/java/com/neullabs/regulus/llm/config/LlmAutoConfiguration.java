package com.neullabs.regulus.llm.config;

import com.neullabs.regulus.llm.LlmClient;
import com.neullabs.regulus.llm.cost.CostCalculator;
import com.neullabs.regulus.llm.cost.TokenCounter;
import com.neullabs.regulus.llm.provider.AnthropicLlmClient;
import com.neullabs.regulus.llm.provider.GeminiLlmClient;
import com.neullabs.regulus.llm.provider.OpenAiLlmClient;
import com.neullabs.regulus.llm.resilience.ResilienceConfig;
import com.neullabs.regulus.llm.resilience.ResilientLlmClient;
import com.neullabs.regulus.llm.router.LlmRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

/**
 * Auto-configuration for LLM providers.
 * Creates LlmClient beans based on configuration.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "regulus.ai.llm", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LlmProperties.class)
public class LlmAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LlmAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public TokenCounter tokenCounter() {
        return new TokenCounter();
    }

    @Bean
    @ConditionalOnMissingBean
    public CostCalculator costCalculator(TokenCounter tokenCounter) {
        return new CostCalculator(tokenCounter);
    }

    @Bean
    @ConditionalOnMissingBean
    public ResilienceConfig resilienceConfig(LlmProperties properties) {
        LlmProperties.ResilienceProperties rp = properties.getResilience();

        // Use preset if specified, otherwise build from properties
        if (rp.getPreset() != null && !rp.getPreset().isBlank()) {
            return switch (rp.getPreset().toLowerCase()) {
                case "high-throughput" -> ResilienceConfig.highThroughput();
                case "low-latency" -> ResilienceConfig.lowLatency();
                case "financial" -> ResilienceConfig.financial();
                default -> ResilienceConfig.defaults();
            };
        }

        return ResilienceConfig.builder()
            .failureRateThreshold(rp.getFailureRateThreshold())
            .slowCallRateThreshold(rp.getSlowCallRateThreshold())
            .slowCallDurationSeconds(rp.getSlowCallDurationSeconds())
            .waitDurationInOpenStateSeconds(rp.getWaitDurationInOpenStateSeconds())
            .permittedCallsInHalfOpenState(rp.getPermittedCallsInHalfOpenState())
            .minimumNumberOfCalls(rp.getMinimumNumberOfCalls())
            .slidingWindowSize(rp.getSlidingWindowSize())
            .maxRetryAttempts(rp.getMaxRetryAttempts())
            .retryWaitDurationMs(rp.getRetryWaitDurationMs())
            .retryExponentialMultiplier(rp.getRetryExponentialMultiplier())
            .rateLimitRefreshPeriodSeconds(rp.getRateLimitRefreshPeriodSeconds())
            .rateLimitForPeriod(rp.getRateLimitForPeriod())
            .rateLimitTimeoutSeconds(rp.getRateLimitTimeoutSeconds())
            .timeoutSeconds(rp.getTimeoutSeconds())
            .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "openAiLlmClient")
    @ConditionalOnProperty(prefix = "regulus.ai.llm.providers.openai", name = "enabled", havingValue = "true")
    public LlmClient openAiLlmClient(LlmProperties properties, ResilienceConfig resilienceConfig) {
        LlmProperties.ProviderProperties config = properties.getProviders().get("openai");
        log.info("Creating OpenAI LLM client: model={}", config.getModel());
        LlmClient client = new OpenAiLlmClient(
            config.getApiKey(),
            config.getModel() != null ? config.getModel() : "gpt-4o",
            config.getTimeout()
        );
        return wrapWithResilience(client, properties.getResilience().isEnabled(), resilienceConfig);
    }

    @Bean
    @ConditionalOnMissingBean(name = "anthropicLlmClient")
    @ConditionalOnProperty(prefix = "regulus.ai.llm.providers.anthropic", name = "enabled", havingValue = "true")
    public LlmClient anthropicLlmClient(LlmProperties properties, ResilienceConfig resilienceConfig) {
        LlmProperties.ProviderProperties config = properties.getProviders().get("anthropic");
        log.info("Creating Anthropic LLM client: model={}", config.getModel());
        LlmClient client = new AnthropicLlmClient(
            config.getApiKey(),
            config.getModel() != null ? config.getModel() : "claude-3-5-sonnet-20241022",
            config.getTimeout()
        );
        return wrapWithResilience(client, properties.getResilience().isEnabled(), resilienceConfig);
    }

    @Bean
    @ConditionalOnMissingBean(name = "geminiLlmClient")
    @ConditionalOnProperty(prefix = "regulus.ai.llm.providers.gemini", name = "enabled", havingValue = "true")
    public LlmClient geminiLlmClient(LlmProperties properties, ResilienceConfig resilienceConfig) {
        LlmProperties.ProviderProperties config = properties.getProviders().get("gemini");
        log.info("Creating Gemini LLM client: project={}, location={}, model={}",
            config.getProjectId(), config.getLocation(), config.getModel());
        LlmClient client = new GeminiLlmClient(
            config.getProjectId(),
            config.getLocation(),
            config.getModel() != null ? config.getModel() : "gemini-1.5-pro"
        );
        return wrapWithResilience(client, properties.getResilience().isEnabled(), resilienceConfig);
    }

    private LlmClient wrapWithResilience(LlmClient client, boolean resilienceEnabled, ResilienceConfig config) {
        if (resilienceEnabled) {
            log.info("Wrapping {} with resilience (circuit breaker, retry, rate limiter)", client.getProviderName());
            return new ResilientLlmClient(client, config);
        }
        return client;
    }

    @Bean
    @ConditionalOnMissingBean
    public LlmRouter llmRouter(LlmProperties properties, Map<String, LlmClient> llmClients) {
        Map<String, LlmClient> providers = new HashMap<>();

        // Collect all LlmClient beans
        for (Map.Entry<String, LlmClient> entry : llmClients.entrySet()) {
            String beanName = entry.getKey();
            LlmClient client = entry.getValue();

            // Skip the router itself
            if (client instanceof LlmRouter) {
                continue;
            }

            String providerName = client.getProviderName();
            providers.put(providerName, client);
            log.info("Registered LLM provider: {} (bean: {})", providerName, beanName);
        }

        if (providers.isEmpty()) {
            log.warn("No LLM providers configured. Creating router with empty providers.");
        }

        String defaultProvider = properties.getDefaultProvider();
        if (!providers.containsKey(defaultProvider) && !providers.isEmpty()) {
            defaultProvider = providers.keySet().iterator().next();
            log.warn("Default provider '{}' not available, using '{}' instead",
                properties.getDefaultProvider(), defaultProvider);
        }

        return new LlmRouter(
            providers,
            defaultProvider,
            properties.getFallbackOrder(),
            properties.getRoutingStrategy()
        );
    }
}
