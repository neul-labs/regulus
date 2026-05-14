package com.neullabs.regulus.policy.guard;

import com.neullabs.regulus.policy.model.PolicyContext;
import com.neullabs.regulus.policy.model.PolicyResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Consent Policy Guard")
class ConsentPolicyGuardTest {

    private ConsentPolicyGuard guard;

    @BeforeEach
    void setUp() {
        guard = new ConsentPolicyGuard();
    }

    @Test
    @DisplayName("should have correct policy name")
    void shouldHaveCorrectPolicyName() {
        assertThat(guard.getName()).isEqualTo("require.Consent");
    }

    @Nested
    @DisplayName("when consent is not required")
    class WhenConsentNotRequired {

        @Test
        @DisplayName("should allow without explicit consent")
        void shouldAllowWithoutConsent() {
            PolicyContext context = PolicyContext.builder()
                .purposeCode("CONTRACT_PERFORMANCE")
                .lawfulBasis("CONTRACT")
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isAllowed()).isTrue();
        }
    }

    @Nested
    @DisplayName("when consent is required by lawful basis")
    class WhenConsentRequiredByBasis {

        @Test
        @DisplayName("should deny if consent not granted")
        void shouldDenyIfConsentNotGranted() {
            PolicyContext context = PolicyContext.builder()
                .lawfulBasis("CONSENT")
                .consentGranted(false)
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isDenied()).isTrue();
            assertThat(result.getViolations().get(0).violationType()).isEqualTo("CONSENT_NOT_GRANTED");
        }

        @Test
        @DisplayName("should allow if consent granted")
        void shouldAllowIfConsentGranted() {
            PolicyContext context = PolicyContext.builder()
                .lawfulBasis("CONSENT")
                .consentGranted(true)
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isAllowed()).isTrue();
        }
    }

    @Nested
    @DisplayName("when consent is required by purpose code")
    class WhenConsentRequiredByPurpose {

        @Test
        @DisplayName("should deny if consent purpose but not granted")
        void shouldDenyConsentPurposeNotGranted() {
            PolicyContext context = PolicyContext.builder()
                .purposeCode("CONSENT")
                .consentGranted(false)
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isDenied()).isTrue();
        }

        @Test
        @DisplayName("should allow if consent purpose and granted")
        void shouldAllowConsentPurposeAndGranted() {
            PolicyContext context = PolicyContext.builder()
                .purposeCode("CONSENT")
                .consentGranted(true)
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isAllowed()).isTrue();
        }
    }

    @Nested
    @DisplayName("when consent is withdrawn")
    class WhenConsentWithdrawn {

        @Test
        @DisplayName("should deny if consent status is withdrawn")
        void shouldDenyWithdrawnConsent() {
            PolicyContext context = PolicyContext.builder()
                .consentGranted(true)
                .correlationId("test-123")
                .attribute("consentStatus", "WITHDRAWN")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isDenied()).isTrue();
            assertThat(result.getViolations().get(0).violationType()).isEqualTo("CONSENT_WITHDRAWN");
        }
    }

    @Nested
    @DisplayName("when consent is explicitly required via attribute")
    class WhenConsentExplicitlyRequired {

        @Test
        @DisplayName("should deny if requires consent attribute is true but not granted")
        void shouldDenyWhenRequiresConsentAttribute() {
            PolicyContext context = PolicyContext.builder()
                .correlationId("test-123")
                .attribute("requiresConsent", true)
                .consentGranted(false)
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isDenied()).isTrue();
        }

        @Test
        @DisplayName("should allow if requires consent attribute is true and granted")
        void shouldAllowWhenConsentRequiredAndGranted() {
            PolicyContext context = PolicyContext.builder()
                .correlationId("test-123")
                .attribute("requiresConsent", true)
                .consentGranted(true)
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isAllowed()).isTrue();
        }
    }
}
