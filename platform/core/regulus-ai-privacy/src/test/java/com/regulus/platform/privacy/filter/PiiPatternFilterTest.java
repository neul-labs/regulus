package com.regulus.platform.privacy.filter;

import com.regulus.platform.privacy.model.RedactionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PII Pattern Filter")
class PiiPatternFilterTest {

    private PiiPatternFilter filter;

    @BeforeEach
    void setUp() {
        filter = new PiiPatternFilter();
    }

    @Test
    @DisplayName("should have correct filter name")
    void shouldHaveCorrectName() {
        assertThat(filter.getName()).isEqualTo("pii-pattern");
    }

    @Test
    @DisplayName("should support all content types")
    void shouldSupportAllContentTypes() {
        assertThat(filter.supports("text/plain")).isTrue();
        assertThat(filter.supports("application/json")).isTrue();
    }

    @Nested
    @DisplayName("UK Sort Code")
    class UkSortCode {

        @Test
        @DisplayName("should redact sort code")
        void shouldRedactSortCode() {
            String content = "Sort code: 12-34-56";

            RedactionResult result = filter.redact(content);

            assertThat(result.hasRedactions()).isTrue();
            assertThat(result.redactedContent()).contains("[SORT:**-**-**]");
            assertThat(result.redactedContent()).doesNotContain("12-34-56");
        }
    }

    @Nested
    @DisplayName("Payment Card Number")
    class PaymentCard {

        @Test
        @DisplayName("should redact card number with dashes")
        void shouldRedactCardWithDashes() {
            String content = "Card: 4111-1111-1111-1111";

            RedactionResult result = filter.redact(content);

            assertThat(result.hasRedactions()).isTrue();
            assertThat(result.redactedContent()).contains("[CARD:****-****-****-****]");
        }

        @Test
        @DisplayName("should redact card number with spaces")
        void shouldRedactCardWithSpaces() {
            String content = "Card: 4111 1111 1111 1111";

            RedactionResult result = filter.redact(content);

            assertThat(result.hasRedactions()).isTrue();
        }
    }

    @Nested
    @DisplayName("Date of Birth")
    class DateOfBirth {

        @Test
        @DisplayName("should redact DOB in DD/MM/YYYY format")
        void shouldRedactDobDdMmYyyy() {
            String content = "DOB: 15/03/1985";

            RedactionResult result = filter.redact(content);

            assertThat(result.hasRedactions()).isTrue();
            assertThat(result.redactedContent()).contains("[DOB:**/**/****]");
        }

        @Test
        @DisplayName("should redact DOB with dashes")
        void shouldRedactDobWithDashes() {
            String content = "Born: 15-03-1985";

            RedactionResult result = filter.redact(content);

            assertThat(result.hasRedactions()).isTrue();
        }
    }

    @Test
    @DisplayName("should return unchanged for null content")
    void shouldHandleNullContent() {
        RedactionResult result = filter.redact(null);

        assertThat(result.hasRedactions()).isFalse();
    }

    @Test
    @DisplayName("should return unchanged for empty content")
    void shouldHandleEmptyContent() {
        RedactionResult result = filter.redact("");

        assertThat(result.hasRedactions()).isFalse();
    }

    @Test
    @DisplayName("should return unchanged for content without PII")
    void shouldHandleNoPii() {
        String content = "This is a normal message with no sensitive data.";

        RedactionResult result = filter.redact(content);

        assertThat(result.hasRedactions()).isFalse();
        assertThat(result.redactedContent()).isEqualTo(content);
    }

    @Test
    @DisplayName("should include metadata about patterns checked")
    void shouldIncludeMetadata() {
        String content = "Sort code: 12-34-56";

        RedactionResult result = filter.redact(content);

        assertThat(result.metadata()).containsKey("patternsChecked");
        assertThat(result.metadata()).containsKey("originalLength");
        assertThat(result.metadata()).containsKey("redactedLength");
    }
}
