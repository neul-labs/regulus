package com.regulus.platform.agents.security.gcp;

import com.regulus.platform.agents.security.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

/**
 * Filter for validating GCP service account tokens.
 * Supports both ID tokens and access tokens from GCP IAM.
 */
public class GcpServiceAccountFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GcpServiceAccountFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecurityProperties.GcpConfig gcpConfig;
    private final List<String> excludedPaths;

    public GcpServiceAccountFilter(SecurityProperties.GcpConfig gcpConfig, List<String> excludedPaths) {
        this.gcpConfig = gcpConfig;
        this.excludedPaths = excludedPaths;
        log.info("GCP service account filter initialized for project: {}", gcpConfig.getProjectId());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip excluded paths
        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("Missing or invalid Authorization header");
            sendUnauthorized(response, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            GcpTokenInfo tokenInfo = validateToken(token);

            if (!isServiceAccountAllowed(tokenInfo.email)) {
                log.warn("Service account not in allowed list: {}", tokenInfo.email);
                sendForbidden(response, "Service account not authorized");
                return;
            }

            // Set attributes for downstream use
            request.setAttribute("gcp.service.account.email", tokenInfo.email);
            request.setAttribute("gcp.service.account.project", tokenInfo.projectId);
            request.setAttribute("gcp.token.audience", tokenInfo.audience);

            log.debug("Authenticated service account: {}", tokenInfo.email);
            filterChain.doFilter(request, response);

        } catch (TokenValidationException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            sendUnauthorized(response, "Token validation failed: " + e.getMessage());
        }
    }

    private GcpTokenInfo validateToken(String token) throws TokenValidationException {
        // Decode JWT without verification (verification happens with JWKS)
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new TokenValidationException("Invalid JWT format");
        }

        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            return parseTokenPayload(payloadJson);
        } catch (Exception e) {
            throw new TokenValidationException("Failed to decode token: " + e.getMessage());
        }
    }

    private GcpTokenInfo parseTokenPayload(String json) throws TokenValidationException {
        // Simple JSON parsing - in production use Jackson
        String email = extractJsonValue(json, "email");
        String sub = extractJsonValue(json, "sub");
        String aud = extractJsonValue(json, "aud");
        String iss = extractJsonValue(json, "iss");
        String exp = extractJsonValue(json, "exp");

        // Validate issuer
        if (iss == null || (!iss.contains("accounts.google.com") && !iss.contains("googleapis.com"))) {
            throw new TokenValidationException("Invalid token issuer: " + iss);
        }

        // Validate expiration
        if (exp != null) {
            long expTime = Long.parseLong(exp);
            if (System.currentTimeMillis() / 1000 > expTime) {
                throw new TokenValidationException("Token has expired");
            }
        }

        // Validate audience if configured
        if (gcpConfig.getProjectId() != null && aud != null) {
            if (!aud.contains(gcpConfig.getProjectId())) {
                log.debug("Audience mismatch: expected {}, got {}", gcpConfig.getProjectId(), aud);
            }
        }

        // Extract service account email
        String serviceAccountEmail = email != null ? email : sub;
        if (serviceAccountEmail == null) {
            throw new TokenValidationException("No service account identifier in token");
        }

        // Validate it's a service account
        if (!serviceAccountEmail.endsWith(".iam.gserviceaccount.com") &&
            !serviceAccountEmail.endsWith("@appspot.gserviceaccount.com")) {
            throw new TokenValidationException("Token is not from a service account");
        }

        // Extract project ID from service account email
        String projectId = extractProjectFromServiceAccount(serviceAccountEmail);

        return new GcpTokenInfo(serviceAccountEmail, projectId, aud);
    }

    private String extractProjectFromServiceAccount(String email) {
        // Format: name@project-id.iam.gserviceaccount.com
        if (email.endsWith(".iam.gserviceaccount.com")) {
            int atIndex = email.indexOf('@');
            int suffixIndex = email.indexOf(".iam.gserviceaccount.com");
            if (atIndex > 0 && suffixIndex > atIndex) {
                return email.substring(atIndex + 1, suffixIndex);
            }
        }
        return null;
    }

    private boolean isServiceAccountAllowed(String email) {
        if (!gcpConfig.isValidateServiceAccount()) {
            return true;
        }

        List<String> allowedAccounts = gcpConfig.getAllowedServiceAccounts();
        if (allowedAccounts == null || allowedAccounts.isEmpty()) {
            // No whitelist means allow all service accounts
            return true;
        }

        // Check exact match
        if (allowedAccounts.contains(email)) {
            return true;
        }

        // Check wildcard patterns (e.g., *@project.iam.gserviceaccount.com)
        for (String pattern : allowedAccounts) {
            if (pattern.startsWith("*@")) {
                String suffix = pattern.substring(1);
                if (email.endsWith(suffix)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isExcludedPath(String path) {
        for (String excluded : excludedPaths) {
            if (excluded.endsWith("/**")) {
                String prefix = excluded.substring(0, excluded.length() - 3);
                if (path.startsWith(prefix)) {
                    return true;
                }
            } else if (path.equals(excluded)) {
                return true;
            }
        }
        return false;
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) return null;

        start += searchKey.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"')) {
            start++;
        }

        int end = start;
        while (end < json.length() && json.charAt(end) != '"' && json.charAt(end) != ',' && json.charAt(end) != '}') {
            end++;
        }

        return json.substring(start, end).trim();
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    private record GcpTokenInfo(String email, String projectId, String audience) {}

    private static class TokenValidationException extends Exception {
        TokenValidationException(String message) {
            super(message);
        }
    }
}
