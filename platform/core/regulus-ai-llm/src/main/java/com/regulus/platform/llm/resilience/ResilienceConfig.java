package com.regulus.platform.llm.resilience;

/**
 * Configuration for resilience features (circuit breaker, retry, rate limiting).
 * Uses builder pattern for flexible configuration.
 */
public class ResilienceConfig {

    // Circuit Breaker defaults
    private float failureRateThreshold = 50.0f;
    private float slowCallRateThreshold = 80.0f;
    private long slowCallDurationSeconds = 30;
    private long waitDurationInOpenStateSeconds = 60;
    private int permittedCallsInHalfOpenState = 3;
    private int minimumNumberOfCalls = 10;
    private int slidingWindowSize = 20;

    // Retry defaults
    private int maxRetryAttempts = 3;
    private long retryWaitDurationMs = 1000;
    private double retryExponentialMultiplier = 2.0;

    // Rate Limiter defaults
    private int rateLimitRefreshPeriodSeconds = 1;
    private int rateLimitForPeriod = 100;
    private int rateLimitTimeoutSeconds = 5;

    // Time Limiter defaults
    private long timeoutSeconds = 120;

    public static Builder builder() {
        return new Builder();
    }

    public static ResilienceConfig defaults() {
        return new ResilienceConfig();
    }

    /**
     * Preset for high-throughput, latency-tolerant workloads.
     */
    public static ResilienceConfig highThroughput() {
        return builder()
            .failureRateThreshold(60.0f)
            .slowCallRateThreshold(90.0f)
            .slowCallDurationSeconds(60)
            .rateLimitForPeriod(500)
            .timeoutSeconds(180)
            .build();
    }

    /**
     * Preset for low-latency, failure-sensitive workloads.
     */
    public static ResilienceConfig lowLatency() {
        return builder()
            .failureRateThreshold(30.0f)
            .slowCallRateThreshold(50.0f)
            .slowCallDurationSeconds(10)
            .waitDurationInOpenStateSeconds(30)
            .maxRetryAttempts(2)
            .timeoutSeconds(30)
            .build();
    }

    /**
     * Preset for critical financial operations.
     */
    public static ResilienceConfig financial() {
        return builder()
            .failureRateThreshold(40.0f)
            .slowCallRateThreshold(60.0f)
            .slowCallDurationSeconds(20)
            .waitDurationInOpenStateSeconds(120)
            .permittedCallsInHalfOpenState(5)
            .minimumNumberOfCalls(20)
            .maxRetryAttempts(3)
            .retryWaitDurationMs(2000)
            .rateLimitForPeriod(50) // More conservative
            .timeoutSeconds(60)
            .build();
    }

    // Getters
    public float getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public float getSlowCallRateThreshold() {
        return slowCallRateThreshold;
    }

    public long getSlowCallDurationSeconds() {
        return slowCallDurationSeconds;
    }

    public long getWaitDurationInOpenStateSeconds() {
        return waitDurationInOpenStateSeconds;
    }

    public int getPermittedCallsInHalfOpenState() {
        return permittedCallsInHalfOpenState;
    }

    public int getMinimumNumberOfCalls() {
        return minimumNumberOfCalls;
    }

    public int getSlidingWindowSize() {
        return slidingWindowSize;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public long getRetryWaitDurationMs() {
        return retryWaitDurationMs;
    }

    public double getRetryExponentialMultiplier() {
        return retryExponentialMultiplier;
    }

    public int getRateLimitRefreshPeriodSeconds() {
        return rateLimitRefreshPeriodSeconds;
    }

    public int getRateLimitForPeriod() {
        return rateLimitForPeriod;
    }

    public int getRateLimitTimeoutSeconds() {
        return rateLimitTimeoutSeconds;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public static class Builder {
        private final ResilienceConfig config = new ResilienceConfig();

        public Builder failureRateThreshold(float threshold) {
            config.failureRateThreshold = threshold;
            return this;
        }

        public Builder slowCallRateThreshold(float threshold) {
            config.slowCallRateThreshold = threshold;
            return this;
        }

        public Builder slowCallDurationSeconds(long seconds) {
            config.slowCallDurationSeconds = seconds;
            return this;
        }

        public Builder waitDurationInOpenStateSeconds(long seconds) {
            config.waitDurationInOpenStateSeconds = seconds;
            return this;
        }

        public Builder permittedCallsInHalfOpenState(int calls) {
            config.permittedCallsInHalfOpenState = calls;
            return this;
        }

        public Builder minimumNumberOfCalls(int calls) {
            config.minimumNumberOfCalls = calls;
            return this;
        }

        public Builder slidingWindowSize(int size) {
            config.slidingWindowSize = size;
            return this;
        }

        public Builder maxRetryAttempts(int attempts) {
            config.maxRetryAttempts = attempts;
            return this;
        }

        public Builder retryWaitDurationMs(long ms) {
            config.retryWaitDurationMs = ms;
            return this;
        }

        public Builder retryExponentialMultiplier(double multiplier) {
            config.retryExponentialMultiplier = multiplier;
            return this;
        }

        public Builder rateLimitRefreshPeriodSeconds(int seconds) {
            config.rateLimitRefreshPeriodSeconds = seconds;
            return this;
        }

        public Builder rateLimitForPeriod(int limit) {
            config.rateLimitForPeriod = limit;
            return this;
        }

        public Builder rateLimitTimeoutSeconds(int seconds) {
            config.rateLimitTimeoutSeconds = seconds;
            return this;
        }

        public Builder timeoutSeconds(long seconds) {
            config.timeoutSeconds = seconds;
            return this;
        }

        public ResilienceConfig build() {
            return config;
        }
    }
}
