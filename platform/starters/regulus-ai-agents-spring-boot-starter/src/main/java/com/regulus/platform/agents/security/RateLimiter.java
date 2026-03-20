package com.regulus.platform.agents.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple token bucket rate limiter.
 * Limits requests per client based on configurable rates.
 */
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private final int requestsPerSecond;
    private final int burstCapacity;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final Duration cleanupInterval = Duration.ofMinutes(5);
    private volatile Instant lastCleanup = Instant.now();

    public RateLimiter(int requestsPerSecond, int burstCapacity) {
        this.requestsPerSecond = requestsPerSecond;
        this.burstCapacity = burstCapacity;
        log.info("Rate limiter initialized (rate={}/s, burst={})", requestsPerSecond, burstCapacity);
    }

    /**
     * Check if a request should be allowed.
     *
     * @param clientId the client identifier (IP, API key, etc.)
     * @return true if the request is allowed
     */
    public boolean allowRequest(String clientId) {
        cleanupIfNeeded();

        TokenBucket bucket = buckets.computeIfAbsent(clientId,
            k -> new TokenBucket(requestsPerSecond, burstCapacity));

        boolean allowed = bucket.tryConsume();

        if (!allowed) {
            log.warn("Rate limit exceeded for client: {}", maskClientId(clientId));
        }

        return allowed;
    }

    /**
     * Get remaining tokens for a client.
     */
    public int getRemainingTokens(String clientId) {
        TokenBucket bucket = buckets.get(clientId);
        return bucket != null ? bucket.getAvailableTokens() : burstCapacity;
    }

    /**
     * Get the reset time for a client's rate limit.
     */
    public Instant getResetTime(String clientId) {
        TokenBucket bucket = buckets.get(clientId);
        return bucket != null ? bucket.getResetTime() : Instant.now();
    }

    private void cleanupIfNeeded() {
        Instant now = Instant.now();
        if (Duration.between(lastCleanup, now).compareTo(cleanupInterval) > 0) {
            lastCleanup = now;
            // Remove stale buckets (not accessed in the last cleanup interval)
            buckets.entrySet().removeIf(entry ->
                Duration.between(entry.getValue().getLastAccess(), now).compareTo(cleanupInterval) > 0);
            log.debug("Cleaned up rate limiter buckets, remaining: {}", buckets.size());
        }
    }

    private String maskClientId(String clientId) {
        if (clientId == null || clientId.length() < 8) return "****";
        return clientId.substring(0, 4) + "****";
    }

    /**
     * Token bucket implementation.
     */
    private static class TokenBucket {
        private final int refillRate; // tokens per second
        private final int capacity;
        private final AtomicInteger tokens;
        private final AtomicLong lastRefillTime;
        private volatile Instant lastAccess = Instant.now();

        TokenBucket(int refillRate, int capacity) {
            this.refillRate = refillRate;
            this.capacity = capacity;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
        }

        boolean tryConsume() {
            lastAccess = Instant.now();
            refill();

            int current = tokens.get();
            while (current > 0) {
                if (tokens.compareAndSet(current, current - 1)) {
                    return true;
                }
                current = tokens.get();
            }
            return false;
        }

        int getAvailableTokens() {
            refill();
            return tokens.get();
        }

        Instant getLastAccess() {
            return lastAccess;
        }

        Instant getResetTime() {
            // Time until bucket is full again
            int missing = capacity - tokens.get();
            if (missing <= 0) return Instant.now();
            long msUntilFull = (missing * 1000L) / refillRate;
            return Instant.now().plusMillis(msUntilFull);
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long lastRefill = lastRefillTime.get();
            long elapsed = now - lastRefill;

            if (elapsed > 0) {
                int tokensToAdd = (int) (elapsed * refillRate / 1000);
                if (tokensToAdd > 0) {
                    if (lastRefillTime.compareAndSet(lastRefill, now)) {
                        int current = tokens.get();
                        int newValue = Math.min(capacity, current + tokensToAdd);
                        tokens.compareAndSet(current, newValue);
                    }
                }
            }
        }
    }
}
