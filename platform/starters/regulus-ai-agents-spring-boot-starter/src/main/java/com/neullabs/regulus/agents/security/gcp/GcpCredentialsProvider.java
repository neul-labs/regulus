package com.neullabs.regulus.agents.security.gcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * GCP credentials provider supporting Application Default Credentials (ADC)
 * and explicit service account configuration.
 */
public class GcpCredentialsProvider {

    private static final Logger log = LoggerFactory.getLogger(GcpCredentialsProvider.class);

    private static final String METADATA_SERVER_URL = "http://metadata.google.internal/computeMetadata/v1/";
    private static final String METADATA_FLAVOR_HEADER = "Metadata-Flavor";
    private static final String METADATA_FLAVOR_VALUE = "Google";

    private static final Duration TOKEN_REFRESH_MARGIN = Duration.ofMinutes(5);

    private final GcpCredentialsConfig config;
    private final HttpClient httpClient;
    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

    public GcpCredentialsProvider(GcpCredentialsConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        log.info("GCP credentials provider initialized with mode: {}", config.getMode());
    }

    /**
     * Get an access token for GCP services.
     *
     * @return the access token
     * @throws GcpCredentialsException if credentials cannot be obtained
     */
    public String getAccessToken() throws GcpCredentialsException {
        CachedToken token = cachedToken.get();
        if (token != null && !token.isExpired()) {
            return token.accessToken;
        }

        synchronized (this) {
            token = cachedToken.get();
            if (token != null && !token.isExpired()) {
                return token.accessToken;
            }

            token = fetchNewToken();
            cachedToken.set(token);
            return token.accessToken;
        }
    }

    /**
     * Get an ID token for authenticating to other GCP services.
     *
     * @param audience the target audience (service URL)
     * @return the ID token
     * @throws GcpCredentialsException if credentials cannot be obtained
     */
    public String getIdToken(String audience) throws GcpCredentialsException {
        return switch (config.getMode()) {
            case METADATA_SERVER -> fetchIdTokenFromMetadata(audience);
            case SERVICE_ACCOUNT_FILE -> fetchIdTokenFromServiceAccount(audience);
            case COMPUTE_ENGINE -> fetchIdTokenFromMetadata(audience);
            default -> throw new GcpCredentialsException("ID token not supported in mode: " + config.getMode());
        };
    }

