package com.regulus.platform.privacy.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Result of a redaction operation, containing the redacted content
 * and metadata about what was redacted.
 */
public record RedactionResult(
    String redactedContent,
    List<RedactedField> redactedFields,
    Map<String, Object> metadata
) {

    public static RedactionResult unchanged(String content) {
        return new RedactionResult(content, Collections.emptyList(), Collections.emptyMap());
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean hasRedactions() {
        return !redactedFields.isEmpty();
    }

    public int redactionCount() {
        return redactedFields.size();
    }

    public record RedactedField(
        String path,
        String fieldType,
        String replacementToken
    ) {}

    public static class Builder {
        private String redactedContent;
        private List<RedactedField> redactedFields = Collections.emptyList();
        private Map<String, Object> metadata = Collections.emptyMap();

        public Builder redactedContent(String content) {
            this.redactedContent = content;
            return this;
        }

        public Builder redactedFields(List<RedactedField> fields) {
            this.redactedFields = fields;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public RedactionResult build() {
            return new RedactionResult(redactedContent, redactedFields, metadata);
        }
    }
}
