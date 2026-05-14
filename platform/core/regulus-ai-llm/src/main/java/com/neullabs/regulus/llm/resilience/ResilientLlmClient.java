package com.neullabs.regulus.llm.resilience;

import com.neullabs.regulus.llm.LlmClient;
import com.neullabs.regulus.llm.LlmRequest;
import com.neullabs.regulus.llm.LlmResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Resilient wrapper for LLM clients with circuit breaker, retry, rate limiting, and timeout.
 * Protects against LLM provider failures and prevents cascade failures.
 */
public class ResilientLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientLlmClient.class);

    private final LlmClient delegate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final RateLimiter rateLimiter;
    private final TimeLimiter timeLimiter;
    private final ResilienceConfig config;

    public ResilientLlmClient(LlmClient delegate, ResilienceConfig config) {
        this.delegate = delegate;
        this.config = config;

        String name = delegate.getProviderName() + "-" + delegate.getModelName();

        // Circuit Breaker
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(config.getFailureRateThreshold())
            .slowCallRateThreshold(config.getSlowCallRateThreshold())
            .slowCallDurationThreshold(Duration.ofSeconds(config.getSlowCallDurationSeconds()))
            .waitDurationInOpenState(Duration.ofSeconds(config.getWaitDurationInOpenStateSeconds()))
            .permittedNumberOfCallsInHalfOpenState(config.getPermittedCallsInHalfOpenState())
            .minimumNumberOfCalls(config.getMinimumNumberOfCalls())
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(config.getSlidingWindowSize())
            .recordExceptions(Exception.class)
            .ignoreExceptions(IllegalArgumentException.class) // Don't count bad requests
            .build();

        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.of(cbConfig);
        this.circuitBreaker = cbRegistry.circuitBreaker(name);

        // Register event listeners
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> log.warn("Circuit breaker '{}' state transition: {} -> {}",
                name, event.getStateTransition().getFromState(), event.getStateTransition().getToState()))
            .onFailureRateExceeded(event -> log.error("Circuit breaker '{}' failure rate exceeded: {}%",
                name, event.getFailureRate()))
            .onSlowCallRateExceeded(event -> log.warn("Circuit breaker '{}' slow call rate exceeded: {}%",
                name, event.getSlowCallRate()));

        // Retry
        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(config.getMaxRetryAttempts())
            .waitDuration(Duration.ofMillis(config.getRetryWaitDurationMs()))
            .retryOnException(e -> isRetryable(e))
            .build();

        RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
        this.retry = retryRegistry.retry(name);

        retry.getEventPublisher()
            .onRetry(event -> log.warn("Retry '{}' attempt {} after failure: {}",
                name, event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()));

        // Rate Limiter
        RateLimiterConfig rlConfig = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(config.getRateLimitRefreshPeriodSeconds()))
            .limitForPeriod(config.getRateLimitForPeriod())
            .timeoutDuration(Duration.ofSeconds(config.getRateLimitTimeoutSeconds()))
            .build();

        RateLimiterRegistry rlRegistry = RateLimiterRegistry.of(rlConfig);
        this.rateLimiter = rlRegistry.rateLimiter(name);

        // Time Limiter
        TimeLimiterConfig tlConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(config.getTimeoutSeconds()))
            .cancelRunningFuture(true)
            .build();

        this.timeLimiter = TimeLimiter.of(tlConfig);

        log.info("Created resilient LLM client wrapper for: {} (CB threshold: {}%, retries: {}, timeout: {}s)",
            name, config.getFailureRateThreshold(), config.getMaxRetryAttempts(), config.getTimeoutSeconds());
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        Supplier<LlmResponse> supplier = CircuitBreaker.decorateSupplier(circuitBreaker,
            Retry.decorateSupplier(retry,
                RateLimiter.decorateSupplier(rateLimiter,
                    () -> delegate.generate(request))));

        try {
            return supplier.get();
        } catch (Exception e) {
            throw mapException(e);
        }
    }

    @Override
    public CompletableFuture<LlmResponse> generateAsync(LlmRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            Supplier<LlmResponse> supplier = CircuitBreaker.decorateSupplier(circuitBreaker,
                Retry.decorateSupplier(retry,
                    RateLimiter.decorateSupplier(rateLimiter,
                        () -> delegate.generate(request))));
            try {
                return supplier.get();
            } catch (Exception e) {
                throw mapException(e);
            }
        });
    }

    @Override
    public LlmResponse generateStreaming(LlmRequest request, StreamingHandler handler) {
        // Streaming has its own handling, apply circuit breaker but skip retry
        Supplier<LlmResponse> supplier = CircuitBreaker.decorateSupplier(circuitBreaker,
            RateLimiter.decorateSupplier(rateLimiter,
                () -> delegate.generateStreaming(request, handler)));

        try {
            return supplier.get();
        } catch (Exception e) {
            throw mapException(e);
        }
    }

    @Override
    public String getProviderName() {
        return delegate.getProviderName();
    }

    @Override
    public String getModelName() {
        return delegate.getModelName();
    }

    @Override
    public boolean isAvailable() {
        // Check circuit breaker state
        CircuitBreaker.State state = circuitBreaker.getState();
        if (state == CircuitBreaker.State.OPEN) {
            log.debug("LLM client {} unavailable: circuit breaker OPEN", getProviderName());
            return false;
        }
        return delegate.isAvailable();
    }

    @Override
    public List<LlmClient.LlmCapability> getCapabilities() {
        return delegate.getCapabilities();
    }

    /**
     * Get current circuit breaker state.
     */
    public CircuitBreaker.State getCircuitBreakerState() {
        return circuitBreaker.getState();
    }

    /**
     * Get circuit breaker metrics.
     */
    public CircuitBreakerMetrics getMetrics() {
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        return new CircuitBreakerMetrics(
            metrics.getFailureRate(),
            metrics.getSlowCallRate(),
            metrics.getNumberOfSuccessfulCalls(),
            metrics.getNumberOfFailedCalls(),
            metrics.getNumberOfSlowCalls(),
            (int) metrics.getNumberOfNotPermittedCalls(),
            circuitBreaker.getState().name()
        );
    }

    /**
     * Reset circuit breaker (use with caution).
     */
    public void resetCircuitBreaker() {
        log.info("Resetting circuit breaker for: {}", getProviderName());
        circuitBreaker.reset();
    }

    private boolean isRetryable(Throwable e) {
        // Retry on transient errors, not on client errors
        if (e instanceof IllegalArgumentException) {
            return false;
        }
        if (e instanceof TimeoutException) {
            return true;
        }
        if (e.getMessage() != null) {
            String msg = e.getMessage().toLowerCase();
            // Retry on rate limits, server errors, connection errors
            return msg.contains("rate limit") ||
                   msg.contains("429") ||
                   msg.contains("500") ||
                   msg.contains("502") ||
                   msg.contains("503") ||
                   msg.contains("504") ||
                   msg.contains("timeout") ||
                   msg.contains("connection");
        }
        return true; // Retry by default
    }

    private RuntimeException mapException(Exception e) {
        if (e instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
            return new LlmCircuitBreakerOpenException(
                "Circuit breaker is open for " + getProviderName(), e);
        }
        if (e instanceof io.github.resilience4j.ratelimiter.RequestNotPermitted) {
            return new LlmRateLimitException(
                "Rate limit exceeded for " + getProviderName(), e);
        }
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException(e);
    }

    /**
     * Circuit breaker metrics snapshot.
     */
    public record CircuitBreakerMetrics(
        float failureRate,
        float slowCallRate,
        int successfulCalls,
        int failedCalls,
        int slowCalls,
        int notPermittedCalls,
        String state
    ) {}

    /**
     * Exception when circuit breaker is open.
     */
    public static class LlmCircuitBreakerOpenException extends RuntimeException {
        public LlmCircuitBreakerOpenException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception when rate limit is exceeded.
     */
    public static class LlmRateLimitException extends RuntimeException {
        public LlmRateLimitException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
