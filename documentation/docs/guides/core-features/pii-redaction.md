# PII Redaction

Automatic detection and redaction of Personally Identifiable Information (PII) to protect customer data and ensure GDPR compliance.

## Overview

The Privacy Filter automatically detects and masks sensitive information in both inputs and outputs, including:

- National Insurance Numbers (NINO)
- UK sort codes and account numbers
- Credit/debit card numbers
- Phone numbers (UK format)
- Email addresses
- UK postcodes
- IBAN and BIC codes

## Configuration

### Basic Configuration

```yaml title="application.yml"
regulus:
  privacy:
    redaction:
      enabled: true
      replacement: "[REDACTED]"
```

### Full Configuration

```yaml title="application.yml"
regulus:
  privacy:
    redaction:
      enabled: true
      replacement: "[REDACTED]"

      # Enable/disable specific patterns
      patterns:
        nino: true
        sort-code: true
        account-number: true
        credit-card: true
        phone-uk: true
        email: true
        postcode: true
        iban: true
        bic: true

      # Custom replacements per type
      replacements:
        nino: "[NINO REDACTED]"
        credit-card: "[CARD REDACTED]"
        email: "[EMAIL REDACTED]"

      # JSONPath for structured data
      json-paths:
        - "$.customer.nino"
        - "$.account.sortCode"
        - "$.payment.cardNumber"

      # Audit redactions
      audit:
        enabled: true
        log-pattern-matches: true
```

## Usage

### Basic Usage

```java
@Service
public class AgentService {

    private final PrivacyFilter privacyFilter;
    private final LlmClient llmClient;

    public Mono<String> process(String userInput) {
        // Redact PII from input
        String sanitizedInput = privacyFilter.redact(userInput);

        return llmClient.chat(sanitizedInput)
            .map(response -> {
                // Redact PII from output
                return privacyFilter.redact(response.content());
            });
    }
}
```

### With Detection Results

```java
public Mono<ProcessedMessage> processWithDetection(String input) {
    RedactionResult result = privacyFilter.redactWithDetails(input);

    // Log what was found (without the actual values)
    result.detections().forEach(detection -> {
        log.info("Detected {} at position {}",
            detection.type(),
            detection.position());
    });

    return llmClient.chat(result.redactedText())
        .map(response -> new ProcessedMessage(
            privacyFilter.redact(response.content()),
            result.detections().size()
        ));
}
```

## Supported Patterns

### National Insurance Number (NINO)

Format: `AB123456C`

```
Input:  "My NINO is AB123456C"
Output: "My NINO is [NINO REDACTED]"
```

### Sort Code

Format: `12-34-56` or `123456`

```
Input:  "Sort code: 12-34-56"
Output: "Sort code: [REDACTED]"
```

### Account Number

Format: 8 digits

```
Input:  "Account: 12345678"
Output: "Account: [REDACTED]"
```

### Credit Card Number

Format: 16 digits (various groupings)

```
Input:  "Card: 4111-1111-1111-1111"
Output: "Card: [CARD REDACTED]"
```

### UK Phone Number

Formats: `07xxx`, `+44 7xxx`, `0207`, etc.

```
Input:  "Call me on 07700 900123"
Output: "Call me on [REDACTED]"
```

### Email Address

```
Input:  "Email: john.smith@example.com"
Output: "Email: [EMAIL REDACTED]"
```

### UK Postcode

Format: `SW1A 1AA`, `EC1A 1BB`, etc.

```
Input:  "Address: London, SW1A 1AA"
Output: "Address: London, [REDACTED]"
```

### IBAN

```
Input:  "IBAN: GB82WEST12345698765432"
Output: "IBAN: [REDACTED]"
```

### BIC/SWIFT Code

```
Input:  "BIC: WESTGB2L"
Output: "BIC: [REDACTED]"
```

## Custom Patterns

### Adding Custom Patterns

```java
@Configuration
public class PrivacyConfig {

    @Bean
    public PrivacyFilter customPrivacyFilter(
            PrivacyFilterProperties properties) {

        PrivacyFilter filter = new PrivacyFilter(properties);

        // Add custom pattern for internal employee IDs
        filter.addPattern(
            "employee-id",
            Pattern.compile("EMP-\\d{6}"),
            "[EMPLOYEE ID REDACTED]"
        );

        // Add custom pattern for internal reference numbers
        filter.addPattern(
            "reference",
            Pattern.compile("REF-[A-Z]{3}-\\d{8}"),
            "[REFERENCE REDACTED]"
        );

        return filter;
    }
}
```

### Pattern with Validation

