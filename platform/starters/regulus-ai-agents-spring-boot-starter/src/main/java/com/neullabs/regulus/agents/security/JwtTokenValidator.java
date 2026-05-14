package com.neullabs.regulus.agents.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT token validator with JWKS key fetching.
 * Supports GCP service account tokens and standard OAuth2 tokens.
 */
public class JwtTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);

    private final SecurityProperties.OAuth2Config config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Map<String, RSAPublicKey> keyCache;
    private volatile Instant keysCacheExpiry;

    public JwtTokenValidator(SecurityProperties.OAuth2Config config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.keyCache = new ConcurrentHashMap<>();
        this.keysCacheExpiry = Instant.MIN;
    }

    /**
     * Validate a JWT token.
     */
    public ValidationResult validate(String token) {
        try {
            // Parse JWT
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return ValidationResult.invalid("Invalid JWT format");
            }

            // Decode header and payload
            String headerJson = decodeBase64Url(parts[0]);
            String payloadJson = decodeBase64Url(parts[1]);

            JsonNode header = objectMapper.readTree(headerJson);
            JsonNode payload = objectMapper.readTree(payloadJson);

            // Verify algorithm
            String alg = header.path("alg").asText();
            if (!"RS256".equals(alg)) {
                return ValidationResult.invalid("Unsupported algorithm: " + alg);
            }

            // Check expiration
            long exp = payload.path("exp").asLong();
            if (Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
                return ValidationResult.invalid("Token expired");
            }

            // Check not before
            if (payload.has("nbf")) {
                long nbf = payload.path("nbf").asLong();
                if (Instant.ofEpochSecond(nbf).isAfter(Instant.now())) {
                    return ValidationResult.invalid("Token not yet valid");
                }
            }

            // Verify issuer
            String issuer = payload.path("iss").asText();
            if (!config.getIssuerUri().equals(issuer)) {
                return ValidationResult.invalid("Invalid issuer: " + issuer);
            }

            // Verify audience if configured
            if (config.getAudience() != null && !config.getAudience().isBlank()) {
                JsonNode audNode = payload.path("aud");
                boolean audienceValid = false;
                if (audNode.isArray()) {
                    for (JsonNode aud : audNode) {
                        if (config.getAudience().equals(aud.asText())) {
                            audienceValid = true;
                            break;
                        }
                    }
                } else {
                    audienceValid = config.getAudience().equals(audNode.asText());
                }
                if (!audienceValid) {
                    return ValidationResult.invalid("Invalid audience");
                }
            }

            // Verify signature
            String kid = header.path("kid").asText();
            RSAPublicKey publicKey = getPublicKey(kid);
            if (publicKey == null) {
                return ValidationResult.invalid("Unknown key ID: " + kid);
            }

            if (!verifySignature(parts[0] + "." + parts[1], parts[2], publicKey)) {
                return ValidationResult.invalid("Invalid signature");
            }

            // GCP service account validation
            if ("google".equalsIgnoreCase(config.getProvider()) && config.getGcp().isValidateServiceAccount()) {
                String email = payload.path("email").asText(null);
                if (email != null && !config.getGcp().getAllowedServiceAccounts().isEmpty()) {
                    if (!config.getGcp().getAllowedServiceAccounts().contains(email)) {
                        return ValidationResult.invalid("Service account not allowed: " + email);
                    }
                }
            }

            // Check required scopes
            if (!config.getRequiredScopes().isEmpty()) {
                String scope = payload.path("scope").asText("");
                List<String> tokenScopes = List.of(scope.split("\\s+"));
                for (String required : config.getRequiredScopes()) {
                    if (!tokenScopes.contains(required)) {
                        return ValidationResult.invalid("Missing required scope: " + required);
                    }
                }
            }

            // Extract claims
            String subject = payload.path("sub").asText();
            String email = payload.path("email").asText(null);
            Map<String, Object> claims = extractClaims(payload);

            return ValidationResult.valid(subject, email, claims);

        } catch (Exception e) {
            log.error("JWT validation error", e);
            return ValidationResult.invalid("Validation error: " + e.getMessage());
        }
    }

    private String decodeBase64Url(String input) {
        byte[] decoded = Base64.getUrlDecoder().decode(input);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private RSAPublicKey getPublicKey(String kid) {
        // Check cache
        if (Instant.now().isBefore(keysCacheExpiry) && keyCache.containsKey(kid)) {
            return keyCache.get(kid);
        }

        // Fetch JWKS
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getJwksUri()))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Failed to fetch JWKS: status={}", response.statusCode());
                return null;
            }

            JsonNode jwks = objectMapper.readTree(response.body());
            JsonNode keys = jwks.path("keys");

            keyCache.clear();
            for (JsonNode key : keys) {
                String keyId = key.path("kid").asText();
                String n = key.path("n").asText();
                String e = key.path("e").asText();

                BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
                BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));

                RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                KeyFactory factory = KeyFactory.getInstance("RSA");
                RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(spec);

                keyCache.put(keyId, publicKey);
            }

            // Cache for 1 hour
            keysCacheExpiry = Instant.now().plus(Duration.ofHours(1));

            return keyCache.get(kid);

        } catch (Exception e) {
            log.error("Error fetching JWKS", e);
            return null;
        }
    }

    private boolean verifySignature(String data, String signature, RSAPublicKey publicKey) {
        try {
            java.security.Signature sig = java.security.Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = Base64.getUrlDecoder().decode(signature);
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            log.error("Signature verification error", e);
            return false;
        }
    }

    private Map<String, Object> extractClaims(JsonNode payload) {
        Map<String, Object> claims = new HashMap<>();
        payload.fields().forEachRemaining(field -> {
            String key = field.getKey();
            JsonNode value = field.getValue();
            if (value.isTextual()) {
                claims.put(key, value.asText());
            } else if (value.isNumber()) {
                claims.put(key, value.numberValue());
            } else if (value.isBoolean()) {
                claims.put(key, value.asBoolean());
            }
        });
        return claims;
    }

    /**
     * Result of JWT validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String subject;
        private final String email;
        private final Map<String, Object> claims;
        private final String error;

        private ValidationResult(boolean valid, String subject, String email,
                                 Map<String, Object> claims, String error) {
            this.valid = valid;
            this.subject = subject;
            this.email = email;
            this.claims = claims;
            this.error = error;
        }

        public static ValidationResult valid(String subject, String email, Map<String, Object> claims) {
            return new ValidationResult(true, subject, email, claims, null);
        }

        public static ValidationResult invalid(String error) {
            return new ValidationResult(false, null, null, null, error);
        }

        public boolean isValid() {
            return valid;
        }

        public String getSubject() {
            return subject;
        }

        public String getEmail() {
            return email;
        }

        public Map<String, Object> getClaims() {
            return claims;
        }

        public String getError() {
            return error;
        }
    }
}
