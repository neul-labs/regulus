package com.neullabs.regulus.policy.guard;

import com.neullabs.regulus.policy.model.PolicyContext;
import com.neullabs.regulus.policy.model.PolicyResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Policy Enforcer")
class PolicyEnforcerTest {

    private PolicyEnforcer enforcer;
    private LeiPolicyGuard leiGuard;
    private PurposeCodePolicyGuard purposeCodeGuard;
    private ConsentPolicyGuard consentGuard;

    @BeforeEach
    void setUp() {
        leiGuard = new LeiPolicyGuard();
        purposeCodeGuard = new PurposeCodePolicyGuard();
        consentGuard = new ConsentPolicyGuard();
        enforcer = new PolicyEnforcer(List.of(leiGuard, purposeCodeGuard, consentGuard));
    }

    @Test
    @DisplayName("should list all registered policies")
    void shouldListRegisteredPolicies() {
        List<String> policies = enforcer.getRegisteredPolicies();

        assertThat(policies).containsExactlyInAnyOrder(
            "require.LEI",
            "require.PurposeCode",
            "require.Consent"
        );
    }

    @Nested
    @DisplayName("enforce all policies")
    class EnforceAll {

        @Test
        @DisplayName("should pass when all policies satisfied")
        void shouldPassWhenAllSatisfied() {
            PolicyContext context = PolicyContext.builder()
                .legalEntityIdentifier("MLU0ZO3ML4LN2LL2TL39")
                .purposeCode("CONTRACT_PERFORMANCE")
                .lawfulBasis("CONTRACT")
                .correlationId("test-123")
                .build();

            PolicyResult result = enforcer.enforceAll(context);

            assertThat(result.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("should fail when LEI missing")
        void shouldFailWhenLeiMissing() {
            PolicyContext context = PolicyContext.builder()
                .purposeCode("CONTRACT_PERFORMANCE")
                .correlationId("test-123")
                .build();

            PolicyResult result = enforcer.enforceAll(context);

            assertThat(result.isDenied()).isTrue();
            assertThat(result.getViolations()).anyMatch(v -> v.policyName().equals("require.LEI"));
        }

        @Test
        @DisplayName("should collect multiple violations")
        void shouldCollectMultipleViolations() {
            PolicyContext context = PolicyContext.builder()
                .correlationId("test-123")
                .build();

            PolicyResult result = enforcer.enforceAll(context);

            assertThat(result.isDenied()).isTrue();
            assertThat(result.getViolations()).hasSizeGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    @DisplayName("enforce specific policies")
    class EnforceSpecific {

        @Test
        @DisplayName("should only enforce specified policies")
        void shouldOnlyEnforceSpecified() {
            PolicyContext context = PolicyContext.builder()
                .legalEntityIdentifier("MLU0ZO3ML4LN2LL2TL39")
                // Missing purpose code
                .correlationId("test-123")
                .build();

            // Only enforce LEI
            PolicyResult result = enforcer.enforce(context, "require.LEI");

            assertThat(result.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("should report unknown policies")
        void shouldReportUnknownPolicies() {
            PolicyContext context = PolicyContext.builder()
                .correlationId("test-123")
                .build();

            PolicyResult result = enforcer.enforce(context, "unknown.policy");

            assertThat(result.isDenied()).isTrue();
            assertThat(result.getViolations().get(0).violationType()).isEqualTo("UNKNOWN_POLICY");
        }
    }

    @Nested
    @DisplayName("enforce any policy (OR mode)")
    class EnforceAny {

        @Test
        @DisplayName("should pass if any policy passes")
        void shouldPassIfAnyPasses() {
            PolicyContext context = PolicyContext.builder()
                .legalEntityIdentifier("MLU0ZO3ML4LN2LL2TL39")
                // Missing purpose code - but LEI passes
                .correlationId("test-123")
                .build();

            PolicyResult result = enforcer.enforceAny(context, "require.LEI", "require.PurposeCode");

            assertThat(result.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("should fail if no policy passes")
        void shouldFailIfNonePasses() {
            PolicyContext context = PolicyContext.builder()
                // Missing both LEI and purpose code
                .correlationId("test-123")
                .build();

            PolicyResult result = enforcer.enforceAny(context, "require.LEI", "require.PurposeCode");

            assertThat(result.isDenied()).isTrue();
            assertThat(result.getViolations()).hasSizeGreaterThanOrEqualTo(2);
        }
    }
}
