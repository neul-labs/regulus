package com.neullabs.regulus.agents.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Servlet filter for API key authentication.
 * Validates API keys in request headers.
 */
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

    private final String headerName;
    private final Set<String> validKeys;
    private final Set<String> excludedPaths;

    public ApiKeyFilter(String headerName, Set<String> validKeys, Set<String> excludedPaths) {
        this.headerName = headerName;
        this.validKeys = validKeys;
        this.excludedPaths = excludedPaths;
        log.info("API key filter initialized (header={}, keys={}, excluded={})",
            headerName, validKeys.size(), excludedPaths);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip authentication for excluded paths
        if (isExcluded(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(headerName);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Missing API key for request: {} {}", request.getMethod(), path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing API key\",\"code\":\"UNAUTHORIZED\"}");
            return;
        }

        if (!validKeys.contains(apiKey)) {
            log.warn("Invalid API key for request: {} {}", request.getMethod(), path);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid API key\",\"code\":\"FORBIDDEN\"}");
            return;
        }

        log.debug("API key validated for request: {} {}", request.getMethod(), path);
        filterChain.doFilter(request, response);
    }

    private boolean isExcluded(String path) {
        return excludedPaths.stream().anyMatch(excluded -> {
            if (excluded.endsWith("*")) {
                return path.startsWith(excluded.substring(0, excluded.length() - 1));
            }
            return path.equals(excluded);
        });
    }
}