```java
@Component
public class ValidatedPatternDetector implements PiiDetector {

    private final Pattern pattern = Pattern.compile("\\b\\d{8}\\b");

    @Override
    public List<Detection> detect(String text) {
        List<Detection> detections = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String match = matcher.group();
            // Only match if it passes Luhn check (for account numbers)
            if (isValidAccountNumber(match)) {
                detections.add(new Detection(
                    "account-number",
                    matcher.start(),
                    matcher.end()
                ));
            }
        }
        return detections;
    }
}
```

## Structured Data Redaction

### JSON Redaction

```java
public String redactJson(String json) {
    JsonNode node = objectMapper.readTree(json);
    JsonNode redacted = privacyFilter.redactJson(node);
    return objectMapper.writeValueAsString(redacted);
}
```

### With JSONPath Configuration

```yaml
regulus:
  privacy:
    redaction:
      json-paths:
        - "$.customer.nationalInsuranceNumber"
        - "$.customer.contact.email"
        - "$.customer.contact.phone"
        - "$.payment.cardDetails.number"
        - "$.payment.cardDetails.cvv"
        - "$.account.sortCode"
        - "$.account.accountNumber"
```

### Deep JSON Redaction

```java
@Service
public class JsonRedactionService {

    private final PrivacyFilter privacyFilter;
    private final ObjectMapper objectMapper;

    public String redactDeep(String json) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode redacted = redactNode(root);
        return objectMapper.writeValueAsString(redacted);
    }

    private JsonNode redactNode(JsonNode node) {
        if (node.isTextual()) {
            return new TextNode(privacyFilter.redact(node.asText()));
        }
        if (node.isObject()) {
            ObjectNode obj = objectMapper.createObjectNode();
            node.fields().forEachRemaining(entry ->
                obj.set(entry.getKey(), redactNode(entry.getValue()))
            );
            return obj;
        }
        if (node.isArray()) {
            ArrayNode arr = objectMapper.createArrayNode();
            node.forEach(element -> arr.add(redactNode(element)));
            return arr;
        }
        return node;
    }
}
```

## Audit Logging

All redactions can be logged for compliance:

```java
@Component
public class RedactionAuditLogger {

    private final AuditLogger auditLogger;

    public void logRedaction(
            String sessionId,
            List<Detection> detections) {

        detections.forEach(detection -> {
            auditLogger.log(AuditEvent.builder()
                .type("PII_REDACTION")
                .sessionId(sessionId)
                .details(Map.of(
                    "piiType", detection.type(),
                    "position", detection.position()
                    // Never log the actual PII value
                ))
                .build());
        });
    }
}
```

## Testing

```java
@SpringBootTest
class PrivacyFilterTest {

    @Autowired
    private PrivacyFilter privacyFilter;

    @Test
    void shouldRedactNino() {
        String input = "My NINO is AB123456C";
        String result = privacyFilter.redact(input);
        assertThat(result).doesNotContain("AB123456C");
        assertThat(result).contains("[NINO REDACTED]");
    }

    @Test
    void shouldRedactMultiplePii() {
        String input = "NINO: AB123456C, Phone: 07700900123";
        String result = privacyFilter.redact(input);
        assertThat(result).doesNotContain("AB123456C");
        assertThat(result).doesNotContain("07700900123");
    }

    @Test
    void shouldPreserveNonPiiText() {
        String input = "Hello, how can I help?";
        String result = privacyFilter.redact(input);
        assertThat(result).isEqualTo(input);
    }

    @Test
    void shouldDetectPiiTypes() {
        String input = "Email: test@example.com, Card: 4111111111111111";
        RedactionResult result = privacyFilter.redactWithDetails(input);

        assertThat(result.detections())
            .extracting(Detection::type)
            .containsExactlyInAnyOrder("email", "credit-card");
    }
}
```

## Performance Considerations

1. **Compile patterns once** - Patterns are compiled at startup
2. **Use appropriate scope** - Apply redaction only where needed
3. **Consider caching** - For repeated text processing
4. **Monitor latency** - Track redaction time in metrics

```yaml
regulus:
  privacy:
    redaction:
      # Performance tuning
      max-text-length: 100000  # Limit processing for very large texts
      parallel-threshold: 10000  # Use parallel processing above this size
```

## Metrics

Available metrics:

- `regulus.privacy.redactions.total` - Total redactions by type
- `regulus.privacy.detections.total` - Total PII detections
- `regulus.privacy.latency` - Redaction processing time

```promql
# Redaction rate by type
rate(regulus_privacy_redactions_total[5m])

# Most common PII types detected
topk(5, sum by (pii_type) (regulus_privacy_detections_total))
```
