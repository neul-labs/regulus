package com.neullabs.regulus.privacy.filter;

import com.neullabs.regulus.privacy.model.RedactionResult;

/**
 * Interface for privacy filters that redact sensitive data.
 */
public interface PrivacyFilter {

    /**
     * Filter name for identification and configuration.
     */
    String getName();

    /**
     * Redact sensitive data from the given content.
     *
     * @param content the content to filter
     * @return redaction result containing filtered content and metadata
     */
    RedactionResult redact(String content);

    /**
     * Check if this filter supports the given content type.
     */
    boolean supports(String contentType);

    /**
     * Priority for ordering filters (higher = earlier execution).
     */
    default int getPriority() {
        return 0;
    }
}
