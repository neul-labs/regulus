# Consumer Duty Implementation Guide

Complete guide to implementing [FCA Consumer Duty](https://www.fca.org.uk/firms/consumer-duty) requirements in AI agents using Regulus.

---

## Overview

The Consumer Duty (FG22/5, PS22/9) requires firms to deliver good outcomes for retail customers. For AI agents, this means:

- **Products must be suitable** for the target market
- **Communications must be clear** and understandable
- **Customers must be supported** throughout their journey
- **Prices must represent fair value**

---

## The Four Outcomes

### 1. Products and Services

**FCA Requirement**: Products and services are designed to meet the needs of customers in the target market.

#### Regulus Controls

```java
@Tool(name = "recommend_product")
@RequireSuitabilityCheck
@TargetMarketValidation
public ProductRecommendation recommendProduct(
    CustomerProfile customer,
    ProductType productType
) {
    // Validate customer is in target market
    if (!targetMarketValidator.isInTargetMarket(customer, productType)) {
        throw new TargetMarketException(
            "Customer not in target market for " + productType
        );
    }

    // Perform suitability assessment
    SuitabilityAssessment assessment = suitabilityChecker.assess(
        customer,
        productType
    );

    if (!assessment.isSuitable()) {
        return ProductRecommendation.unsuitable(assessment.getReasons());
    }

    return ProductRecommendation.suitable(
        selectProduct(customer, productType),
        assessment
    );
}
```

#### Configuration

```yaml
regulus:
  ai:
    governance:
      consumer-duty:
        products-and-services:
          enabled: true
          suitability-check-required: true
          target-market-validation: true
          eligibility-rules:
            - rule: AGE_CHECK
              min-age: 18
            - rule: RESIDENCY_CHECK
              allowed: [UK]
            - rule: RISK_PROFILE_MATCH
              enabled: true
```

#### Evidence

| Evidence Type | Source | Retention |
|---------------|--------|-----------|
| Suitability assessments | `SuitabilityChecker.assess()` | 7 years |
| Target market validation | `TargetMarketValidator` logs | 7 years |
| Product recommendations | Agent decision trace | 7 years |
| Declined recommendations | Audit trail | 7 years |

---

### 2. Price and Value

**FCA Requirement**: Products and services provide fair value to customers.

#### Regulus Controls

```java
@Tool(name = "quote_product")
@RequireValueAssessment
@FeeTransparency
public ProductQuote quoteProduct(
    CustomerProfile customer,
    ProductType productType
) {
    ProductQuote quote = pricingEngine.generateQuote(customer, productType);

    // Ensure fee transparency
    FeeBreakdown fees = quote.getFeeBreakdown();
    if (!fees.isFullyDisclosed()) {
        throw new FeeTransparencyException("All fees must be disclosed");
    }

    // Log for value assessment evidence
    auditLogger.logQuote(quote, customer, "CONSUMER_DUTY_VALUE");

    return quote.withConsumerDutyDisclosure(
        generateValueStatement(quote)
    );
}
```

#### Configuration

```yaml
regulus:
  ai:
    governance:
      consumer-duty:
        price-and-value:
          enabled: true
          fee-transparency-required: true
          value-assessment:
            enabled: true
            benchmark-comparison: true
          fee-disclosure:
            format: ITEMISED
            timing: UPFRONT
```

#### Fee Disclosure Template

```java
public record FeeDisclosure(
    BigDecimal totalCost,
    List<FeeItem> breakdown,
    String valueStatement,
    Instant disclosedAt
) {
    public static FeeDisclosure forMortgage(MortgageQuote quote) {
        return new FeeDisclosure(
            quote.getTotalCost(),
            List.of(
                new FeeItem("Arrangement fee", quote.getArrangementFee()),
                new FeeItem("Valuation fee", quote.getValuationFee()),
                new FeeItem("Legal fees (estimate)", quote.getLegalFeesEstimate()),
                new FeeItem("Interest over term", quote.getTotalInterest())
            ),
            "This mortgage has an APR of " + quote.getApr() + "%. " +
            "The total amount payable is £" + quote.getTotalPayable(),
            Instant.now()
        );
    }
}
```

---

### 3. Consumer Understanding

**FCA Requirement**: Communications enable customers to make effective, timely, and informed decisions.

#### Regulus Controls

```java
@Tool(name = "explain_product")
@RequireExplanation(level = ExplanationLevel.HIGH)
@PlainLanguageRequired
public ProductExplanation explainProduct(
    ProductType productType,
    CustomerProfile customer
) {
    // Generate explanation using LLM
    String explanation = llmClient.chat(
        buildExplanationPrompt(productType, customer)
    );

    // Validate plain language
    PlainLanguageScore score = plainLanguageValidator.assess(explanation);
    if (score.getReadabilityScore() < 60) { // Flesch reading ease
        explanation = simplifyExplanation(explanation);
    }

    // Check for jargon
    List<String> jargonTerms = jargonDetector.findJargon(explanation);
    if (!jargonTerms.isEmpty()) {
        explanation = replaceJargon(explanation, jargonTerms);
    }

    return new ProductExplanation(
        explanation,
        score,
        customer.getPreferredCommunicationStyle()
    );
}
```

#### Configuration

```yaml
regulus:
  ai:
    governance:
      consumer-duty:
        consumer-understanding:
          enabled: true
          plain-language:
            enabled: true
            min-readability-score: 60  # Flesch reading ease
            max-sentence-length: 25
            jargon-replacement: true
          explanation:
            detail-level: HIGH
            include-risks: true
            include-alternatives: true
          communication-preferences:
            respect-customer-preference: true
            formats: [WRITTEN, VERBAL_SUMMARY, VIDEO]
```

#### Plain Language Validation

```java
public class PlainLanguageValidator {

    private static final int MIN_READABILITY = 60;
    private static final int MAX_SENTENCE_LENGTH = 25;

    public PlainLanguageScore assess(String text) {
        double fleschScore = calculateFleschReadingEase(text);
        double avgSentenceLength = calculateAverageSentenceLength(text);
        List<String> complexWords = findComplexWords(text);

        boolean passes = fleschScore >= MIN_READABILITY
            && avgSentenceLength <= MAX_SENTENCE_LENGTH
            && complexWords.size() <= 5;

        return new PlainLanguageScore(
            fleschScore,
            avgSentenceLength,
            complexWords,
            passes,
            generateImprovementSuggestions(text)
        );
    }
}
```

#### Jargon Replacement Map

| Jargon Term | Plain English |
|-------------|---------------|
| APR | Annual interest rate |
| LTV | Loan-to-value (how much you borrow vs property value) |
| Equity | The part of your home you own outright |
| Amortisation | Paying off your loan over time |
| Affordability | What you can comfortably afford to pay |
| Collateral | Asset used to secure the loan |

---

### 4. Consumer Support

**FCA Requirement**: Customers receive support when they need it.

#### Regulus Controls

```java
@Tool(name = "handle_customer_query")
@VulnerableCustomerAware
@EscalationEnabled
public CustomerResponse handleQuery(
    CustomerQuery query,
    CustomerProfile customer
) {
    // Check for vulnerable customer indicators
    VulnerabilityAssessment vulnerability = vulnerabilityDetector.assess(
        customer,
        query
    );

    if (vulnerability.isVulnerable()) {
        // Flag for enhanced support
        return handleVulnerableCustomer(query, customer, vulnerability);
    }

    // Check if escalation needed
    if (escalationChecker.needsHumanReview(query)) {
        return escalateToHuman(query, customer, "COMPLEXITY");
    }

    // Process normally with audit trail
    CustomerResponse response = processQuery(query, customer);

    // Ensure support path is clear
    response = response.withSupportOptions(
        getSupportOptions(customer)
    );

    return response;
}
```

#### Vulnerable Customer Detection

```java
public class VulnerabilityDetector {

    // FCA vulnerability drivers
    public enum VulnerabilityDriver {
        HEALTH,           // Physical or mental health conditions
        LIFE_EVENTS,      // Bereavement, job loss, divorce
        RESILIENCE,       // Low financial resilience
        CAPABILITY        // Low financial capability or confidence
    }

    public VulnerabilityAssessment assess(
        CustomerProfile customer,
        CustomerQuery query
    ) {
        List<VulnerabilityIndicator> indicators = new ArrayList<>();

        // Health indicators
        if (query.mentionsHealthCondition()) {
            indicators.add(new VulnerabilityIndicator(
                HEALTH, "Health condition mentioned", Severity.MEDIUM
            ));
        }

        // Life event indicators
        if (query.mentionsBereavement() || query.mentionsJobLoss()) {
            indicators.add(new VulnerabilityIndicator(
                LIFE_EVENTS, "Significant life event", Severity.HIGH
            ));
        }

        // Financial stress indicators
        if (query.indicatesFinancialStress()) {
            indicators.add(new VulnerabilityIndicator(
                RESILIENCE, "Financial stress indicated", Severity.HIGH
            ));
        }

        // Capability indicators
        if (query.showsConfusion() || customer.hasLowFinancialLiteracy()) {
            indicators.add(new VulnerabilityIndicator(
                CAPABILITY, "May need additional support", Severity.MEDIUM
            ));
        }

        return new VulnerabilityAssessment(
            !indicators.isEmpty(),
            indicators,
            recommendActions(indicators)
        );
    }
}
```

#### Configuration

```yaml
regulus:
  ai:
    governance:
      consumer-duty:
        consumer-support:
          enabled: true
          vulnerable-customers:
            detection-enabled: true
            enhanced-support: true
            escalation-required: true
            indicators:
              - health-conditions
              - life-events
              - financial-stress
              - low-capability
          escalation:
            enabled: true
            triggers:
              - COMPLAINT
              - COMPLEX_QUERY
              - VULNERABLE_CUSTOMER
              - HIGH_VALUE_DECISION
            channels:
              - HUMAN_AGENT
              - SPECIALIST_TEAM
              - COMPLAINTS_HANDLER
          support-options:
            always-visible: true
            include-phone: true
            include-branch: true
            include-chat: true
```

---

## Cross-Cutting Rules

### Acting in Good Faith

```java
@Aspect
@Component
public class GoodFaithGuard {

    @Around("@annotation(RequireGoodFaith)")
    public Object enforceGoodFaith(ProceedingJoinPoint joinPoint) throws Throwable {
        // Check for conflicts of interest
        ConflictCheck conflict = conflictChecker.check(joinPoint.getArgs());
        if (conflict.hasConflict()) {
            auditLogger.logConflict(conflict);
            // Disclose conflict to customer
        }

        // Ensure recommendation is in customer's interest
        Object result = joinPoint.proceed();

        if (result instanceof Recommendation rec) {
            if (!customerInterestValidator.isInCustomerInterest(rec)) {
                throw new GoodFaithViolationException(
                    "Recommendation not in customer's best interest"
                );
            }
        }

        return result;
    }
}
```

### Avoiding Foreseeable Harm

```java
@Aspect
@Component
public class ForeseeableHarmGuard {

    @Around("@annotation(PreventForeseeableHarm)")
    public Object preventHarm(ProceedingJoinPoint joinPoint) throws Throwable {
        // Pre-execution harm assessment
        HarmAssessment preAssessment = harmAssessor.assess(joinPoint.getArgs());

        if (preAssessment.hasForeseeableHarm()) {
            if (preAssessment.getSeverity() == Severity.HIGH) {
                // Block execution
                throw new ForeseeableHarmException(preAssessment.getDetails());
            } else {
                // Warn and log
                auditLogger.logPotentialHarm(preAssessment);
            }
        }

        Object result = joinPoint.proceed();

        // Post-execution harm check
        HarmAssessment postAssessment = harmAssessor.assessOutcome(result);
        if (postAssessment.hasCausedHarm()) {
            // Trigger remediation workflow
            remediationService.initiateRemediation(postAssessment);
        }

        return result;
    }
}
```

---

## Monitoring and Reporting

### Consumer Duty Dashboard Metrics

```yaml
# Prometheus metrics for Consumer Duty monitoring
regulus_consumer_duty_suitability_checks_total{outcome="suitable"} 15234
regulus_consumer_duty_suitability_checks_total{outcome="unsuitable"} 342
regulus_consumer_duty_vulnerable_customers_detected_total 89
regulus_consumer_duty_escalations_total{reason="complexity"} 156
regulus_consumer_duty_escalations_total{reason="vulnerable"} 89
regulus_consumer_duty_plain_language_score_avg 72.5
regulus_consumer_duty_complaints_total 23
```

### Board Reporting Template

```markdown
## Consumer Duty Quarterly Report

### Products and Services
- Suitability checks performed: 15,576
- Unsuitable recommendations prevented: 342 (2.2%)
- Target market validations: 15,576

### Price and Value
- Value assessments completed: 8,234
- Fee disclosures provided: 100%
- Average value score: 7.8/10

### Consumer Understanding
- Average readability score: 72.5 (target: 60)
- Jargon terms replaced: 1,234
- Customer comprehension feedback: 4.2/5

### Consumer Support
- Vulnerable customers identified: 89
- Escalations to human agents: 245
- Average resolution time: 2.3 hours
- Complaints received: 23
- Complaints resolved within SLA: 22 (95.7%)

### Actions Required
1. [Action item from monitoring]
2. [Action item from complaints analysis]
```

---

## Implementation Checklist

### Phase 1: Foundation
- [ ] Enable Consumer Duty module in configuration
- [ ] Implement suitability checking for all product recommendations
- [ ] Add target market validation
- [ ] Configure fee transparency requirements

### Phase 2: Communications
- [ ] Implement plain language validation
- [ ] Add jargon detection and replacement
- [ ] Configure explanation requirements
- [ ] Test readability scoring

### Phase 3: Support
- [ ] Implement vulnerable customer detection
- [ ] Configure escalation triggers
- [ ] Set up human handoff workflows
- [ ] Test support path visibility

### Phase 4: Monitoring
- [ ] Deploy Consumer Duty dashboard
- [ ] Configure alerting thresholds
- [ ] Set up board reporting
- [ ] Schedule quarterly reviews

---

## Related Documentation

- [Governance & Security](./governance-security.md)
- [Regulatory Reference](../references/regulatory-reference.md)
- [Risk Control Matrix](./risk-control-matrix.md)
- [Quickstart Tutorial](../guides/quickstart-tutorial.md)
