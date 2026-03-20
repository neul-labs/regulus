package com.regulus.platform.policy.guard;

import com.regulus.platform.policy.model.PolicyContext;
import com.regulus.platform.policy.model.PolicyResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LEI Policy Guard")
class LeiPolicyGuardTest {

    private LeiPolicyGuard guard;

    @BeforeEach
    void setUp() {
        guard = new LeiPolicyGuard();
    }

    @Test
    @DisplayName("should have correct policy name")
    void shouldHaveCorrectPolicyName() {
        assertThat(guard.getName()).isEqualTo("require.LEI");
    }

    @Test
    @DisplayName("should handle its own policy name")
    void shouldHandleOwnPolicyName() {
        assertThat(guard.handles("require.LEI")).isTrue();
        assertThat(guard.handles("require.Other")).isFalse();
    }

    @Nested
    @DisplayName("when LEI is missing")
    class WhenLeiMissing {

        @Test
        @DisplayName("should deny request")
        void shouldDenyRequest() {
            PolicyContext context = PolicyContext.builder()
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isDenied()).isTrue();
            assertThat(result.getViolations()).hasSize(1);
            assertThat(result.getViolations().get(0).violationType()).isEqualTo("MISSING_LEI");
        }
    }

    @Nested
    @DisplayName("when LEI format is invalid")
    class WhenLeiInvalid {

        @Test
        @DisplayName("should deny short LEI")
        void shouldDenyShortLei() {
            PolicyContext context = PolicyContext.builder()
                .legalEntityIdentifier("ABC123")
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isDenied()).isTrue();
            assertThat(result.getViolations().get(0).violationType()).isEqualTo("INVALID_LEI_FORMAT");
        }

        @Test
        @DisplayName("should deny LEI with invalid characters")
        void shouldDenyInvalidCharacters() {
            PolicyContext context = PolicyContext.builder()
                .legalEntityIdentifier("5493001KJTIIGC8Y1R!2")
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isDenied()).isTrue();
        }

        @Test
        @DisplayName("should deny LEI with wrong length")
        void shouldDenyWrongLength() {
            PolicyContext context = PolicyContext.builder()
                .legalEntityIdentifier("5493001KJTIIGC8Y1R1234")
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isDenied()).isTrue();
        }
    }

    @Nested
    @DisplayName("when LEI is valid")
    class WhenLeiValid {

        @Test
        @DisplayName("should allow valid LEI format")
        void shouldAllowValidLei() {
            // Valid LEI format: 18 alphanumeric + 2 digits
            PolicyContext context = PolicyContext.builder()
                .legalEntityIdentifier("5493001KJTIIGC8Y1R12")
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isAllowed()).isTrue();
            assertThat(result.getViolations()).isEmpty();
        }

        @Test
        @DisplayName("should allow lowercase LEI (case insensitive)")
        void shouldAllowLowercaseLei() {
            PolicyContext context = PolicyContext.builder()
                .legalEntityIdentifier("5493001kjtiigc8y1r12")
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("should allow real-world LEI examples")
        void shouldAllowRealWorldLei() {
            // HSBC Holdings plc LEI
            PolicyContext context = PolicyContext.builder()
                .legalEntityIdentifier("MLU0ZO3ML4LN2LL2TL39")
                .correlationId("test-123")
                .build();

            PolicyResult result = guard.evaluate(context);

            assertThat(result.isAllowed()).isTrue();
        }
    }
}
