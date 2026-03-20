package com.regulus.platform.privacy.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.regulus.platform.privacy.model.RedactionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Privacy filter that redacts sensitive fields using JSONPath expressions.
 * Supports configurable paths and replacement strategies.
 */
public class JsonPathRedactionFilter implements PrivacyFilter {

    private static final Logger log = LoggerFactory.getLogger(JsonPathRedactionFilter.class);

    private final List<RedactionRule> rules;
    private final ObjectMapper objectMapper;
    private final Configuration jsonPathConfig;

    public JsonPathRedactionFilter(List<RedactionRule> rules) {
        this.rules = new ArrayList<>(rules);
        this.objectMapper = new ObjectMapper();
        this.jsonPathConfig = Configuration.defaultConfiguration()
            .addOptions(Option.SUPPRESS_EXCEPTIONS);
    }

    @Override
    public String getName() {
        return "jsonpath-redaction";
    }

    @Override
    public RedactionResult redact(String content) {
        if (content == null || content.isBlank()) {
            return RedactionResult.unchanged(content);
        }

        try {
            JsonNode rootNode = objectMapper.readTree(content);
            List<RedactionResult.RedactedField> redactedFields = new ArrayList<>();
            Map<String, Object> metadata = new HashMap<>();

            for (RedactionRule rule : rules) {
                try {
                    Object result = JsonPath.using(jsonPathConfig)
                        .parse(content)
                        .read(rule.jsonPath());

                    if (result != null) {
                        rootNode = applyRedaction(rootNode, rule, redactedFields);
                    }
                } catch (PathNotFoundException e) {
                    // Path not found, nothing to redact
                    log.trace("Path '{}' not found in content", rule.jsonPath());
                }
            }

            String redactedContent = objectMapper.writeValueAsString(rootNode);

            metadata.put("originalLength", content.length());
            metadata.put("redactedLength", redactedContent.length());
            metadata.put("rulesApplied", rules.size());

            return RedactionResult.builder()
                .redactedContent(redactedContent)
                .redactedFields(redactedFields)
                .metadata(metadata)
                .build();

        } catch (JsonProcessingException e) {
            log.warn("Failed to parse content as JSON, returning unchanged", e);
            return RedactionResult.unchanged(content);
        }
    }

    @Override
    public boolean supports(String contentType) {
        return contentType != null &&
            (contentType.contains("json") || contentType.contains("application/json"));
    }

    @Override
    public int getPriority() {
        return 100;
    }

    private JsonNode applyRedaction(JsonNode rootNode, RedactionRule rule,
                                    List<RedactionResult.RedactedField> redactedFields) {
        String[] pathParts = parseJsonPath(rule.jsonPath());
        return redactPath(rootNode, pathParts, 0, rule, redactedFields);
    }

    private JsonNode redactPath(JsonNode node, String[] pathParts, int index,
                                RedactionRule rule, List<RedactionResult.RedactedField> redactedFields) {
        if (index >= pathParts.length || node == null) {
            return node;
        }

        String part = pathParts[index];

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;

            if (index == pathParts.length - 1) {
                // Final path part - apply redaction
                if (objectNode.has(part)) {
                    String replacement = generateReplacement(rule);
                    objectNode.set(part, new TextNode(replacement));

                    redactedFields.add(new RedactionResult.RedactedField(
                        rule.jsonPath(),
                        rule.fieldType(),
                        replacement
                    ));

                    log.debug("Redacted field at path '{}' with type '{}'",
                        rule.jsonPath(), rule.fieldType());
                }
            } else if (objectNode.has(part)) {
                // Recurse into nested object
                JsonNode child = redactPath(objectNode.get(part), pathParts, index + 1, rule, redactedFields);
                objectNode.set(part, child);
            }
        }

        return node;
    }

    private String[] parseJsonPath(String jsonPath) {
        // Simple parsing: remove $ prefix and split by .
        String path = jsonPath.startsWith("$.") ? jsonPath.substring(2) : jsonPath;
        return path.split("\\.");
    }

    private String generateReplacement(RedactionRule rule) {
        return switch (rule.replacementStrategy()) {
            case MASK -> "[REDACTED]";
            case HASH -> "[HASH:" + UUID.randomUUID().toString().substring(0, 8) + "]";
            case TOKEN -> "[TOKEN:" + rule.fieldType().toUpperCase() + "]";
            case EMPTY -> "";
            case FIXED -> rule.fixedReplacement() != null ? rule.fixedReplacement() : "[REDACTED]";
        };
    }

    public record RedactionRule(
        String jsonPath,
        String fieldType,
        ReplacementStrategy replacementStrategy,
        String fixedReplacement
    ) {
        public static RedactionRule of(String jsonPath, String fieldType) {
            return new RedactionRule(jsonPath, fieldType, ReplacementStrategy.MASK, null);
        }

        public static RedactionRule of(String jsonPath, String fieldType, ReplacementStrategy strategy) {
            return new RedactionRule(jsonPath, fieldType, strategy, null);
        }
    }

    public enum ReplacementStrategy {
        MASK,       // Replace with [REDACTED]
        HASH,       // Replace with hashed token
        TOKEN,      // Replace with typed token [TOKEN:TYPE]
        EMPTY,      // Replace with empty string
        FIXED       // Replace with fixed value
    }
}
