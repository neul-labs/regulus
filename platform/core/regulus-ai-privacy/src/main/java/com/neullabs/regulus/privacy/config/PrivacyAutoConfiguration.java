package com.neullabs.regulus.privacy.config;

import com.neullabs.regulus.privacy.filter.JsonPathRedactionFilter;
import com.neullabs.regulus.privacy.filter.PiiPatternFilter;
import com.neullabs.regulus.privacy.filter.PrivacyFilter;
import com.neullabs.regulus.privacy.filter.PrivacyFilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-configuration for privacy filtering components.
 */
@AutoConfiguration
@EnableConfigurationProperties(PrivacyProperties.class)
@ConditionalOnProperty(name = "regulus.ai.privacy.enabled", havingValue = "true", matchIfMissing = true)
public class PrivacyAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PrivacyAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public JsonPathRedactionFilter jsonPathRedactionFilter(PrivacyProperties properties) {
        List<JsonPathRedactionFilter.RedactionRule> rules = new ArrayList<>();

        for (PrivacyProperties.RedactionPath path : properties.getRedact()) {
            JsonPathRedactionFilter.ReplacementStrategy strategy =
                parseStrategy(path.getReplacement());

            rules.add(new JsonPathRedactionFilter.RedactionRule(
                path.getPath(),
                path.getFieldType(),
                strategy,
                null
            ));
        }

        // Add common sensitive field paths by default
        if (rules.isEmpty()) {
            rules.addAll(getDefaultRedactionRules());
        }

        log.info("Creating JsonPathRedactionFilter with {} rules", rules.size());
        return new JsonPathRedactionFilter(rules);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "regulus.ai.privacy.pii-detection-enabled", havingValue = "true", matchIfMissing = true)
    public PiiPatternFilter piiPatternFilter() {
        log.info("Creating PII Pattern Filter with default UK financial patterns");
        return new PiiPatternFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public PrivacyFilterChain privacyFilterChain(List<PrivacyFilter> filters) {
        log.info("Creating PrivacyFilterChain with {} filters", filters.size());
        return new PrivacyFilterChain(filters);
    }

    private JsonPathRedactionFilter.ReplacementStrategy parseStrategy(String strategy) {
        try {
            return JsonPathRedactionFilter.ReplacementStrategy.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            return JsonPathRedactionFilter.ReplacementStrategy.MASK;
        }
    }

    private List<JsonPathRedactionFilter.RedactionRule> getDefaultRedactionRules() {
        return List.of(
            JsonPathRedactionFilter.RedactionRule.of("$.customer.nationalId", "NATIONAL_ID"),
            JsonPathRedactionFilter.RedactionRule.of("$.customer.dateOfBirth", "DATE_OF_BIRTH"),
            JsonPathRedactionFilter.RedactionRule.of("$.customer.email", "EMAIL"),
            JsonPathRedactionFilter.RedactionRule.of("$.customer.phone", "PHONE"),
            JsonPathRedactionFilter.RedactionRule.of("$.customer.address", "ADDRESS"),
            JsonPathRedactionFilter.RedactionRule.of("$.account.number", "BANK_ACCOUNT"),
            JsonPathRedactionFilter.RedactionRule.of("$.account.sortCode", "BANK_IDENTIFIER"),
            JsonPathRedactionFilter.RedactionRule.of("$.payment.cardNumber", "PAYMENT_CARD"),
            JsonPathRedactionFilter.RedactionRule.of("$.payment.cvv", "PAYMENT_CARD"),
            JsonPathRedactionFilter.RedactionRule.of("$.beneficiary.iban", "BANK_ACCOUNT")
        );
    }
}
