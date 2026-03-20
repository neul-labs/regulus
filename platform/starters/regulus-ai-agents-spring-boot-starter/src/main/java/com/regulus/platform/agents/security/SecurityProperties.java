package com.regulus.platform.agents.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for security hardening.
 * Supports OAuth2, mTLS, and API key authentication.
 */
@ConfigurationProperties(prefix = "regulus.ai.security")
public class SecurityProperties {

    /**
     * Whether security is enabled.
     */
    private boolean enabled = true;

    /**
     * OAuth2 configuration.
     */
    private OAuth2Config oauth2 = new OAuth2Config();

    /**
     * mTLS configuration.
     */
    private MtlsConfig mtls = new MtlsConfig();

    /**
     * API key configuration.
     */
    private ApiKeyConfig apiKey = new ApiKeyConfig();

    /**
     * Rate limiting configuration.
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public OAuth2Config getOauth2() {
        return oauth2;
    }

    public void setOauth2(OAuth2Config oauth2) {
        this.oauth2 = oauth2;
    }

    public MtlsConfig getMtls() {
        return mtls;
    }

    public void setMtls(MtlsConfig mtls) {
        this.mtls = mtls;
    }

    public ApiKeyConfig getApiKey() {
        return apiKey;
    }

    public void setApiKey(ApiKeyConfig apiKey) {
        this.apiKey = apiKey;
    }

    public RateLimitConfig getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitConfig rateLimit) {
        this.rateLimit = rateLimit;
    }

    public static class OAuth2Config {
        /**
         * Whether OAuth2 is enabled.
         */
        private boolean enabled = false;

        /**
         * OAuth2 provider type (google, azure, okta, generic).
         */
        private String provider = "google";

        /**
         * OAuth2 issuer URI.
         * For GCP: https://accounts.google.com
         */
        private String issuerUri = "https://accounts.google.com";

        /**
         * OAuth2 audience (your GCP project or service account).
         */
        private String audience;

        /**
         * Required scopes for access.
         */
        private List<String> requiredScopes = List.of();

        /**
         * JWKS URI for token validation.
         * For GCP: https://www.googleapis.com/oauth2/v3/certs
         */
        private String jwksUri = "https://www.googleapis.com/oauth2/v3/certs";

        /**
         * GCP-specific configuration.
         */
        private GcpConfig gcp = new GcpConfig();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getIssuerUri() {
            return issuerUri;
        }

