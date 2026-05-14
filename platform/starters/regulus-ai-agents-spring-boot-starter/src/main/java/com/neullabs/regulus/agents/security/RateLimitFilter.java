package com.neullabs.regulus.agents.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Servlet filter for rate limiting requests.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiter rateLimiter;
    private final boolean byClientIp;
    private final boolean byClient;
    private final String apiKeyHeader;

    public RateLimitFilter(RateLimiter rateLimiter, boolean byClientIp, boolean byClient, String apiKeyHeader) {
        this.rateLimiter = rateLimiter;
        this.byClientIp = byClientIp;
        this.byClient = byClient;
        this.apiKeyHeader = apiKeyHeader;
        log.info("Rate limit filter initialized (byIp={}, byClient={})", byClientIp, byClient);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientId = determineClientId(request);

        if (!rateLimiter.allowRequest(clientId)) {
            log.warn("Rate limit exceeded: {} {} from {}",
                request.getMethod(), request.getRequestURI(), clientId);

            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");

            int remaining = rateLimiter.getRemainingTokens(clientId);
            Instant resetTime = rateLimiter.getResetTime(clientId);

            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime.getEpochSecond()));
            response.setHeader("Retry-After", String.valueOf(
                Math.max(1, resetTime.getEpochSecond() - Instant.now().getEpochSecond())));

            response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"code\":\"RATE_LIMITED\"}");
            return;
        }

        // Add rate limit headers to successful responses
        int remaining = rateLimiter.getRemainingTokens(clientId);
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        filterChain.doFilter(request, response);
    }

    private String determineClientId(HttpServletRequest request) {
        // Priority: API key > OAuth client > IP address
        if (byClient) {
            String apiKey = request.getHeader(apiKeyHeader);
            if (apiKey != null && !apiKey.isBlank()) {
                return "key:" + apiKey;
            }

            // Check for OAuth client ID from principal
            if (request.getUserPrincipal() != null) {
                return "client:" + request.getUserPrincipal().getName();
            }
        }

        if (byClientIp) {
            return "ip:" + getClientIp(request);
        }

        return "global";
    }

    private String getClientIp(HttpServletRequest request) {
        // Check for forwarded headers (reverse proxy scenarios)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first IP in the chain (original client)
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }
}
