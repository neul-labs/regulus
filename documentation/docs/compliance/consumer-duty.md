# Consumer Duty

FCA Consumer Duty (PS22/9) implementation guide.

## Overview

The FCA Consumer Duty requires firms to act to deliver good outcomes for retail customers. Regulus helps meet these requirements through built-in features that support the four customer outcomes.

## Four Customer Outcomes

### 1. Products and Services

**Requirement**: Products and services are designed to meet the needs of consumers.

**Regulus Implementation**:

```yaml
regulus:
  consumer-duty:
    products-services:
      target-market-validation: true
      suitability-checks: true
```

```java
@Service
public class SuitabilityService {

    private final PolicyGuard policyGuard;

    public Mono<SuitabilityResult> checkSuitability(
            String productId,
            CustomerProfile customer) {

        return policyGuard.enforce(PolicyContext.builder()
            .purposeCode("SUITABILITY_CHECK")
            .customerId(customer.id())
            .build())
            .then(performSuitabilityCheck(productId, customer));
    }

    private Mono<SuitabilityResult> performSuitabilityCheck(
            String productId,
            CustomerProfile customer) {

        Product product = productService.getProduct(productId);

        // Check target market alignment
        if (!product.targetMarket().matches(customer)) {
            return Mono.just(SuitabilityResult.notSuitable(
                "Customer outside target market"
            ));
        }

        // Additional checks...
        return Mono.just(SuitabilityResult.suitable());
    }
}
```

### 2. Price and Value

**Requirement**: Products and services provide fair value.

**Regulus Implementation**:

```java
@Service
public class FairValueAssessmentService {

    public FairValueAssessment assess(String productId) {
        Product product = productService.getProduct(productId);

        return FairValueAssessment.builder()
            .productId(productId)
            .totalCost(calculateTotalCost(product))
            .benefitsProvided(enumerateBenefits(product))
            .limitationsAndExclusions(enumerateLimitations(product))
            .comparisonToAlternatives(compareAlternatives(product))
            .valueAssessment(determineValue(product))
            .build();
    }
}
```

Audit logging for pricing decisions:

```java
public Mono<Quote> generateQuote(QuoteRequest request) {
    return calculateQuote(request)
        .doOnSuccess(quote -> auditLogger.log(AuditEvent.builder()
            .type("PRICING_DECISION")
            .details(Map.of(
                "customerId", request.customerId(),
                "productId", request.productId(),
                "quotedPrice", quote.price(),
                "pricingFactors", quote.factors()
            ))
            .build()));
}
```

### 3. Consumer Understanding

**Requirement**: Communications enable consumers to make informed decisions.

**Regulus Implementation**:

```java
@Service
public class CommunicationService {

    private final LlmClient llmClient;
    private final ReadabilityChecker readabilityChecker;

    public Mono<Communication> generateCommunication(
            CommunicationRequest request) {

        String systemPrompt = """
            Generate clear, jargon-free communication for a bank customer.
            Use plain English at a reading level suitable for the general public.
            Avoid technical terms or explain them when necessary.
            Be concise and highlight key information.
            """;

        return llmClient.chat(List.of(
            ChatMessage.system(systemPrompt),
            ChatMessage.user(request.topic())
        ))
        .map(response -> {
            String content = response.content();

            // Validate readability
            ReadabilityScore score = readabilityChecker.check(content);
            if (score.gradeLevel() > 8) {  // Target: 8th grade or below
                log.warn("Communication exceeds target reading level: {}",
                    score.gradeLevel());
            }

            return Communication.builder()
                .content(content)
                .readabilityScore(score)
                .keyPoints(extractKeyPoints(content))
                .build();
        });
    }
}
```

Configuration for communication standards:

```yaml
regulus:
  consumer-duty:
    consumer-understanding:
      max-reading-level: 8  # Grade level
      require-key-points: true
      jargon-detection: true
      banned-phrases:
        - "subject to terms and conditions"
        - "as per our policy"
```

### 4. Consumer Support

**Requirement**: Firms provide support that meets consumers' needs.

**Regulus Implementation**:

```java
@Service
public class CustomerSupportAgent {

    private final LlmClient llmClient;
    private final EscalationService escalationService;

    public Flux<SupportResponse> handleQuery(SupportRequest request) {
        // Track support interaction
        SupportMetrics.recordInteraction(request);

        return processQuery(request)
            .timeout(Duration.ofSeconds(30))
            .onErrorResume(TimeoutException.class, e -> {
                // Escalate if AI can't respond quickly
                return escalationService.escalateToHuman(request)
                    .map(ticket -> SupportResponse.escalated(ticket));
            });
    }

    private Flux<SupportResponse> processQuery(SupportRequest request) {
        // Detect vulnerable customers
        if (vulnerabilityDetector.detect(request)) {
            return escalationService.escalateToSpecialist(request)
                .map(ticket -> SupportResponse.escalatedVulnerable(ticket));
        }

        // Process with AI
        return llmClient.streamChat(request.query())
            .map(SupportResponse::fromChunk);
    }
}
```

Support quality monitoring:

```yaml
regulus:
  consumer-duty:
    consumer-support:
      max-response-time: 30s
      escalation-threshold: 3  # Max AI attempts before human
      vulnerability-detection: true
      satisfaction-tracking: true
```

## Vulnerability Detection

```java
@Component
public class VulnerabilityDetector {

    private final List<String> vulnerabilityIndicators = List.of(
        "confused", "don't understand", "struggling",
        "bereaved", "illness", "disability"
    );

    public boolean detect(SupportRequest request) {
        String text = request.query().toLowerCase();

        // Check for indicators
        if (vulnerabilityIndicators.stream().anyMatch(text::contains)) {
            return true;
        }

        // Check customer flags
        CustomerProfile profile = customerService.getProfile(request.customerId());
        return profile.hasVulnerabilityFlag();
    }
}
```

## Outcome Monitoring

### Metrics Dashboard

```yaml
# Prometheus metrics for Consumer Duty
regulus.consumer_duty.suitability.checks.total
regulus.consumer_duty.suitability.rejections.total
regulus.consumer_duty.communications.readability_score
regulus.consumer_duty.support.response_time
regulus.consumer_duty.support.escalations.total
regulus.consumer_duty.support.satisfaction_score
regulus.consumer_duty.vulnerability.detections.total
```

### Outcome Reporting

```java
@Service
public class ConsumerDutyReportService {

    public ConsumerDutyReport generateReport(DateRange period) {
        return ConsumerDutyReport.builder()
            .period(period)

            // Outcome 1: Products and Services
            .suitabilityChecksPassed(getSuitabilityMetrics(period))
            .targetMarketComplaints(getComplaints(period, "TARGET_MARKET"))

            // Outcome 2: Price and Value
            .fairValueAssessments(getFairValueMetrics(period))
            .pricingComplaints(getComplaints(period, "PRICING"))

            // Outcome 3: Consumer Understanding
            .averageReadabilityScore(getReadabilityMetrics(period))
            .communicationComplaints(getComplaints(period, "UNDERSTANDING"))

            // Outcome 4: Consumer Support
            .averageResponseTime(getSupportMetrics(period).responseTime())
            .escalationRate(getSupportMetrics(period).escalationRate())
            .satisfactionScore(getSupportMetrics(period).satisfaction())
            .vulnerableCustomersSupported(getVulnerabilityMetrics(period))

            .build();
    }
}
```

## Audit Trail

All Consumer Duty-relevant actions are logged:

```java
@Aspect
@Component
public class ConsumerDutyAuditAspect {

    @AfterReturning(
        pointcut = "@annotation(ConsumerDutyRelevant)",
        returning = "result"
    )
    public void auditConsumerDutyAction(JoinPoint jp, Object result) {
        ConsumerDutyRelevant annotation = getAnnotation(jp);

        auditLogger.log(AuditEvent.builder()
            .type("CONSUMER_DUTY_ACTION")
            .outcome(annotation.outcome())
            .action(jp.getSignature().getName())
            .details(buildDetails(jp, result))
            .build());
    }
}

// Usage
@ConsumerDutyRelevant(outcome = ConsumerOutcome.CONSUMER_UNDERSTANDING)
public Communication generateCommunication(CommunicationRequest request) {
    // ...
}
```

## Testing

### Consumer Duty Test Scenarios

```java
@SpringBootTest
class ConsumerDutyTest {

    @Test
    void shouldRejectUnsuitableProduct() {
        CustomerProfile customer = CustomerProfile.builder()
            .riskTolerance(RiskTolerance.LOW)
            .build();

        Product highRiskProduct = productService.getProduct("HIGH_RISK_FUND");

        StepVerifier.create(suitabilityService.checkSuitability(
                highRiskProduct.id(), customer))
            .assertNext(result -> {
                assertThat(result.isSuitable()).isFalse();
                assertThat(result.reason()).contains("risk tolerance");
            })
            .verifyComplete();
    }

    @Test
    void shouldEscalateVulnerableCustomer() {
        SupportRequest request = SupportRequest.builder()
            .query("I'm really confused about my account, my spouse passed away")
            .build();

        StepVerifier.create(supportAgent.handleQuery(request))
            .assertNext(response -> {
                assertThat(response.isEscalated()).isTrue();
                assertThat(response.escalationReason())
                    .contains("vulnerability");
            })
            .verifyComplete();
    }

    @Test
    void shouldGenerateReadableCommunication() {
        Communication comm = communicationService
            .generateCommunication(CommunicationRequest.builder()
                .topic("Explain overdraft fees")
                .build())
            .block();

        assertThat(comm.readabilityScore().gradeLevel())
            .isLessThanOrEqualTo(8);
    }
}
```

## Best Practices

1. **Document decisions** - Log all suitability and pricing decisions
2. **Monitor outcomes** - Track metrics for all four outcomes
3. **Test regularly** - Include Consumer Duty scenarios in testing
4. **Train models carefully** - Ensure LLM prompts align with Duty requirements
5. **Escalate appropriately** - Don't rely solely on AI for vulnerable customers
6. **Review communications** - Regular readability audits
