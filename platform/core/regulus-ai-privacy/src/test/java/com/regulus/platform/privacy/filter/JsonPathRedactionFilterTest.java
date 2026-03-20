package com.regulus.platform.privacy.filter;

import com.regulus.platform.privacy.model.RedactionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JsonPath Redaction Filter")
class JsonPathRedactionFilterTest {

    private JsonPathRedactionFilter filter;

    @BeforeEach
    void setUp() {
        List<JsonPathRedactionFilter.RedactionRule> rules = List.of(
            JsonPathRedactionFilter.RedactionRule.of("$.customer.nationalId", "NATIONAL_ID"),
            JsonPathRedactionFilter.RedactionRule.of("$.customer.email", "EMAIL"),
            JsonPathRedactionFilter.RedactionRule.of("$.account.number", "BANK_ACCOUNT"),
            JsonPathRedactionFilter.RedactionRule.of(
                "$.payment.cardNumber",
                "PAYMENT_CARD",
                JsonPathRedactionFilter.ReplacementStrategy.TOKEN
            )
        );
        filter = new JsonPathRedactionFilter(rules);
    }

    @Test
    @DisplayName("should have correct filter name")
    void shouldHaveCorrectName() {
        assertThat(filter.getName()).isEqualTo("jsonpath-redaction");
    }

    @Test
    @DisplayName("should support JSON content types")
    void shouldSupportJsonContentTypes() {
        assertThat(filter.supports("application/json")).isTrue();
        assertThat(filter.supports("application/json; charset=utf-8")).isTrue();
        assertThat(filter.supports("text/plain")).isFalse();
    }

    @Nested
    @DisplayName("simple field redaction")
    class SimpleFieldRedaction {

        @Test
        @DisplayName("should redact top-level nested field")
        void shouldRedactNestedField() {
            String json = """
                {
                    "customer": {
                        "name": "John Doe",
                        "nationalId": "AB123456C"
                    }
                }
                """;

            RedactionResult result = filter.redact(json);

            assertThat(result.hasRedactions()).isTrue();
            assertThat(result.redactedContent()).contains("[REDACTED]");
            assertThat(result.redactedContent()).doesNotContain("AB123456C");
        }

        @Test
        @DisplayName("should redact email field")
        void shouldRedactEmailField() {
            String json = """
                {
                    "customer": {
                        "email": "john@example.com"
                    }
                }
                """;

            RedactionResult result = filter.redact(json);

            assertThat(result.hasRedactions()).isTrue();
            assertThat(result.redactedContent()).doesNotContain("john@example.com");
        }
    }

    @Nested
    @DisplayName("replacement strategies")
    class ReplacementStrategies {

        @Test
        @DisplayName("should use MASK strategy by default")
        void shouldUseMaskByDefault() {
            String json = """
                {"customer": {"nationalId": "AB123456C"}}
                """;

            RedactionResult result = filter.redact(json);

            assertThat(result.redactedContent()).contains("[REDACTED]");
        }

        @Test
        @DisplayName("should use TOKEN strategy when configured")
        void shouldUseTokenStrategy() {
            String json = """
                {"payment": {"cardNumber": "4111111111111111"}}
                """;

            RedactionResult result = filter.redact(json);

            assertThat(result.redactedContent()).contains("[TOKEN:PAYMENT_CARD]");
        }
    }

    @Nested
    @DisplayName("multiple fields")
    class MultipleFields {

        @Test
        @DisplayName("should redact multiple fields in same object")
        void shouldRedactMultipleFields() {
            String json = """
                {
                    "customer": {
                        "name": "John Doe",
                        "nationalId": "AB123456C",
                        "email": "john@example.com"
                    }
                }
                """;

            RedactionResult result = filter.redact(json);

            assertThat(result.redactionCount()).isEqualTo(2);
            assertThat(result.redactedContent()).doesNotContain("AB123456C");
            assertThat(result.redactedContent()).doesNotContain("john@example.com");
            assertThat(result.redactedContent()).contains("John Doe"); // Name not redacted
        }

        @Test
        @DisplayName("should redact fields across different objects")
        void shouldRedactAcrossObjects() {
            String json = """
                {
                    "customer": {"nationalId": "AB123456C"},
                    "account": {"number": "12345678"}
                }
                """;

            RedactionResult result = filter.redact(json);

            assertThat(result.redactionCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle missing fields gracefully")
        void shouldHandleMissingFields() {
            String json = """
                {"customer": {"name": "John"}}
                """;

            RedactionResult result = filter.redact(json);

            assertThat(result.hasRedactions()).isFalse();
            assertThat(result.redactedContent()).contains("John");
        }

        @Test
        @DisplayName("should return unchanged for invalid JSON")
        void shouldHandleInvalidJson() {
            String invalidJson = "not a valid json";

            RedactionResult result = filter.redact(invalidJson);

            assertThat(result.hasRedactions()).isFalse();
            assertThat(result.redactedContent()).isEqualTo(invalidJson);
        }

        @Test
        @DisplayName("should handle null content")
        void shouldHandleNull() {
            RedactionResult result = filter.redact(null);

            assertThat(result.hasRedactions()).isFalse();
        }

        @Test
        @DisplayName("should handle empty JSON")
        void shouldHandleEmptyJson() {
            RedactionResult result = filter.redact("{}");

            assertThat(result.hasRedactions()).isFalse();
        }
    }

    @Nested
    @DisplayName("metadata tracking")
    class MetadataTracking {

        @Test
        @DisplayName("should track redacted field paths")
        void shouldTrackRedactedPaths() {
            String json = """
                {"customer": {"nationalId": "AB123456C"}}
                """;

            RedactionResult result = filter.redact(json);

            assertThat(result.redactedFields()).hasSize(1);
            assertThat(result.redactedFields().get(0).path()).isEqualTo("$.customer.nationalId");
            assertThat(result.redactedFields().get(0).fieldType()).isEqualTo("NATIONAL_ID");
        }

        @Test
        @DisplayName("should include content length metadata")
        void shouldIncludeMetadata() {
            String json = """
                {"customer": {"nationalId": "AB123456C"}}
                """;

            RedactionResult result = filter.redact(json);

            assertThat(result.metadata()).containsKey("originalLength");
            assertThat(result.metadata()).containsKey("redactedLength");
            assertThat(result.metadata()).containsKey("rulesApplied");
        }
    }
}
