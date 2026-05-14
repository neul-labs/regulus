package com.neullabs.regulus.adk.plugins;

import java.util.regex.Pattern;

final class BuiltInPatterns {

    private BuiltInPatterns() {}

    static Pattern regex(RegulusPrivacyPlugin.BuiltInPattern p) {
        return switch (p) {
            case NINO              -> Pattern.compile("\\b[A-CEGHJ-PR-TW-Z]{2}\\d{6}[A-D]\\b");
            case IBAN              -> Pattern.compile("\\b[A-Z]{2}\\d{2}[A-Z0-9]{1,30}\\b");
            case BIC               -> Pattern.compile("\\b[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?\\b");
            case SORT_CODE         -> Pattern.compile("\\b\\d{2}-\\d{2}-\\d{2}\\b");
            case UK_ACCOUNT_NUMBER -> Pattern.compile("\\b\\d{8}\\b");
            case UK_POSTCODE       -> Pattern.compile("\\b[A-Z]{1,2}\\d[A-Z\\d]?\\s*\\d[A-Z]{2}\\b");
            case EMAIL             -> Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
            case NHS_NUMBER        -> Pattern.compile("\\b\\d{3}\\s?\\d{3}\\s?\\d{4}\\b");
        };
    }
}
