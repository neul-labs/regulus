package com.regulus.platform.llm.router;

import com.regulus.platform.llm.LlmClient;
import com.regulus.platform.llm.LlmRequest;
import com.regulus.platform.llm.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Routes LLM requests to appropriate providers with fallback support.
 * Supports multiple routing strategies: primary, round-robin, cost-optimized, latency-optimized.
 */
public class LlmRouter implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmRouter.class);

    private final Map<String, LlmClient> providers;
    private final List<String> fallbackOrder;
    private final RoutingStrategy strategy;
    private final String defaultProvider;

    // For round-robin routing
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    // Track provider health
    private final Map<String, ProviderHealth> healthStatus = new ConcurrentHashMap<>();

    public LlmRouter(
            Map<String, LlmClient> providers,
            String defaultProvider,
            List<String> fallbackOrder,
            RoutingStrategy strategy) {
        this.providers = providers;
        this.defaultProvider = defaultProvider;
        this.fallbackOrder = fallbackOrder;
        this.strategy = strategy;

        // Initialize health status
        providers.keySet().forEach(name ->
            healthStatus.put(name, new ProviderHealth(name, true, 0, 0)));

        log.info("LLM Router initialized: default={}, strategy={}, providers={}",
            defaultProvider, strategy, providers.keySet());
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        List<String> providersToTry = getProvidersForStrategy();

        for (String providerName : providersToTry) {
            LlmClient client = providers.get(providerName);
            if (client == null || !client.isAvailable()) {
                log.debug("Provider {} not available, trying next", providerName);
                continue;
            }

            if (!isProviderHealthy(providerName)) {
                log.debug("Provider {} marked unhealthy, trying next", providerName);
                continue;
            }

            try {
                log.debug("Routing request {} to provider {}", request.id(), providerName);
                LlmResponse response = client.generate(request);

                if (response.success()) {
                    recordSuccess(providerName);
                    return response;
                } else {
                    log.warn("Provider {} returned error: {}", providerName, response.errorMessage());
                    recordFailure(providerName);
                }
            } catch (Exception e) {
                log.error("Provider {} threw exception", providerName, e);
                recordFailure(providerName);
            }
        }

        return LlmResponse.error(request.id(), "router", "All providers failed or unavailable");
    }

    @Override
    public CompletableFuture<LlmResponse> generateAsync(LlmRequest request) {
        return CompletableFuture.supplyAsync(() -> generate(request));
    }

    @Override
    public LlmResponse generateStreaming(LlmRequest request, StreamingHandler handler) {
        LlmClient primary = providers.get(defaultProvider);
        if (primary != null && primary.isAvailable()) {
            return primary.generateStreaming(request, handler);
        }

        // Fallback to non-streaming
        LlmResponse response = generate(request);
        if (response.success() && response.content() != null) {
            handler.onToken(response.content());
        }
        return response;
    }

    @Override
    public String getProviderName() {
        return "router";
    }

    @Override
    public String getModelName() {
        LlmClient primary = providers.get(defaultProvider);
        return primary != null ? primary.getModelName() : "unknown";
    }

    @Override
    public boolean isAvailable() {
        return providers.values().stream().anyMatch(LlmClient::isAvailable);
    }

    @Override
    public List<LlmCapability> getCapabilities() {
        // Return union of all provider capabilities
        return providers.values().stream()
            .filter(LlmClient::isAvailable)
            .flatMap(c -> c.getCapabilities().stream())
            .distinct()
            .toList();
    }

    /**
     * Get a specific provider by name.
     */
    public Optional<LlmClient> getProvider(String name) {
        return Optional.ofNullable(providers.get(name));
    }

    /**
     * Get all registered providers.
     */
    public Map<String, LlmClient> getProviders() {
        return Map.copyOf(providers);
    }

    /**
     * Get health status for all providers.
     */
    public Map<String, ProviderHealth> getHealthStatus() {
        return Map.copyOf(healthStatus);
    }

    /**
     * Force a provider to be marked as healthy (for recovery).
     */
    public void markHealthy(String providerName) {
        ProviderHealth health = healthStatus.get(providerName);
        if (health != null) {
            healthStatus.put(providerName, new ProviderHealth(
                providerName, true, 0, health.totalRequests()
            ));
            log.info("Provider {} marked healthy", providerName);
        }
    }

    private List<String> getProvidersForStrategy() {
        return switch (strategy) {
            case PRIMARY_ONLY -> List.of(defaultProvider);
            case FALLBACK -> {
                List<String> order = new java.util.ArrayList<>();
                order.add(defaultProvider);
                for (String provider : fallbackOrder) {
                    if (!provider.equals(defaultProvider)) {
                        order.add(provider);
                    }
                }
                yield order;
            }
            case ROUND_ROBIN -> {
                List<String> available = providers.entrySet().stream()
                    .filter(e -> e.getValue().isAvailable())
                    .map(Map.Entry::getKey)
                    .toList();
                if (available.isEmpty()) yield List.of();
                int index = roundRobinIndex.getAndIncrement() % available.size();
                // Start from index, then wrap around
                List<String> result = new java.util.ArrayList<>();
                for (int i = 0; i < available.size(); i++) {
                    result.add(available.get((index + i) % available.size()));
                }
                yield result;
            }
            case COST_OPTIMIZED -> {
                // Prefer cheaper models first
                // Order: gemini-flash, openai-gpt-4o-mini, anthropic-haiku, then others
                List<String> costOrder = List.of("gemini", "openai", "anthropic", "azure");
                yield costOrder.stream()
                    .filter(providers::containsKey)
                    .toList();
            }
            case LATENCY_OPTIMIZED -> {
                // Sort by average latency (healthiest first)
                yield healthStatus.values().stream()
                    .filter(h -> h.healthy)
                    .sorted((a, b) -> Double.compare(
                        a.failureCount() / Math.max(1.0, a.totalRequests()),
                        b.failureCount() / Math.max(1.0, b.totalRequests())
                    ))
                    .map(ProviderHealth::name)
                    .toList();
            }
        };
    }

    private boolean isProviderHealthy(String providerName) {
        ProviderHealth health = healthStatus.get(providerName);
        if (health == null) return true;

        // Consider unhealthy if >50% failure rate in last 10 requests
        if (health.totalRequests() >= 10) {
            double failureRate = (double) health.failureCount() / health.totalRequests();
            return failureRate < 0.5;
        }

        return health.healthy();
    }

    private void recordSuccess(String providerName) {
        ProviderHealth current = healthStatus.get(providerName);
        if (current != null) {
            healthStatus.put(providerName, new ProviderHealth(
                providerName,
                true,
                Math.max(0, current.failureCount() - 1), // Decay failures on success
                current.totalRequests() + 1
            ));
        }
    }

    private void recordFailure(String providerName) {
        ProviderHealth current = healthStatus.get(providerName);
        if (current != null) {
            int failures = current.failureCount() + 1;
            boolean healthy = failures < 5; // Mark unhealthy after 5 consecutive failures
            healthStatus.put(providerName, new ProviderHealth(
                providerName,
                healthy,
                failures,
                current.totalRequests() + 1
            ));

            if (!healthy) {
                log.warn("Provider {} marked unhealthy after {} failures", providerName, failures);
            }
        }
    }

    /**
     * Routing strategy for LLM requests.
     */
    public enum RoutingStrategy {
        PRIMARY_ONLY,     // Only use default provider
        FALLBACK,         // Use default, fall back to others on failure
        ROUND_ROBIN,      // Distribute across providers
        COST_OPTIMIZED,   // Prefer cheaper providers
        LATENCY_OPTIMIZED // Prefer faster/healthier providers
    }

    /**
     * Health status for a provider.
     */
    public record ProviderHealth(
        String name,
        boolean healthy,
        int failureCount,
        int totalRequests
    ) {}
}
