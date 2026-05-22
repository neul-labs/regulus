package com.neullabs.regulus.identity.oidc;

import com.neullabs.regulus.identity.Identity;
import com.neullabs.regulus.identity.Jurisdiction;
import com.neullabs.regulus.identity.spi.AuthenticationException;
import com.neullabs.regulus.identity.spi.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OidcIdentityAdapterTest {

    private static JwtAuthenticationToken jwt(Map<String, Object> claims) {
        Jwt jwt = Jwt.withTokenValue("dummy")
                .header("alg", "RS256")
                .claims(c -> c.putAll(claims))
                .issuedAt(Instant.parse("2026-05-22T10:00:00Z"))
                .expiresAt(Instant.parse("2026-05-22T11:00:00Z"))
                .build();
        return new JwtAuthenticationToken(jwt);
    }

    private static RequestContext ctxWith(JwtAuthenticationToken jwtAuth) {
        return new RequestContext("https", Map.of(), Optional.empty(),
                Map.of(OidcIdentityAdapter.SPRING_AUTH_KEY, jwtAuth));
    }

    @Test
    void mapsCanonicalClaims() throws AuthenticationException {
        var adapter = new OidcIdentityAdapter();
        var identity = adapter.authenticate(ctxWith(jwt(Map.of(
                "sub", "u-42",
                "preferred_username", "alice",
                "regulus.tenant", "acme",
                "regulus.jurisdiction", "EU_UK",
                "regulus.purpose", List.of("retail-support", "fraud-investigation"),
                "regulus.lawful_basis", List.of("consent"),
                "scope", "openid profile",
                "roles", List.of("agent-operator", "regulus.killswitch.requester"),
                "iss", "https://idp.example"))));

        assertThat(identity.principal().id()).isEqualTo("u-42");
        assertThat(identity.principal().displayName()).isEqualTo("alice");
        assertThat(identity.claims().tenantId()).isEqualTo("acme");
        assertThat(identity.claims().jurisdiction()).isEqualTo(Jurisdiction.EU_UK);
        assertThat(identity.claims().purposeCodes())
                .containsExactlyInAnyOrder("retail-support", "fraud-investigation");
        assertThat(identity.claims().lawfulBases()).containsExactly("consent");
        assertThat(identity.claims().roles())
                .contains("openid", "profile", "agent-operator", "regulus.killswitch.requester");
        assertThat(identity.provenance().adapterId()).isEqualTo("oidc");
        assertThat(identity.provenance().tokenIssuer()).isEqualTo("https://idp.example");
    }

    @Test
    void missingTenantClaimRejected() {
        var adapter = new OidcIdentityAdapter();
        assertThatThrownBy(() -> adapter.authenticate(ctxWith(jwt(Map.of(
                "sub", "u-1",
                "regulus.jurisdiction", "EU")))))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("tenant");
    }

    @Test
    void missingJurisdictionRejected() {
        var adapter = new OidcIdentityAdapter();
        assertThatThrownBy(() -> adapter.authenticate(ctxWith(jwt(Map.of(
                "sub", "u-1",
                "regulus.tenant", "acme")))))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("jurisdiction");
    }

    @Test
    void unsupportedJurisdictionRejected() {
        var adapter = new OidcIdentityAdapter();
        assertThatThrownBy(() -> adapter.authenticate(ctxWith(jwt(Map.of(
                "sub", "u-1",
                "regulus.tenant", "acme",
                "regulus.jurisdiction", "US")))))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Unsupported");
    }

    @Test
    void realmAccessRolesAreMerged() throws AuthenticationException {
        var identity = new OidcIdentityAdapter().authenticate(ctxWith(jwt(Map.of(
                "sub", "u-1",
                "regulus.tenant", "acme",
                "regulus.jurisdiction", "UK",
                "realm_access", Map.of("roles", List.of("realm-role-1", "realm-role-2"))))));

        assertThat(identity.claims().roles()).contains("realm-role-1", "realm-role-2");
    }

    @Test
    void unmappedClaimsForwardedAsExtensions() throws AuthenticationException {
        var identity = new OidcIdentityAdapter().authenticate(ctxWith(jwt(Map.of(
                "sub", "u-1",
                "regulus.tenant", "acme",
                "regulus.jurisdiction", "UK",
                "legalEntityIdentifier", "529900LWLPYR7C5DOL90",
                "dept", "ops"))));

        assertThat(identity.claims().extensions())
                .containsEntry("legalEntityIdentifier", "529900LWLPYR7C5DOL90")
                .containsEntry("dept", "ops");
    }
}
