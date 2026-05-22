package com.neullabs.regulus.identity.oidc;

import com.neullabs.regulus.identity.Identity;
import com.neullabs.regulus.identity.IdentityHolder;
import com.neullabs.regulus.identity.Jurisdiction;
import com.neullabs.regulus.identity.bridge.PolicyContextBridge;
import com.neullabs.regulus.identity.spi.AuthenticationException;
import com.neullabs.regulus.identity.spi.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end sanity check of the OIDC inbound path. Boots no Spring context —
 * just exercises the wire that production traffic takes:
 *
 * <ol>
 *   <li>{@link JwtAuthenticationToken} arrives (post-Spring-Security)</li>
 *   <li>{@link OidcIdentityAdapter} mints a Regulus {@link Identity}</li>
 *   <li>{@code IdentityHolder} carries it for the request thread</li>
 *   <li>{@link PolicyContextBridge} derives both legacy
 *       {@code PolicyContext} shapes from the Identity</li>
 * </ol>
 *
 * <p>If this passes, the contract this PR locks in works in isolation.
 * Booting Spring + MockMvc + an embedded OAuth2 resource server is the
 * larger-scoped integration test that lands once the build pipeline has a
 * JDK 21 toolchain confirmed.
 */
class OidcIdentityEndToEndTest {

    @AfterEach
    void tearDown() {
        IdentityHolder.clear();
    }

    private JwtAuthenticationToken realisticJwt() {
        Jwt jwt = Jwt.withTokenValue("dummy")
                .header("alg", "RS256")
                .issuer("https://idp.example/realms/regulated-prod")
                .subject("u-42")
                .claim("preferred_username", "alice")
                .claim("regulus.tenant", "acme-bank")
                .claim("regulus.jurisdiction", "EU_UK")
                .claim("regulus.purpose", List.of("retail-support"))
                .claim("regulus.lawful_basis", List.of("contract"))
                .claim("roles", List.of("agent-operator", "regulus.killswitch.requester"))
                .claim("legalEntityIdentifier", "529900LWLPYR7C5DOL90")
                .issuedAt(Instant.parse("2026-05-22T10:00:00Z"))
                .expiresAt(Instant.parse("2026-05-22T11:00:00Z"))
                .build();
        return new JwtAuthenticationToken(jwt);
    }

    @Test
    void jwtFlowsThroughAdapterIntoHolderAndDerivesPolicyContext() throws AuthenticationException {
        var adapter = new OidcIdentityAdapter();
        var ctx = new RequestContext("https", Map.of(), Optional.empty(),
                Map.of(OidcIdentityAdapter.SPRING_AUTH_KEY, realisticJwt()));

        // Step 1+2: adapter mints Identity
        Identity identity = adapter.authenticate(ctx);
        assertThat(identity.principal().id()).isEqualTo("u-42");
        assertThat(identity.claims().tenantId()).isEqualTo("acme-bank");
        assertThat(identity.claims().jurisdiction()).isEqualTo(Jurisdiction.EU_UK);
        assertThat(identity.claims().hasPurpose("retail-support")).isTrue();
        assertThat(identity.claims().hasRole("regulus.killswitch.requester")).isTrue();

        // Step 3: filter binds it to the request thread
        IdentityHolder.set(identity);
        assertThat(IdentityHolder.get()).contains(identity);

        // Step 4: bridge derives the policy-model PolicyContext for downstream guards
        var policyCtx = PolicyContextBridge.toPolicyModel(identity, "retail-support", "corr-001");

        assertThat(policyCtx.getUserId()).contains("u-42");
        assertThat(policyCtx.getPurposeCode()).contains("retail-support");
        assertThat(policyCtx.getCorrelationId()).contains("corr-001");
        assertThat(policyCtx.getLegalEntityIdentifier()).contains("529900LWLPYR7C5DOL90");
        assertThat(policyCtx.getAttributes())
                .containsEntry("regulus.tenant", "acme-bank")
                .containsEntry("regulus.jurisdiction", "EU_UK")
                .containsEntry("regulus.identity.adapter", "oidc")
                .containsEntry("regulus.roles",
                        // Set ordering is not guaranteed — both members must be present in the joined string
                        policyCtx.getAttributes().get("regulus.roles"));
        assertThat(policyCtx.getAttributes().get("regulus.roles"))
                .contains("agent-operator")
                .contains("regulus.killswitch.requester");
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedAdkPluginPolicyContextStillDerivesCorrectly() throws AuthenticationException {
        var adapter = new OidcIdentityAdapter();
        var identity = adapter.authenticate(new RequestContext("https", Map.of(), Optional.empty(),
                Map.of(OidcIdentityAdapter.SPRING_AUTH_KEY, realisticJwt())));

        var adkCtx = PolicyContextBridge.toAdkPlugin(identity, "retail-support", null, "model", "gemini-1.5");

        assertThat(adkCtx.purposeCode()).isEqualTo("retail-support");
        assertThat(adkCtx.subjectId()).isEqualTo("u-42");
        assertThat(adkCtx.actor()).isEqualTo("u-42");
        assertThat(adkCtx.targetKind()).isEqualTo("model");
        assertThat(adkCtx.targetId()).isEqualTo("gemini-1.5");
        assertThat(adkCtx.attributes())
                .containsEntry("regulus.tenant", "acme-bank")
                .containsEntry("regulus.jurisdiction", "EU_UK");
    }
}
