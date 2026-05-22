package com.neullabs.regulus.identity.bridge;

import com.neullabs.regulus.identity.Claims;
import com.neullabs.regulus.identity.Identity;
import com.neullabs.regulus.identity.Principal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Derives the two legacy {@code PolicyContext} shapes from a canonical
 * {@link Identity}. Every adapter populates {@code Identity}; this bridge is
 * the single point where Regulus' new identity model meets the existing
 * policy and ADK-plugin code paths.
 *
 * <p>Fields not present on {@code Identity} (purpose code chosen for *this*
 * call, target of the invocation, etc.) are accepted as explicit arguments —
 * they belong to the request, not the principal.
 */
public final class PolicyContextBridge {

    private PolicyContextBridge() {}

    /**
     * Builds the policy-model {@code PolicyContext} (the long-term home for
     * policy inputs). LEI, lawful basis and consent are taken from
     * {@link Claims} when present; absence is encoded as the field being
     * unset, matching the existing {@code Optional}-based getters.
     */
    public static com.neullabs.regulus.policy.model.PolicyContext toPolicyModel(
            Identity identity, String purposeCode, String correlationId) {
        Objects.requireNonNull(identity, "identity");
        Claims claims = identity.claims();
        Principal principal = identity.principal();

        com.neullabs.regulus.policy.model.PolicyContext.Builder b =
                com.neullabs.regulus.policy.model.PolicyContext.builder()
                        .userId(principal.id())
                        .purposeCode(purposeCode);

        if (correlationId != null) {
            b.correlationId(correlationId);
        }
        // LEI lives in the extensions map per OIDC adapter convention; if a
        // caller wants a strongly-typed slot it can be promoted later.
        String lei = claims.extensions().get("legalEntityIdentifier");
        if (lei != null) {
            b.legalEntityIdentifier(lei);
        }
        if (!claims.lawfulBases().isEmpty()) {
            // Carry the first declared lawful basis as the "active" one; the
            // full set is available to consent/automated-decision guards via
            // the attribute map.
            b.lawfulBasis(claims.lawfulBases().iterator().next());
        }
        if (claims.lawfulBases().contains("consent")) {
            b.consentGranted(true);
        }
        b.attribute("regulus.tenant", claims.tenantId());
        b.attribute("regulus.jurisdiction", claims.jurisdiction().name());
        b.attribute("regulus.lawful_bases", String.join(",", claims.lawfulBases()));
        b.attribute("regulus.roles", String.join(",", claims.roles()));
        b.attribute("regulus.purposes", String.join(",", claims.purposeCodes()));
        b.attribute("regulus.identity.adapter", identity.provenance().adapterId());
        return b.build();
    }

    /**
     * Builds the (deprecated) ADK-plugin {@code PolicyContext}.
     *
     * @deprecated downstream code should migrate to consuming
     *             {@link com.neullabs.regulus.policy.model.PolicyContext}.
     *             This factory keeps existing call sites compiling during
     *             the transition.
     */
    @Deprecated
    public static com.neullabs.regulus.adk.plugins.PolicyContext toAdkPlugin(
            Identity identity,
            String purposeCode,
            String actor,
            String targetKind,
            String targetId) {
        Objects.requireNonNull(identity, "identity");
        Claims claims = identity.claims();
        Map<String, String> attrs = new HashMap<>(claims.extensions());
        attrs.put("regulus.tenant", claims.tenantId());
        attrs.put("regulus.jurisdiction", claims.jurisdiction().name());
        attrs.put("regulus.roles", String.join(",", claims.roles()));
        attrs.put("regulus.purposes", String.join(",", claims.purposeCodes()));
        attrs.put("regulus.identity.adapter", identity.provenance().adapterId());
        return new com.neullabs.regulus.adk.plugins.PolicyContext(
                purposeCode,
                identity.principal().id(),
                actor == null ? identity.principal().id() : actor,
                targetKind,
                targetId,
                Map.copyOf(attrs));
    }
}
