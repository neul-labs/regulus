package com.regulus.platform.policy.guard;

import com.regulus.platform.policy.model.PolicyContext;
import com.regulus.platform.policy.model.PolicyResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Purpose Code Policy Guard")
class PurposeCodePolicyGuardTest {

    private PurposeCodePolicyGuard guard;

    @BeforeEach
    void setUp() {
        guard = new PurposeCodePolicyGuard();
    }

    @Test
    @DisplayName("should have correct policy name")
    void shouldHaveCorrectPolicyName() {
        assertThat(guard.getName()).isEqualTo("require.PurposeCode");
    }

    @Nested
    @DisplayName("when purpose code is missing")
    class WhenPurposeCodeMissing {

        @Test
        @DisplayName("should deny request")
        void shouldDenyRequest() {
            PolicyContext context = PolicyContext.builder()
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isDenied()).isTrue();
            assertThat(result.getViolations()).hasSize(1);
            assertThat(result.getViolations().get(0).violationType()).isEqualTo("MISSING_PURPOSE_CODE");
        }
    }

    @Nested
    @DisplayName("when purpose code is invalid")
    class WhenPurposeCodeInvalid {

        @Test
        @DisplayName("should deny unknown purpose code")
        void shouldDenyUnknownPurposeCode() {
            PolicyContext context = PolicyContext.builder()
                .purposeCode("UNKNOWN_PURPOSE")
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isDenied()).isTrue();
            assertThat(result.getViolations().get(0).violationType()).isEqualTo("INVALID_PURPOSE_CODE");
        }
    }

    @Nested
    @DisplayName("when purpose code is valid")
    class WhenPurposeCodeValid {

        @ParameterizedTest
        @ValueSource(strings = {
            "CONTRACT_PERFORMANCE",
            "LEGAL_OBLIGATION",
            "CONSENT",
            "LEGITIMATE_INTEREST",
            "FRAUD_PREVENTION",
            "AML_KYC",
            "CUSTOMER_SERVICE",
            "PAYMENT_PROCESSING",
            "REGULATORY_REPORTING"
        })
        @DisplayName("should allow standard GDPR purpose codes")
        void shouldAllowValidPurposeCodes(String purposeCode) {
            PolicyContext context = PolicyContext.builder()
                .purposeCode(purposeCode)
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("should allow case insensitive purpose codes")
        void shouldAllowCaseInsensitive() {
            PolicyContext context = PolicyContext.builder()
                .purposeCode("contract_performance")
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isAllowed()).isTrue();
        }
    }

    @Nested
    @DisplayName("lawful basis alignment")
    class LawfulBasisAlignment {

        @Test
        @DisplayName("should allow aligned purpose and lawful basis")
        void shouldAllowAlignedPurposeAndBasis() {
            PolicyContext context = PolicyContext.builder()
                .purposeCode("CONTRACT_PERFORMANCE")
                .lawfulBasis("CONTRACT")
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("should deny misaligned purpose and lawful basis")
        void shouldDenyMisalignedPurposeAndBasis() {
            PolicyContext context = PolicyContext.builder()
                .purposeCode("CONTRACT_PERFORMANCE")
                .lawfulBasis("CONSENT")  // Should be CONTRACT
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isDenied()).isTrue();
            assertThat(result.getViolations().get(0).violationType()).isEqualTo("LAWFUL_BASIS_MISMATCH");
        }

        @Test
        @DisplayName("should allow AML_KYC with LEGAL_OBLIGATION basis")
        void shouldAllowAmlKycWithLegalObligation() {
            PolicyContext context = PolicyContext.builder()
                .purposeCode("AML_KYC")
                .lawfulBasis("LEGAL_OBLIGATION")
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isAllowed()).isTrue();
        }
    }
}
