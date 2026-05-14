package com.neullabs.regulus.llm.config;

import com.neullabs.regulus.llm.router.LlmRouter.RoutingStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for LLM providers.
 */
@ConfigurationProperties(prefix = "regulus.ai.llm")
public class LlmProperties {

    private boolean enabled = true;
    private String defaultProvider = "openai";
    private RoutingStrategy routingStrategy = RoutingStrategy.FALLBACK;
    private List<String> fallbackOrder = List.of("openai", "anthropic", "gemini");
    private Map<String, ProviderProperties> providers = Map.of();
    private CostTrackingProperties costTracking = new CostTrackingProperties();
    private ResilienceProperties resilience = new ResilienceProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public RoutingStrategy getRoutingStrategy() {
        return routingStrategy;
    }

    public void setRoutingStrategy(RoutingStrategy routingStrategy) {
        this.routingStrategy = routingStrategy;
    }

    public List<String> getFallbackOrder() {
        return fallbackOrder;
    }

    public void setFallbackOrder(List<String> fallbackOrder) {
        this.fallbackOrder = fallbackOrder;
    }

    public Map<String, ProviderProperties> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderProperties> providers) {
        this.providers = providers;
    }

    public CostTrackingProperties getCostTracking() {
        return costTracking;
    }

    public void setCostTracking(CostTrackingProperties costTracking) {
        this.costTracking = costTracking;
    }

    public ResilienceProperties getResilience() {
        return resilience;
    }

    public void setResilience(ResilienceProperties resilience) {
        this.resilience = resilience;
    }

    /**
     * Provider-specific configuration.
     */
    public static class ProviderProperties {
        private boolean enabled = false;
        private String apiKey;
        private String model;
        private Duration timeout = Duration.ofSeconds(30);

        // OpenAI/Anthropic specific
        private String baseUrl;
        private String organizationId;

        // Gemini/Vertex specific
        private String projectId;
        private String location = "us-central1";

        // Azure specific
        private String endpoint;
        private String deploymentName;
        private String apiVersion = "2024-02-01";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getOrganizationId() {
            return organizationId;
        }

        public void setOrganizationId(String organizationId) {
            this.organizationId = organizationId;
        }

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getDeploymentName() {
            return deploymentName;
        }

        public void setDeploymentName(String deploymentName) {
            this.deploymentName = deploymentName;
        }

        public String getApiVersion() {
            return apiVersion;
        }

        public void setApiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
        }
    }

    /**
     * Cost tracking configuration.
     */
    public static class CostTrackingProperties {
        private boolean enabled = true;
        private double dailyAlertThreshold = 100.0;
        private double monthlyBudget = 1000.0;
        private String currency = "USD";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getDailyAlertThreshold() {
            return dailyAlertThreshold;
        }

        public void setDailyAlertThreshold(double dailyAlertThreshold) {
            this.dailyAlertThreshold = dailyAlertThreshold;
        }

        public double getMonthlyBudget() {
            return monthlyBudget;
        }

        public void setMonthlyBudget(double monthlyBudget) {
            this.monthlyBudget = monthlyBudget;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }

    /**
     * Resilience configuration (circuit breaker, retry, rate limiting).
     */
    public static class ResilienceProperties {
        private boolean enabled = true;
        private String preset = "financial"; // defaults, high-throughput, low-latency, financial

        // Circuit breaker
        private float failureRateThreshold = 40.0f;
        private float slowCallRateThreshold = 60.0f;
        private long slowCallDurationSeconds = 20;
        private long waitDurationInOpenStateSeconds = 60;
        private int permittedCallsInHalfOpenState = 3;
        private int minimumNumberOfCalls = 10;
        private int slidingWindowSize = 20;

        // Retry
        private int maxRetryAttempts = 3;
        private long retryWaitDurationMs = 1000;
        private double retryExponentialMultiplier = 2.0;

        // Rate limiter
        private int rateLimitRefreshPeriodSeconds = 1;
        private int rateLimitForPeriod = 100;
        private int rateLimitTimeoutSeconds = 5;

        // Timeout
        private long timeoutSeconds = 120;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPreset() {
            return preset;
        }

        public void setPreset(String preset) {
            this.preset = preset;
        }

        public float getFailureRateThreshold() {
            return failureRateThreshold;
        }

        public void setFailureRateThreshold(float failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
        }

        public float getSlowCallRateThreshold() {
            return slowCallRateThreshold;
        }

        public void setSlowCallRateThreshold(float slowCallRateThreshold) {
            this.slowCallRateThreshold = slowCallRateThreshold;
        }

        public long getSlowCallDurationSeconds() {
            return slowCallDurationSeconds;
        }

        public void setSlowCallDurationSeconds(long slowCallDurationSeconds) {
            this.slowCallDurationSeconds = slowCallDurationSeconds;
        }

        public long getWaitDurationInOpenStateSeconds() {
            return waitDurationInOpenStateSeconds;
        }

        public void setWaitDurationInOpenStateSeconds(long waitDurationInOpenStateSeconds) {
            this.waitDurationInOpenStateSeconds = waitDurationInOpenStateSeconds;
        }

        public int getPermittedCallsInHalfOpenState() {
            return permittedCallsInHalfOpenState;
        }

        public void setPermittedCallsInHalfOpenState(int permittedCallsInHalfOpenState) {
            this.permittedCallsInHalfOpenState = permittedCallsInHalfOpenState;
        }

        public int getMinimumNumberOfCalls() {
            return minimumNumberOfCalls;
        }

        public void setMinimumNumberOfCalls(int minimumNumberOfCalls) {
            this.minimumNumberOfCalls = minimumNumberOfCalls;
        }

        public int getSlidingWindowSize() {
            return slidingWindowSize;
        }

        public void setSlidingWindowSize(int slidingWindowSize) {
            this.slidingWindowSize = slidingWindowSize;
        }

        public int getMaxRetryAttempts() {
            return maxRetryAttempts;
        }

        public void setMaxRetryAttempts(int maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
        }

        public long getRetryWaitDurationMs() {
            return retryWaitDurationMs;
        }

        public void setRetryWaitDurationMs(long retryWaitDurationMs) {
            this.retryWaitDurationMs = retryWaitDurationMs;
        }

        public double getRetryExponentialMultiplier() {
            return retryExponentialMultiplier;
        }

        public void setRetryExponentialMultiplier(double retryExponentialMultiplier) {
            this.retryExponentialMultiplier = retryExponentialMultiplier;
        }

        public int getRateLimitRefreshPeriodSeconds() {
            return rateLimitRefreshPeriodSeconds;
        }

        public void setRateLimitRefreshPeriodSeconds(int rateLimitRefreshPeriodSeconds) {
            this.rateLimitRefreshPeriodSeconds = rateLimitRefreshPeriodSeconds;
        }

        public int getRateLimitForPeriod() {
            return rateLimitForPeriod;
        }

        public void setRateLimitForPeriod(int rateLimitForPeriod) {
            this.rateLimitForPeriod = rateLimitForPeriod;
        }

        public int getRateLimitTimeoutSeconds() {
            return rateLimitTimeoutSeconds;
        }

        public void setRateLimitTimeoutSeconds(int rateLimitTimeoutSeconds) {
            this.rateLimitTimeoutSeconds = rateLimitTimeoutSeconds;
        }

        public long getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }
}