        public void setIssuerUri(String issuerUri) {
            this.issuerUri = issuerUri;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public List<String> getRequiredScopes() {
            return requiredScopes;
        }

        public void setRequiredScopes(List<String> requiredScopes) {
            this.requiredScopes = requiredScopes;
        }

        public String getJwksUri() {
            return jwksUri;
        }

        public void setJwksUri(String jwksUri) {
            this.jwksUri = jwksUri;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public GcpConfig getGcp() {
            return gcp;
        }

        public void setGcp(GcpConfig gcp) {
            this.gcp = gcp;
        }
    }

    /**
     * GCP IAM specific configuration.
     */
    public static class GcpConfig {
        /**
         * GCP project ID.
         */
        private String projectId;

        /**
         * Allowed service accounts (email format).
         */
        private List<String> allowedServiceAccounts = List.of();

        /**
         * Whether to validate service account claims.
         */
        private boolean validateServiceAccount = true;

        /**
         * Required IAM roles (optional).
         */
        private List<String> requiredRoles = List.of();

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public List<String> getAllowedServiceAccounts() {
            return allowedServiceAccounts;
        }

        public void setAllowedServiceAccounts(List<String> allowedServiceAccounts) {
            this.allowedServiceAccounts = allowedServiceAccounts;
        }

        public boolean isValidateServiceAccount() {
            return validateServiceAccount;
        }

        public void setValidateServiceAccount(boolean validateServiceAccount) {
            this.validateServiceAccount = validateServiceAccount;
        }

        public List<String> getRequiredRoles() {
            return requiredRoles;
        }

        public void setRequiredRoles(List<String> requiredRoles) {
            this.requiredRoles = requiredRoles;
        }
    }

    public static class MtlsConfig {
        /**
         * Whether mTLS is enabled.
         */
        private boolean enabled = false;

        /**
         * Path to trust store.
         */
        private String trustStorePath;

        /**
         * Trust store password.
         */
        private String trustStorePassword;

        /**
         * Trust store type (JKS, PKCS12).
         */
        private String trustStoreType = "PKCS12";

        /**
         * Path to key store.
         */
        private String keyStorePath;

        /**
         * Key store password.
         */
        private String keyStorePassword;

        /**
         * Key store type (JKS, PKCS12).
         */
        private String keyStoreType = "PKCS12";

        /**
         * Whether to require client certificate.
         */
        private boolean requireClientCert = false;

        /**
         * Allowed client certificate CNs.
         */
        private List<String> allowedClientCns = List.of();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTrustStorePath() {
            return trustStorePath;
        }

        public void setTrustStorePath(String trustStorePath) {
            this.trustStorePath = trustStorePath;
        }

        public String getTrustStorePassword() {
            return trustStorePassword;
        }

        public void setTrustStorePassword(String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
        }

        public String getTrustStoreType() {
            return trustStoreType;
        }

        public void setTrustStoreType(String trustStoreType) {
            this.trustStoreType = trustStoreType;
        }

        public String getKeyStorePath() {
            return keyStorePath;
        }

        public void setKeyStorePath(String keyStorePath) {
            this.keyStorePath = keyStorePath;
        }

        public String getKeyStorePassword() {
            return keyStorePassword;
        }

        public void setKeyStorePassword(String keyStorePassword) {
            this.keyStorePassword = keyStorePassword;
        }

        public String getKeyStoreType() {
            return keyStoreType;
        }

        public void setKeyStoreType(String keyStoreType) {
            this.keyStoreType = keyStoreType;
        }

        public boolean isRequireClientCert() {
            return requireClientCert;
        }

        public void setRequireClientCert(boolean requireClientCert) {
            this.requireClientCert = requireClientCert;
        }

        public List<String> getAllowedClientCns() {
            return allowedClientCns;
        }

        public void setAllowedClientCns(List<String> allowedClientCns) {
            this.allowedClientCns = allowedClientCns;
        }
    }

    public static class ApiKeyConfig {
        /**
         * Whether API key authentication is enabled.
         */
        private boolean enabled = false;

        /**
         * Header name for API key.
         */
        private String headerName = "X-API-Key";

        /**
         * Valid API keys (in production, use secret management).
         */
        private List<String> validKeys = List.of();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        public List<String> getValidKeys() {
            return validKeys;
        }

        public void setValidKeys(List<String> validKeys) {
            this.validKeys = validKeys;
        }
    }

    public static class RateLimitConfig {
        /**
         * Whether rate limiting is enabled.
         */
        private boolean enabled = false;

        /**
         * Requests per second limit.
         */
        private int requestsPerSecond = 100;

        /**
         * Burst capacity.
         */
        private int burstCapacity = 200;

        /**
         * Rate limit by client IP.
         */
        private boolean byClientIp = true;

        /**
         * Rate limit by API key or OAuth client.
         */
        private boolean byClient = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRequestsPerSecond() {
            return requestsPerSecond;
        }

        public void setRequestsPerSecond(int requestsPerSecond) {
            this.requestsPerSecond = requestsPerSecond;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }

        public boolean isByClientIp() {
            return byClientIp;
        }

        public void setByClientIp(boolean byClientIp) {
            this.byClientIp = byClientIp;
        }

        public boolean isByClient() {
            return byClient;
        }

        public void setByClient(boolean byClient) {
            this.byClient = byClient;
        }
    }
}
