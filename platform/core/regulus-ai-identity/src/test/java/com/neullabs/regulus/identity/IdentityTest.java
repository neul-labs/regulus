package com.neullabs.regulus.identity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityTest {

    private static Identity sampleIdentity(Instant expiry) {
        return new Identity(
                new Principal("user-42", "Alice", Principal.PrincipalType.HUMAN),
                new Claims(
                        "acme",
                        Jurisdiction.EU_UK,
                        Set.of("retail-support"),
                        Set.of("agent-operator"),
                        Set.of("contract", "legitimate-interest"),
                        Map.of("dept", "ops")),
                new Identity.Provenance("oidc", Instant.now(), expiry, "https://idp.example"));
    }

    @Test
    void claimsCopyDefensively() {
        Identity id = sampleIdentity(null);
        assertThatThrownBy(() -> id.claims().roles().add("admin"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void hasRoleAndPurpose() {
        Identity id = sampleIdentity(null);
        assertThat(id.claims().hasRole("agent-operator")).isTrue();
        assertThat(id.claims().hasRole("admin")).isFalse();
        assertThat(id.claims().hasPurpose("retail-support")).isTrue();
    }

    @Test
    void expiryDetectedAtBoundary() {
        Instant now = Instant.now();
        Identity expired = sampleIdentity(now.minus(1, ChronoUnit.SECONDS));
        Identity live = sampleIdentity(now.plus(60, ChronoUnit.SECONDS));
        Identity noExpiry = sampleIdentity(null);

        assertThat(expired.isExpired(now)).isTrue();
        assertThat(live.isExpired(now)).isFalse();
        assertThat(noExpiry.isExpired(now)).isFalse();
    }

    @Test
    void principalRequiresIdAndType() {
        assertThatThrownBy(() -> new Principal(null, "x", Principal.PrincipalType.HUMAN))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Principal("x", "x", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void holderRoundTripsAndClears() {
        Identity id = sampleIdentity(null);
        assertThat(IdentityHolder.get()).isEmpty();
        IdentityHolder.set(id);
        try {
            assertThat(IdentityHolder.get()).contains(id);
            assertThat(IdentityHolder.require()).isSameAs(id);
        } finally {
            IdentityHolder.clear();
        }
        assertThat(IdentityHolder.get()).isEmpty();
        assertThatThrownBy(IdentityHolder::require).isInstanceOf(IllegalStateException.class);
    }
}
