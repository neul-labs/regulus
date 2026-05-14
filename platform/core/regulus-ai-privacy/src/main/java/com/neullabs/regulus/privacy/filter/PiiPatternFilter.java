package com.neullabs.regulus.privacy.filter;

import com.neullabs.regulus.privacy.model.RedactionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Privacy filter that detects and redacts PII using regex patterns.
 * Supports common UK financial services PII patterns.
 */
public class PiiPatternFilter implements PrivacyFilter {

    private static final Logger log = LoggerFactory.getLogger(PiiPatternFilter.class);

    private final List<PiiPattern> patterns;

    public PiiPatternFilter() {
        this.patterns = buildDefaultPatterns();
    }

    public PiiPatternFilter(List<PiiPattern> customPatterns) {
        this.patterns = new ArrayList<>(customPatterns);
    }

    @Override
    public String getName() {
        return "pii-pattern";
    }

    @Override
    public RedactionResult redact(String content) {
        if (content == null || content.isBlank()) {
            return RedactionResult.unchanged(content);
        }

        String redactedContent = content;
        List<RedactionResult.RedactedField> redactedFields = new ArrayList<>();
        Map<String, Object> metadata = new HashMap<>();

        for (PiiPattern piiPattern : patterns) {
            Matcher matcher = piiPattern.pattern().matcher(redactedContent);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String replacement = piiPattern.replacement();
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));

                redactedFields.add(new RedactionResult.RedactedField(
                    piiPattern.name(),
                    piiPattern.fieldType(),
                    replacement
                ));

                log.debug("Redacted PII pattern '{}' at position {}-{}",
                    piiPattern.name(), matcher.start(), matcher.end());
            }
            matcher.appendTail(sb);
            redactedContent = sb.toString();
        }

        metadata.put("patternsChecked", patterns.size());
        metadata.put("originalLength", content.length());
        metadata.put("redactedLength", redactedContent.length());

        return RedactionResult.builder()
            .redactedContent(redactedContent)
            .redactedFields(redactedFields)
            .metadata(metadata)
            .build();
    }

    @Override
    public boolean supports(String contentType) {
        // PII patterns work on any text content
        return true;
    }

    @Override
    public int getPriority() {
        return 50; // Run after JSON-specific filters
    }

    private List<PiiPattern> buildDefaultPatterns() {
        return List.of(
            // UK National Insurance Number
            new PiiPattern(
                "uk-nino",
                "NATIONAL_ID",
                Pattern.compile("\\b[A-CEGHJ-PR-TW-Z]{2}\\s?\\d{2}\\s?\\d{2}\\s?\\d{2}\\s?[A-D]\\b", Pattern.CASE_INSENSITIVE),
                "[NINO:redacted]"
            ),

            // UK Sort Code
            new PiiPattern(
                "uk-sort-code",
                "BANK_IDENTIFIER",
                Pattern.compile("\\b\\d{2}-\\d{2}-\\d{2}\\b"),
                "[SORT:**-**-**]"
            ),

            // UK Bank Account Number (8 digits)
            new PiiPattern(
                "uk-account-number",
                "BANK_ACCOUNT",
                Pattern.compile("\\b\\d{8}\\b"),
                "[ACCT:********]"
            ),

            // Credit/Debit Card Number (basic pattern)
            new PiiPattern(
                "card-number",
                "PAYMENT_CARD",
                Pattern.compile("\\b(?:\\d{4}[- ]?){3}\\d{4}\\b"),
                "[CARD:****-****-****-****]"
            ),

            // UK Phone Number
            new PiiPattern(
                "uk-phone",
                "PHONE",
                Pattern.compile("\\b(?:(?:\\+44\\s?|0)(?:7\\d{3}|\\d{4}))\\s?\\d{3}\\s?\\d{3}\\b"),
                "[PHONE:redacted]"
            ),

            // Email Address
            new PiiPattern(
                "email",
                "EMAIL",
                Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"),
                "[EMAIL:redacted]"
            ),

            // UK Postcode
            new PiiPattern(
                "uk-postcode",
                "ADDRESS",
                Pattern.compile("\\b[A-Z]{1,2}\\d[A-Z\\d]?\\s*\\d[A-Z]{2}\\b", Pattern.CASE_INSENSITIVE),
                "[POST:redacted]"
            ),

            // Date of Birth patterns (common formats)
            new PiiPattern(
                "dob-dmy",
                "DATE_OF_BIRTH",
                Pattern.compile("\\b(?:0?[1-9]|[12]\\d|3[01])[/-](?:0?[1-9]|1[0-2])[/-](?:19|20)\\d{2}\\b"),
                "[DOB:**/**/****]"
            ),

            // IBAN (International Bank Account Number)
            new PiiPattern(
                "iban",
                "BANK_ACCOUNT",
                Pattern.compile("\\b[A-Z]{2}\\d{2}[A-Z0-9]{4}\\d{7}(?:[A-Z0-9]?){0,16}\\b", Pattern.CASE_INSENSITIVE),
                "[IBAN:redacted]"
            ),

            // BIC/SWIFT Code
            new PiiPattern(
                "bic-swift",
                "BANK_IDENTIFIER",
                Pattern.compile("\\b[A-Z]{6}[A-Z0-9]{2}(?:[A-Z0-9]{3})?\\b"),
                "[BIC:redacted]"
            )
        );
    }

    public record PiiPattern(
        String name,
        String fieldType,
        Pattern pattern,
        String replacement
    ) {}
}
