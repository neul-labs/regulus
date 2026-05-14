package com.neullabs.regulus.privacy.filter;

import com.neullabs.regulus.privacy.model.PrivacyMetadata;
import com.neullabs.regulus.privacy.model.RedactionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates multiple privacy filters in a chain.
 * Attaches privacy metadata to processed data.
 */
public class PrivacyFilterChain {

    private static final Logger log = LoggerFactory.getLogger(PrivacyFilterChain.class);

    private final List<PrivacyFilter> filters;

    public PrivacyFilterChain(List<PrivacyFilter> filters) {
        this.filters = filters.stream()
            .sorted(Comparator.comparingInt(PrivacyFilter::getPriority).reversed())
            .toList();
        log.info("PrivacyFilterChain initialized with {} filters: {}",
            filters.size(),
            filters.stream().map(PrivacyFilter::getName).toList());
    }

    /**
     * Apply all filters to the content and attach metadata.
     */
    public FilterResult filter(String content, String contentType, PrivacyMetadata metadata) {
        log.debug("Starting privacy filter chain with {} filters, contentType={}",
            filters.size(), contentType);

        String currentContent = content;
        List<RedactionResult.RedactedField> allRedactions = new ArrayList<>();
        Map<String, Object> combinedMetadata = new HashMap<>();
        List<String> appliedFilters = new ArrayList<>();

        for (PrivacyFilter filter : filters) {
            if (!filter.supports(contentType)) {
                log.trace("Filter '{}' does not support contentType '{}'",
                    filter.getName(), contentType);
                continue;
            }

            RedactionResult result = filter.redact(currentContent);

            if (result.hasRedactions()) {
                currentContent = result.redactedContent();
                allRedactions.addAll(result.redactedFields());
                combinedMetadata.putAll(result.metadata());
                appliedFilters.add(filter.getName());

                log.debug("Filter '{}' applied {} redactions",
                    filter.getName(), result.redactionCount());
            }
        }

        // Attach privacy metadata
        combinedMetadata.put("purposeCode", metadata.getPurposeCode());
        combinedMetadata.put("lawfulBasis", metadata.getLawfulBasis());
        combinedMetadata.put("dataSubjectId", metadata.getDataSubjectId());
        combinedMetadata.put("collectionTimestamp", metadata.getCollectionTimestamp().toString());
        combinedMetadata.put("appliedFilters", appliedFilters);

        if (metadata.getRetentionExpiry() != null) {
            combinedMetadata.put("retentionExpiry", metadata.getRetentionExpiry().toString());
        }

        log.debug("Privacy filter chain complete. {} total redactions, {} filters applied",
            allRedactions.size(), appliedFilters.size());

        return new FilterResult(
            currentContent,
            allRedactions,
            combinedMetadata,
            metadata
        );
    }

    /**
     * Quick filter without full metadata (uses defaults).
     */
    public String filterContent(String content, String contentType) {
        PrivacyMetadata defaultMetadata = PrivacyMetadata.builder()
            .purposeCode("UNSPECIFIED")
            .lawfulBasis("UNSPECIFIED")
            .build();

        return filter(content, contentType, defaultMetadata).redactedContent();
    }

    public List<String> getFilterNames() {
        return filters.stream().map(PrivacyFilter::getName).toList();
    }

    public record FilterResult(
        String redactedContent,
        List<RedactionResult.RedactedField> redactedFields,
        Map<String, Object> metadata,
        PrivacyMetadata privacyMetadata
    ) {
        public boolean hasRedactions() {
            return !redactedFields.isEmpty();
        }

        public int redactionCount() {
            return redactedFields.size();
        }
    }
}
