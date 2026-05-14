package com.neullabs.regulus.adk.plugins;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class BuiltInPatternsTest {

    @Test
    void ninoMatches() {
        assertThat(matches(RegulusPrivacyPlugin.BuiltInPattern.NINO, "AB123456C")).isTrue();
        assertThat(matches(RegulusPrivacyPlugin.BuiltInPattern.NINO, "QQ123456C")).isFalse(); // QQ disallowed
        assertThat(matches(RegulusPrivacyPlugin.BuiltInPattern.NINO, "AB12345C")).isFalse();  // too short
    }

    @Test
    void ibanMatches() {
        assertThat(matches(RegulusPrivacyPlugin.BuiltInPattern.IBAN, "GB82WEST12345698765432")).isTrue();
        assertThat(matches(RegulusPrivacyPlugin.BuiltInPattern.IBAN, "DE89370400440532013000")).isTrue();
    }

    @Test
    void bicMatches() {
        assertThat(matches(RegulusPrivacyPlugin.BuiltInPattern.BIC, "DEUTDEFF")).isTrue();
        assertThat(matches(RegulusPrivacyPlugin.BuiltInPattern.BIC, "DEUTDEFFXXX")).isTrue();
    }

    @Test
    void sortCodeMatches() {
        assertThat(matches(RegulusPrivacyPlugin.BuiltInPattern.SORT_CODE, "12-34-56")).isTrue();
        assertThat(matches(RegulusPrivacyPlugin.BuiltInPattern.SORT_CODE, "123456")).isFalse();
    }

    @Test
    void postcodeMatches() {
        assertThat(matches(RegulusPrivacyPlugin.BuiltInPattern.UK_POSTCODE, "SW1A 1AA")).isTrue();
        assertThat(matches(RegulusPrivacyPlugin.BuiltInPattern.UK_POSTCODE, "EC1V 9LT")).isTrue();
    }

    @Test
    void emailMatches() {
        assertThat(matches(RegulusPrivacyPlugin.BuiltInPattern.EMAIL, "test@example.com")).isTrue();
        assertThat(matches(RegulusPrivacyPlugin.BuiltInPattern.EMAIL, "notanemail")).isFalse();
    }

    @Test
    void nhsNumberMatches() {
        assertThat(matches(RegulusPrivacyPlugin.BuiltInPattern.NHS_NUMBER, "9434765919")).isTrue();
        assertThat(matches(RegulusPrivacyPlugin.BuiltInPattern.NHS_NUMBER, "943 476 5919")).isTrue();
    }

    @Test
    void allBuiltinsHaveAPattern() {
        for (RegulusPrivacyPlugin.BuiltInPattern p : RegulusPrivacyPlugin.BuiltInPattern.values()) {
            assertThat(BuiltInPatterns.regex(p))
                    .as("pattern for %s", p)
                    .isNotNull();
        }
    }

    private boolean matches(RegulusPrivacyPlugin.BuiltInPattern p, String text) {
        Pattern regex = BuiltInPatterns.regex(p);
        return regex.matcher(text).find();
    }
}
