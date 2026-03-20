# Custom Privacy Filters

Creating custom PII detection and redaction patterns.

## Overview

Regulus includes built-in patterns for common UK PII types. This guide covers adding custom patterns for organization-specific data.

## Custom Pattern Registration

### Adding Simple Patterns

```java
@Configuration
public class CustomPrivacyConfig {

    @Bean
    public PrivacyFilter customPrivacyFilter(PrivacyFilterProperties properties) {
        PrivacyFilter filter = new PrivacyFilter(properties);

        // Employee ID: EMP-123456
        filter.addPattern(
            "employee-id",
            Pattern.compile("EMP-\\d{6}"),
            "[EMPLOYEE ID REDACTED]"
        );

        // Internal reference: REF-ABC-12345678
        filter.addPattern(
            "internal-reference",
            Pattern.compile("REF-[A-Z]{3}-\\d{8}"),
            "[REFERENCE REDACTED]"
        );

        // Customer number: CUST followed by 10 digits
        filter.addPattern(
            "customer-number",
            Pattern.compile("CUST\\d{10}"),
            "[CUSTOMER NUMBER REDACTED]"
        );

        return filter;
    }
}
```

### Pattern with Validation

```java
filter.addPattern(
    "uk-driving-licence",
    Pattern.compile("[A-Z]{5}\\d{6}[A-Z]{2}\\d{3}"),
    "[DRIVING LICENCE REDACTED]",
    this::validateDrivingLicence  // Custom validator
);

private boolean validateDrivingLicence(String match) {
    // Validate check digit
    return DrivingLicenceValidator.isValid(match);
}
```

## Custom Detector Implementation

### Implementing PiiDetector

```java
@Component
public class CreditCardDetector implements PiiDetector {

    private static final Pattern CARD_PATTERN =
        Pattern.compile("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b");

    @Override
    public String getType() {
        return "credit-card";
    }

    @Override
    public List<Detection> detect(String text) {
        List<Detection> detections = new ArrayList<>();
        Matcher matcher = CARD_PATTERN.matcher(text);

        while (matcher.find()) {
            String match = matcher.group().replaceAll("[- ]", "");

            // Validate with Luhn algorithm
            if (isValidLuhn(match)) {
                detections.add(new Detection(
                    getType(),
                    matcher.start(),
                    matcher.end(),
                    getCardType(match)
                ));
            }
        }

        return detections;
    }

    @Override
    public String redact(String text, Detection detection) {
        String original = text.substring(detection.start(), detection.end());
        // Keep last 4 digits
        String lastFour = original.replaceAll("[- ]", "")
            .substring(original.length() - 4);
        return "[CARD ****" + lastFour + "]";
    }

    private boolean isValidLuhn(String number) {
        int sum = 0;
        boolean alternate = false;

        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }

            sum += digit;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }

    private String getCardType(String number) {
        if (number.startsWith("4")) return "VISA";
        if (number.startsWith("5")) return "MASTERCARD";
        if (number.startsWith("3")) return "AMEX";
        return "UNKNOWN";
    }
}
```

### Context-Aware Detector

```java
@Component
public class ContextAwareEmailDetector implements PiiDetector {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    // Allowlist for known safe domains
    private final Set<String> allowedDomains;

    @Override
    public List<Detection> detect(String text) {
        List<Detection> detections = new ArrayList<>();
        Matcher matcher = EMAIL_PATTERN.matcher(text);

        while (matcher.find()) {
            String email = matcher.group();
            String domain = email.substring(email.indexOf('@') + 1);

            // Skip corporate domains
            if (!allowedDomains.contains(domain.toLowerCase())) {
                detections.add(new Detection(
                    "email",
                    matcher.start(),
                    matcher.end()
                ));
            }
        }

        return detections;
    }
}
```

## JSON Path Redaction

### Configuring JSON Paths

```yaml
regulus:
  privacy:
    redaction:
      json-paths:
        - "$.customer.email"
        - "$.customer.phone"
        - "$.customer.address.postcode"
        - "$.payment.cardNumber"
        - "$.account.sortCode"
        - "$..nationalInsuranceNumber"  # Recursive
```

### Programmatic JSON Redaction

```java
@Component
public class JsonPrivacyFilter {

    private final PrivacyFilter textFilter;
    private final List<JsonPath> jsonPaths;
    private final ObjectMapper objectMapper;

    public JsonNode redactJson(JsonNode root) {
        // Clone to avoid modifying original
        JsonNode copy = root.deepCopy();

        // Apply JSON path redactions
        for (JsonPath path : jsonPaths) {
            redactPath(copy, path);
        }

        // Apply text redaction to all string values
        return redactAllStrings((ObjectNode) copy);
    }

    private void redactPath(JsonNode node, JsonPath path) {
        List<JsonNode> matches = path.read(node);
        for (JsonNode match : matches) {
            if (match.isTextual()) {
                // Replace with redacted value
                replaceNode(node, path, "[REDACTED]");
            }
        }
    }

    private JsonNode redactAllStrings(ObjectNode node) {
        node.fields().forEachRemaining(entry -> {
            if (entry.getValue().isTextual()) {
                String redacted = textFilter.redact(entry.getValue().asText());
                node.put(entry.getKey(), redacted);
            } else if (entry.getValue().isObject()) {
                redactAllStrings((ObjectNode) entry.getValue());
            } else if (entry.getValue().isArray()) {
                redactArray((ArrayNode) entry.getValue());
            }
        });
        return node;
    }
}
```

