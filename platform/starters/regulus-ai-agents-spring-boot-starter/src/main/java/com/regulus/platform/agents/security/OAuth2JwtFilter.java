package com.regulus.platform.agents.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * JWT OAuth2 authentication filter.
 * Validates JWT tokens from Authorization header and extracts claims.
 * Supports GCP IAM service account tokens.
 */
public class OAuth2JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OAuth2JwtFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenValidator tokenValidator;
    private final Set<String> excludedPaths;

    public OAuth2JwtFilter(JwtTokenValidator tokenValidator, Set<String> excludedPaths) {
        this.tokenValidator = tokenValidator;
        this.excludedPaths = excludedPaths;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Skip excluded paths
        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            sendUnauthorizedResponse(response, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            JwtTokenValidator.ValidationResult result = tokenValidator.validate(token);

            if (!result.isValid()) {
                log.warn("JWT validation failed: {}", result.getError());
                sendUnauthorizedResponse(response, result.getError());
                return;
            }

            // Set authenticated principal in request attributes
            request.setAttribute("authenticated.subject", result.getSubject());
            request.setAttribute("authenticated.email", result.getEmail());
            request.setAttribute("authenticated.claims", result.getClaims());

            // Add to MDC for logging
            MDC.put("authSubject", result.getSubject());
            if (result.getEmail() != null) {
                MDC.put("authEmail", result.getEmail());
            }

            log.debug("JWT authentication successful for subject: {}", result.getSubject());
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWT validation error", e);
            sendUnauthorizedResponse(response, "Token validation failed");
        } finally {
            MDC.remove("authSubject");
            MDC.remove("authEmail");
        }
    }

    private boolean isExcludedPath(String path) {
        for (String excluded : excludedPaths) {
            if (excluded.endsWith("/*")) {
                String prefix = excluded.substring(0, excluded.length() - 2);
                if (path.startsWith(prefix)) {
                    return true;
                }
            } else if (path.equals(excluded)) {
                return true;
            }
        }
        return false;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\":\"unauthorized\",\"message\":\"%s\"}", message
        ));
    }
}
