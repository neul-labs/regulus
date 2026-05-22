package com.neullabs.regulus.identity.bridge;

import com.neullabs.regulus.identity.Claims;
import com.neullabs.regulus.identity.Identity;
import com.neullabs.regulus.identity.Jurisdiction;
import com.neullabs.regulus.identity.Principal;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyContextBridgeTest {

    private Identity identity() {
        return new Identity(
                new Principal("u-1", "Alice", Principal.PrincipalType.HUMAN),
                new Claims(
                        "acme",
                        Jurisdiction.UK,
                        Set.of("retail-support"),
                        Set.of("agent-operator"),
                        Set.of("consent", "legitimate-interest"),
                        Map.of("legalEntityIdentifier", "529900LWLPYR7C5DOL90", "dept", "ops")),
                new Identity.Provenance("oidc", Instant.now(), null, "https://idp.example"));
    }

    @Test
    void toPolicyModelCarriesIdentityFields() {
        var ctx = PolicyContextBridge.toPolicyModel(identity(), "purpose-42", "corr-1");

        assertThat(ctx.getUserId()).contains("u-1");
        assertThat(ctx.getPurposeCode()).contains("purpose-42");
        assertThat(ctx.getCorrelationId()).contains("corr-1");
        assertThat(ctx.getLegalEntityIdentifier()).contains("529900LWLPYR7C5DOL90");
        assertThat(ctx.getLawfulBasis()).isPresent();
        assertThat(ctx.isConsentGranted()).isTrue();
        assertThat(ctx.getAttributes())
                .containsEntry("regulus.tenant", "acme")
                .containsEntry("regulus.jurisdiction", "UK")
                .containsEntry("regulus.identity.adapter", "oidc");
    }

    @Test
    @SuppressWarnings("deprecation")
    void toAdkPluginCarriesIdentityFields() {
        var ctx = PolicyContextBridge.toAdkPlugin(identity(), "purpose-42", null, "model", "gemini-1.5");

        assertThat(ctx.purposeCode()).isEqualTo("purpose-42");
        assertThat(ctx.subjectId()).isEqualTo("u-1");
        assertThat(ctx.actor()).isEqualTo("u-1");
        assertThat(ctx.targetKind()).isEqualTo("model");
        assertThat(ctx.targetId()).isEqualTo("gemini-1.5");
        assertThat(ctx.attributes())
                .containsEntry("regulus.tenant", "acme")
                .containsEntry("regulus.jurisdiction", "UK")
                .containsEntry("dept", "ops");
    }
}