## Structured Data Redaction

### CSV Redaction

```java
@Component
public class CsvPrivacyFilter {

    private final PrivacyFilter textFilter;
    private final Set<String> sensitiveColumns;

    public String redactCsv(String csv) {
        List<String[]> rows = parseCsv(csv);
        if (rows.isEmpty()) return csv;

        String[] headers = rows.get(0);
        int[] sensitiveIndices = findSensitiveIndices(headers);

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);

            if (i == 0) {
                // Keep headers
                result.append(String.join(",", row)).append("\n");
                continue;
            }

            // Redact sensitive columns
            for (int idx : sensitiveIndices) {
                if (idx < row.length) {
                    row[idx] = textFilter.redact(row[idx]);
                }
            }

            result.append(String.join(",", row)).append("\n");
        }

        return result.toString();
    }
}
```

### XML Redaction

```java
@Component
public class XmlPrivacyFilter {

    private final PrivacyFilter textFilter;
    private final Set<String> sensitiveElements;

    public String redactXml(String xml) throws Exception {
        Document doc = parseXml(xml);
        redactElements(doc.getDocumentElement());
        return serializeXml(doc);
    }

    private void redactElements(Element element) {
        // Check if this element is sensitive
        if (sensitiveElements.contains(element.getTagName())) {
            String content = element.getTextContent();
            element.setTextContent(textFilter.redact(content));
        }

        // Process child elements
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                redactElements((Element) child);
            }
        }
    }
}
```

## Audit Integration

```java
@Component
public class AuditedPrivacyFilter {

    private final PrivacyFilter delegate;
    private final AuditLogger auditLogger;

    public RedactionResult redactWithAudit(String text, String sessionId) {
        RedactionResult result = delegate.redactWithDetails(text);

        // Log all detections
        result.detections().forEach(detection -> {
            auditLogger.log(AuditEvent.builder()
                .type("PII_DETECTION")
                .sessionId(sessionId)
                .details(Map.of(
                    "piiType", detection.type(),
                    "position", detection.position()
                    // Never log actual PII
                ))
                .build());
        });

        return result;
    }
}
```

## Performance Optimization

### Compiled Pattern Cache

```java
@Component
public class OptimizedPrivacyFilter {

    // Pre-compile all patterns at startup
    private final List<CompiledPattern> patterns;

    @PostConstruct
    public void init() {
        // Warm up the regex cache
        String warmupText = generateWarmupText();
        detect(warmupText);
    }

    public List<Detection> detect(String text) {
        // Use parallel stream for large texts
        if (text.length() > 10000) {
            return patterns.parallelStream()
                .flatMap(p -> p.detect(text).stream())
                .collect(Collectors.toList());
        }

        return patterns.stream()
            .flatMap(p -> p.detect(text).stream())
            .collect(Collectors.toList());
    }
}
```

### Incremental Processing

```java
public String redactStream(InputStream input) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    StringBuilder result = new StringBuilder();
    String line;

    while ((line = reader.readLine()) != null) {
        result.append(privacyFilter.redact(line)).append("\n");
    }

    return result.toString();
}
```

## Testing

```java
@SpringBootTest
class CustomPrivacyFilterTest {

    @Autowired
    private PrivacyFilter privacyFilter;

    @Test
    void shouldDetectEmployeeId() {
        String input = "Contact EMP-123456 for help";
        String result = privacyFilter.redact(input);

        assertThat(result).doesNotContain("EMP-123456");
        assertThat(result).contains("[EMPLOYEE ID REDACTED]");
    }

    @Test
    void shouldMaskCreditCardWithLastFour() {
        String input = "Card: 4111-1111-1111-1234";
        String result = privacyFilter.redact(input);

        assertThat(result).contains("[CARD ****1234]");
    }

    @Test
    void shouldNotRedactAllowedDomains() {
        String input = "Email: john@ourbank.com";
        String result = privacyFilter.redact(input);

        assertThat(result).isEqualTo(input); // Not redacted
    }

    @ParameterizedTest
    @CsvSource({
        "AB123456C, [NINO REDACTED]",
        "12-34-56, [REDACTED]",
        "07700900123, [REDACTED]"
    })
    void shouldRedactKnownPatterns(String input, String expected) {
        String result = privacyFilter.redact("Value: " + input);
        assertThat(result).contains(expected);
    }
}
```

## Best Practices

1. **Validate patterns** - Test patterns against edge cases
2. **Use allowlists** - Prevent over-redaction
3. **Log detections** - Audit trail without PII
4. **Benchmark performance** - Patterns can be expensive
5. **Keep last N** - For identifiers, keep last 4 chars
6. **Handle encoding** - Consider Unicode and special chars
