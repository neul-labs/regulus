package com.neullabs.regulus.identity.oidc;

import com.neullabs.regulus.identity.Claims;
import com.neullabs.regulus.identity.Identity;
import com.neullabs.regulus.identity.Jurisdiction;
import com.neullabs.regulus.identity.Principal;
import com.neullabs.regulus.identity.spi.AuthenticationException;
import com.neullabs.regulus.identity.spi.IdentityAdapter;
import com.neullabs.regulus.identity.spi.RequestContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.neullabs.regulus.identity.oidc.OidcClaimNames.*;

/**
 * Maps Spring Security's {@link JwtAuthenticationToken} to a Regulus
 * {@link Identity}. The {@link RequestContext#raw()} map must carry the
 * {@code JwtAuthenticationToken} under the key {@code "springAuth"} (the
 * {@link OidcSecurityContextFilter} placed there).
 */
public final class OidcIdentityAdapter implements IdentityAdapter {

    public static final String ADAPTER_ID = "oidc";
    public static final String SPRING_AUTH_KEY = "springAuth";

    @Override
    public String adapterId() {
        return ADAPTER_ID;
    }

    @Override
    public Identity authenticate(RequestContext context) throws AuthenticationException {
        Object raw = context.raw().get(SPRING_AUTH_KEY);
        if (!(raw instanceof JwtAuthenticationToken jwtAuth)) {
            throw new AuthenticationException(
                    "OidcIdentityAdapter expects a JwtAuthenticationToken under raw['" + SPRING_AUTH_KEY + "']");
        }
        Jwt jwt = jwtAuth.getToken();

        Principal principal = new Principal(
                requireString(jwt, SUB),
                firstNonBlank(jwt.getClaimAsString(PREFERRED_USERNAME), jwt.getClaimAsString(NAME)),
                Principal.PrincipalType.HUMAN);

        String tenantId = firstNonBlank(jwt.getClaimAsString(TENANT), jwt.getClaimAsString(TENANT_FALLBACK));
        if (tenantId == null) {
            throw new AuthenticationException(
                    "JWT is missing both '" + TENANT + "' and fallback '" + TENANT_FALLBACK + "' claims");
        }

        Jurisdiction jurisdiction = parseJurisdiction(jwt.getClaimAsString(JURISDICTION));
        Set<String> purposeCodes = stringSet(jwt.getClaim(PURPOSE));
        Set<String> lawfulBases = stringSet(jwt.getClaim(LAWFUL_BASIS));
        Set<String> roles = mergedRoles(jwt);

        Map<String, String> extensions = passThroughClaims(jwt);

        Claims claims = new Claims(tenantId, jurisdiction, purposeCodes, roles, lawfulBases, extensions);

        Instant expiresAt = jwt.getExpiresAt();
        return new Identity(
                principal,
                claims,
                new Identity.Provenance(ADAPTER_ID, Instant.now(), expiresAt, jwt.getIssuer() == null ? null : jwt.getIssuer().toString()));
    }

    private static Jurisdiction parseJurisdiction(String raw) throws AuthenticationException {
        if (raw == null || raw.isBlank()) {
            throw new AuthenticationException(
                    "JWT is missing the '" + JURISDICTION + "' claim — every Identity must carry a jurisdiction");
        }
        try {
            return Jurisdiction.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException(
                    "Unsupported '" + JURISDICTION + "' value: " + raw + " (expected EU, UK, or EU_UK)");
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<String> stringSet(Object value) {
        if (value == null) return Set.of();
        if (value instanceof Collection<?> coll) {
            Set<String> out = new HashSet<>();
            for (Object o : coll) {
                if (o != null) out.add(o.toString());
            }
            return Set.copyOf(out);
        }
        if (value instanceof String s) {
            return s.isBlank() ? Set.of() : Set.of(s);
        }
        return Set.of();
    }

    @SuppressWarnings("unchecked")
    private static Set<String> mergedRoles(Jwt jwt) {
        Set<String> roles = new HashSet<>();
        String scope = jwt.getClaimAsString(SCOPE);
        if (scope != null && !scope.isBlank()) {
            for (String s : scope.split("\\s+")) {
                if (!s.isBlank()) roles.add(s);
            }
        }
        Object directRoles = jwt.getClaim(ROLES);
        roles.addAll(stringSet(directRoles));
        Object realmAccess = jwt.getClaim(REALM_ACCESS);
        if (realmAccess instanceof Map<?, ?> map) {
            Object realmRoles = map.get(ROLES);
            roles.addAll(stringSet(realmRoles));
        }
        return Set.copyOf(roles);
    }

    private static Map<String, String> passThroughClaims(Jwt jwt) {
        // Forward all non-mapped claims as opaque strings so consumers
        // (e.g. PolicyContextBridge) can read tenant-specific custom claims.
        Map<String, String> out = new HashMap<>();
        List<String> mapped = new ArrayList<>(List.of(
                SUB, NAME, PREFERRED_USERNAME, TENANT, TENANT_FALLBACK,
                JURISDICTION, PURPOSE, LAWFUL_BASIS, SCOPE, ROLES, REALM_ACCESS,
                "iss", "exp", "iat", "nbf", "aud", "jti"));
        for (Map.Entry<String, Object> e : jwt.getClaims().entrySet()) {
            if (mapped.contains(e.getKey())) continue;
            Object v = e.getValue();
            if (v != null) {
                out.put(e.getKey(), v.toString());
            }
        }
        return Map.copyOf(out);
    }

    private static String requireString(Jwt jwt, String name) throws AuthenticationException {
        String v = jwt.getClaimAsString(name);
        if (v == null || v.isBlank()) {
            throw new AuthenticationException("JWT is missing required claim '" + name + "'");
        }
        return v;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }
}