    /**
     * Check if running on GCP (Compute Engine, Cloud Run, GKE, etc.).
     */
    public boolean isRunningOnGcp() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(METADATA_SERVER_URL))
                .header(METADATA_FLAVOR_HEADER, METADATA_FLAVOR_VALUE)
                .timeout(Duration.ofSeconds(1))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.debug("Not running on GCP: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get the current project ID.
     */
    public Optional<String> getProjectId() {
        if (config.getProjectId() != null) {
            return Optional.of(config.getProjectId());
        }

        // Try to get from metadata server
        if (isRunningOnGcp()) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(METADATA_SERVER_URL + "project/project-id"))
                    .header(METADATA_FLAVOR_HEADER, METADATA_FLAVOR_VALUE)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return Optional.of(response.body().trim());
                }
            } catch (Exception e) {
                log.warn("Failed to get project ID from metadata: {}", e.getMessage());
            }
        }

        // Try environment variable
        String envProjectId = System.getenv("GOOGLE_CLOUD_PROJECT");
        if (envProjectId != null) {
            return Optional.of(envProjectId);
        }
        envProjectId = System.getenv("GCLOUD_PROJECT");
        if (envProjectId != null) {
            return Optional.of(envProjectId);
        }

        return Optional.empty();
    }

    /**
     * Get the service account email being used.
     */
    public Optional<String> getServiceAccountEmail() {
        if (config.getServiceAccountEmail() != null) {
            return Optional.of(config.getServiceAccountEmail());
        }

        if (isRunningOnGcp()) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(METADATA_SERVER_URL + "instance/service-accounts/default/email"))
                    .header(METADATA_FLAVOR_HEADER, METADATA_FLAVOR_VALUE)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return Optional.of(response.body().trim());
                }
            } catch (Exception e) {
                log.warn("Failed to get service account email from metadata: {}", e.getMessage());
            }
        }

        return Optional.empty();
    }

    private CachedToken fetchNewToken() throws GcpCredentialsException {
        return switch (config.getMode()) {
            case METADATA_SERVER, COMPUTE_ENGINE -> fetchTokenFromMetadata();
            case SERVICE_ACCOUNT_FILE -> fetchTokenFromServiceAccount();
            case APPLICATION_DEFAULT -> fetchTokenFromADC();
        };
    }

    private CachedToken fetchTokenFromMetadata() throws GcpCredentialsException {
        try {
            String url = METADATA_SERVER_URL + "instance/service-accounts/default/token";
            if (config.getScopes() != null && !config.getScopes().isEmpty()) {
                url += "?scopes=" + String.join(",", config.getScopes());
            }

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header(METADATA_FLAVOR_HEADER, METADATA_FLAVOR_VALUE)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new GcpCredentialsException("Metadata server returned: " + response.statusCode());
            }

            return parseTokenResponse(response.body());

        } catch (IOException | InterruptedException e) {
            throw new GcpCredentialsException("Failed to get token from metadata server", e);
        }
    }

    private CachedToken fetchTokenFromServiceAccount() throws GcpCredentialsException {
        if (config.getServiceAccountKeyPath() == null) {
            throw new GcpCredentialsException("Service account key path not configured");
        }

        try (InputStream keyStream = new FileInputStream(config.getServiceAccountKeyPath())) {
            // For a full implementation, you would use Google's auth library
            // This is a simplified placeholder showing the pattern
            throw new GcpCredentialsException(
                "Service account file authentication requires google-auth-library. " +
                "Add dependency: com.google.auth:google-auth-library-oauth2-http"
            );
        } catch (IOException e) {
            throw new GcpCredentialsException("Failed to read service account key file", e);
        }
    }

    private CachedToken fetchTokenFromADC() throws GcpCredentialsException {
        // Check for credentials file from environment
        String credentialsFile = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (credentialsFile != null) {
            GcpCredentialsConfig adcConfig = new GcpCredentialsConfig();
            adcConfig.setMode(GcpCredentialsMode.SERVICE_ACCOUNT_FILE);
            adcConfig.setServiceAccountKeyPath(credentialsFile);
            adcConfig.setScopes(config.getScopes());
            GcpCredentialsProvider adcProvider = new GcpCredentialsProvider(adcConfig);
            return adcProvider.fetchTokenFromServiceAccount();
        }

        // Try metadata server (running on GCP)
        if (isRunningOnGcp()) {
            return fetchTokenFromMetadata();
        }

        throw new GcpCredentialsException(
            "No credentials found. Set GOOGLE_APPLICATION_CREDENTIALS or run on GCP"
        );
    }

    private String fetchIdTokenFromMetadata(String audience) throws GcpCredentialsException {
        try {
            String url = METADATA_SERVER_URL + "instance/service-accounts/default/identity?audience=" + audience;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header(METADATA_FLAVOR_HEADER, METADATA_FLAVOR_VALUE)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new GcpCredentialsException("Failed to get ID token: " + response.statusCode());
            }

            return response.body().trim();

        } catch (IOException | InterruptedException e) {
            throw new GcpCredentialsException("Failed to get ID token from metadata", e);
        }
    }

    private String fetchIdTokenFromServiceAccount(String audience) throws GcpCredentialsException {
        // For a full implementation, you would create a signed JWT
        throw new GcpCredentialsException(
            "ID token from service account file requires google-auth-library"
        );
    }

    private CachedToken parseTokenResponse(String json) throws GcpCredentialsException {
        // Simple JSON parsing (in production, use Jackson)
        try {
            String accessToken = extractJsonValue(json, "access_token");
            String expiresIn = extractJsonValue(json, "expires_in");

            int expiresInSeconds = Integer.parseInt(expiresIn);
            Instant expiresAt = Instant.now().plusSeconds(expiresInSeconds);

            return new CachedToken(accessToken, expiresAt);

        } catch (Exception e) {
            throw new GcpCredentialsException("Failed to parse token response", e);
        }
    }

    private String extractJsonValue(String json, String key) {
        // Simple extraction - in production use proper JSON library
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

    private static class CachedToken {
        final String accessToken;
        final Instant expiresAt;

        CachedToken(String accessToken, Instant expiresAt) {
            this.accessToken = accessToken;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return Instant.now().plus(TOKEN_REFRESH_MARGIN).isAfter(expiresAt);
        }
    }

    /**
     * GCP credentials configuration.
     */
    public static class GcpCredentialsConfig {
        private GcpCredentialsMode mode = GcpCredentialsMode.APPLICATION_DEFAULT;
        private String projectId;
        private String serviceAccountKeyPath;
        private String serviceAccountEmail;
        private java.util.List<String> scopes;

        public GcpCredentialsMode getMode() {
            return mode;
        }

        public void setMode(GcpCredentialsMode mode) {
            this.mode = mode;
        }

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getServiceAccountKeyPath() {
            return serviceAccountKeyPath;
        }

        public void setServiceAccountKeyPath(String serviceAccountKeyPath) {
            this.serviceAccountKeyPath = serviceAccountKeyPath;
        }

        public String getServiceAccountEmail() {
            return serviceAccountEmail;
        }

        public void setServiceAccountEmail(String serviceAccountEmail) {
            this.serviceAccountEmail = serviceAccountEmail;
        }

        public java.util.List<String> getScopes() {
            return scopes;
        }

        public void setScopes(java.util.List<String> scopes) {
            this.scopes = scopes;
        }
    }

    /**
     * GCP credentials mode.
     */
    public enum GcpCredentialsMode {
        APPLICATION_DEFAULT,
        METADATA_SERVER,
        SERVICE_ACCOUNT_FILE,
        COMPUTE_ENGINE
    }

    /**
     * Exception for GCP credentials errors.
     */
    public static class GcpCredentialsException extends Exception {
        public GcpCredentialsException(String message) {
            super(message);
        }

        public GcpCredentialsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
