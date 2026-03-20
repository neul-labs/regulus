# Quickstart Tutorial: Building a Compliant UK Financial Services Agent

This tutorial walks you through building a complete, production-ready AI agent for UK financial services using Regulus. By the end, you'll have a working mortgage affordability adviser with full compliance controls.

---

## What You'll Build

A **Mortgage Affordability Adviser** agent that:
- Calculates affordability based on FCA guidelines
- Exposes capabilities via MCP for other agents to consume
- Enforces data residency (UK only)
- Implements kill switch with dual-control
- Generates SS1/23 compliant audit trails

---

## Prerequisites

- Java 21+
- Gradle 8.5+
- GCP project with Vertex AI enabled (or OpenAI/Anthropic API key)
- `gcloud` CLI authenticated (`gcloud auth application-default login`)

---

## Step 1: Create the Project

### 1.1 Generate Project Structure

```bash
mkdir mortgage-adviser-agent && cd mortgage-adviser-agent
```

### 1.2 Create `build.gradle.kts`

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
}

group = "com.acmebank"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.regulus.ai/releases") }
}

dependencies {
    // Regulus Platform BOM
    implementation(platform("com.regulus.platform:regulus-ai-bom:1.0.0"))

    // Core starters
    implementation("com.regulus.platform:regulus-ai-agents-spring-boot-starter")
    implementation("com.regulus.platform:regulus-ai-safety-starter")
    implementation("com.regulus.platform:regulus-ai-governance-starter")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

### 1.3 Create `settings.gradle.kts`

```kotlin
rootProject.name = "mortgage-adviser-agent"
```

---

## Step 2: Configure the Application

### 2.1 Create `src/main/resources/application.yaml`

```yaml
spring:
  application:
    name: mortgage-adviser-agent

server:
  port: 8080

regulus:
  ai:
    # Enable agent capabilities
    agents:
      enabled: true

    # LLM Configuration - UK region for data residency
    llm:
      provider: gemini
      streaming:
        enabled: true
      gemini:
        project-id: ${GCP_PROJECT_ID}
        location: europe-west2  # GCP London
        model: gemini-1.5-pro

    # GCP Authentication
    gcp:
      authentication:
        mode: APPLICATION_DEFAULT

    # MCP Server - expose tools to other agents
    mcp:
      server:
        enabled: true
        path: /mcp
      streaming:
        enabled: true

    # A2A Server - agent-to-agent communication
    a2a:
      server:
        enabled: true
        path: /a2a
      streaming:
        enabled: true

    # Governance - SS1/23 compliance
    governance:
      enabled: true
      model-registry:
        enabled: true
      policies:
        enforced:
          - require.LEI
          - require.PurposeCode

    # Safety controls
    safety:
      enabled: true

      # Kill switch with dual-control
      kill-switch:
        enabled: true
        provider: in-memory  # Use 'vault' for production
        dual-control:
          enabled: true
          required-approvers: 2
          allow-emergency-bypass: true
          allow-self-approval: false
          authorized-approvers:
            - risk-team@acmebank.com
            - ai-ops@acmebank.com

      # Data residency - UK only
      data-residency:
        enabled: true
        allowed-regions:
          - europe-west2  # GCP London
          - eu-west-2     # AWS London
          - uksouth       # Azure UK South
        enforce-uk-residency: true
        block-violations: true
        allow-unknown-regions: false

      # Privacy controls
      privacy:
        pii-pattern:
          enabled: true
        json-path:
          enabled: true
          paths:
            - $.password
            - $.nationalInsuranceNumber
            - $.sortCode
            - $.accountNumber

      # Prompt injection protection
      prompt-injection:
        enabled: true
        block-on-detection: true

# Actuator endpoints for monitoring
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

# Logging
logging:
  level:
    com.regulus: DEBUG
    com.acmebank: DEBUG
```

---

## Step 3: Create the Application

### 3.1 Create Main Application Class

`src/main/java/com/acmebank/mortgage/MortgageAdviserApplication.java`

```java
package com.acmebank.mortgage;

import com.regulus.platform.agents.annotation.EnableAiAgents;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAiAgents
public class MortgageAdviserApplication {

    public static void main(String[] args) {
        SpringApplication.run(MortgageAdviserApplication.class, args);
    }
}
```

---

## Step 4: Create the Agent

### 4.1 Create the Mortgage Adviser Agent

`src/main/java/com/acmebank/mortgage/agent/MortgageAdviserAgent.java`

```java
package com.acmebank.mortgage.agent;

import com.regulus.platform.agents.annotation.Agent;
import com.regulus.platform.agents.annotation.Tool;
import com.regulus.platform.governance.annotation.ModelArtefact;
import com.regulus.platform.governance.annotation.RequireLEI;
import com.regulus.platform.governance.annotation.RequirePurposeCode;
import com.regulus.platform.llm.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;

/**
 * Mortgage Affordability Adviser Agent.
 *
 * <p>This agent helps customers understand their mortgage affordability
 * based on FCA guidelines. It is registered in the model inventory
 * for SS1/23 compliance.
 */
@Component
@Agent(name = "mortgage-adviser", description = "Helps customers understand mortgage affordability")
@ModelArtefact(
    owner = "Lending Team",
    riskTier = "TIER_2",
    intendedUse = "Customer-facing mortgage affordability assessments",
    reviewCadence = "QUARTERLY"
)
public class MortgageAdviserAgent {

    private static final Logger log = LoggerFactory.getLogger(MortgageAdviserAgent.class);

    // FCA affordability multiplier (typically 4.5x income)
    private static final BigDecimal INCOME_MULTIPLIER = new BigDecimal("4.5");

    // Maximum debt-to-income ratio
    private static final BigDecimal MAX_DTI_RATIO = new BigDecimal("0.45");

    private final LlmClient llmClient;

    public MortgageAdviserAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * Calculate maximum mortgage amount based on FCA affordability rules.
     *
     * @param annualIncome Customer's gross annual income
     * @param monthlyExpenses Customer's total monthly expenses
     * @param existingDebt Any existing debt commitments
     * @return Affordability assessment result
     */
    @Tool(
        name = "calculate_affordability",
        description = "Calculate maximum mortgage amount based on income and expenses"
    )
    @RequireLEI
    @RequirePurposeCode("MORTGAGE_APPLICATION")
    public AffordabilityResult calculateAffordability(
            BigDecimal annualIncome,
            BigDecimal monthlyExpenses,
            BigDecimal existingDebt
    ) {
        log.info("Calculating affordability for income: {}", annualIncome);

        // Step 1: Calculate income-based maximum
        BigDecimal incomeBasedMax = annualIncome.multiply(INCOME_MULTIPLIER);

        // Step 2: Calculate affordability based on expenses
        BigDecimal monthlyIncome = annualIncome.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        BigDecimal disposableIncome = monthlyIncome.subtract(monthlyExpenses);

        // Assume 5% interest rate for stress testing
        BigDecimal stressedMonthlyPayment = disposableIncome.multiply(new BigDecimal("0.8"));
        BigDecimal expenseBasedMax = stressedMonthlyPayment.multiply(BigDecimal.valueOf(12))
                .multiply(BigDecimal.valueOf(25)); // 25-year term

        // Step 3: Apply debt-to-income constraint
        BigDecimal totalDebt = existingDebt != null ? existingDebt : BigDecimal.ZERO;
        BigDecimal dtiBasedMax = annualIncome.multiply(MAX_DTI_RATIO).subtract(totalDebt);

        // Step 4: Take the minimum of all constraints
        BigDecimal maxMortgage = incomeBasedMax
                .min(expenseBasedMax)
                .min(dtiBasedMax.max(BigDecimal.ZERO));

        // Round to nearest thousand
        maxMortgage = maxMortgage.divide(BigDecimal.valueOf(1000), 0, RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(1000));

        log.info("Affordability calculated: max mortgage = {}", maxMortgage);

        return new AffordabilityResult(
                maxMortgage,
                new BigDecimal("0.85"), // Standard LTV
                calculateMonthlyPayment(maxMortgage, new BigDecimal("0.05"), 25),
                Instant.now(),
                "Assessment based on FCA MCOB 11 affordability rules"
        );
    }

    /**
     * Generate a suitability letter for the mortgage recommendation.
     */
    @Tool(
        name = "generate_suitability_letter",
        description = "Generate FCA-compliant suitability letter for mortgage recommendation"
    )
    @RequireLEI
    @RequirePurposeCode("MORTGAGE_APPLICATION")
    public SuitabilityLetter generateSuitabilityLetter(
            String customerName,
            AffordabilityResult affordability,
            String productRecommendation
    ) {
        log.info("Generating suitability letter for customer: {}", customerName);

        String prompt = String.format("""
            Generate a professional suitability letter for a UK mortgage application.

            Customer: %s
            Maximum Mortgage: £%s
            Recommended Product: %s
            Monthly Payment: £%s

            The letter must:
            1. Explain why the product is suitable
            2. Address Consumer Duty requirements
            3. Highlight key risks
            4. Include FCA required disclosures
            5. Be written in plain English

            Generate the letter now:
            """,
            customerName,
            affordability.maxMortgage(),
            productRecommendation,
            affordability.estimatedMonthlyPayment()
        );

        String letterContent = llmClient.chat(prompt);

        return new SuitabilityLetter(
                customerName,
                letterContent,
                Instant.now(),
                "FCA MCOB 4.7A compliant"
        );
    }

    /**
     * Check if a property is affordable for the customer.
     */
    @Tool(
        name = "check_property_affordability",
        description = "Check if a specific property price is within the customer's affordability"
    )
    public PropertyAffordabilityCheck checkPropertyAffordability(
            BigDecimal propertyPrice,
            BigDecimal deposit,
            AffordabilityResult affordability
    ) {
        BigDecimal requiredMortgage = propertyPrice.subtract(deposit);
        boolean isAffordable = requiredMortgage.compareTo(affordability.maxMortgage()) <= 0;

        BigDecimal ltv = requiredMortgage.divide(propertyPrice, 4, RoundingMode.HALF_UP);
        boolean ltvAcceptable = ltv.compareTo(new BigDecimal("0.95")) <= 0;

        return new PropertyAffordabilityCheck(
                propertyPrice,
                requiredMortgage,
                isAffordable,
                ltvAcceptable,
                ltv,
                isAffordable && ltvAcceptable
                    ? "Property is within affordability limits"
                    : "Property exceeds affordability limits"
        );
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal rate, int years) {
        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        int months = years * 12;

        // PMT formula
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = onePlusR.pow(months);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(power);
        BigDecimal denominator = power.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    // Record classes for results

    public record AffordabilityResult(
        BigDecimal maxMortgage,
        BigDecimal recommendedLtv,
        BigDecimal estimatedMonthlyPayment,
        Instant assessmentDate,
        String regulatoryBasis
    ) {}

    public record SuitabilityLetter(
        String customerName,
        String content,
        Instant generatedAt,
        String complianceStatement
    ) {}

    public record PropertyAffordabilityCheck(
        BigDecimal propertyPrice,
        BigDecimal requiredMortgage,
        boolean withinAffordability,
        boolean ltvAcceptable,
        BigDecimal actualLtv,
        String recommendation
    ) {}
}
```

---

## Step 5: Expose Tools via MCP

### 5.1 Create MCP Tool Handler

`src/main/java/com/acmebank/mortgage/mcp/MortgageAffordabilityMcpTool.java`

```java
package com.acmebank.mortgage.mcp;

import com.acmebank.mortgage.agent.MortgageAdviserAgent;
import com.regulus.platform.agents.mcp.server.McpTool;
import com.regulus.platform.agents.mcp.server.McpToolHandler;
import com.regulus.platform.agents.mcp.server.McpToolResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Exposes mortgage affordability calculation via MCP.
 * Other agents can discover and invoke this tool.
 */
@Component
@McpTool(
    name = "calculate_mortgage_affordability",
    description = "Calculate maximum mortgage amount based on FCA affordability rules"
)
public class MortgageAffordabilityMcpTool implements McpToolHandler {

    private final MortgageAdviserAgent agent;

    public MortgageAffordabilityMcpTool(MortgageAdviserAgent agent) {
        this.agent = agent;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "annual_income", Map.of(
                    "type", "number",
                    "description", "Customer's gross annual income in GBP"
                ),
                "monthly_expenses", Map.of(
                    "type", "number",
                    "description", "Total monthly expenses in GBP"
                ),
                "existing_debt", Map.of(
                    "type", "number",
                    "description", "Total existing debt in GBP"
                )
            ),
            "required", new String[]{"annual_income", "monthly_expenses"}
        );
    }

    @Override
    public McpToolResult execute(Map<String, Object> arguments) {
        BigDecimal annualIncome = new BigDecimal(arguments.get("annual_income").toString());
        BigDecimal monthlyExpenses = new BigDecimal(arguments.get("monthly_expenses").toString());
        BigDecimal existingDebt = arguments.containsKey("existing_debt")
            ? new BigDecimal(arguments.get("existing_debt").toString())
            : BigDecimal.ZERO;

        var result = agent.calculateAffordability(annualIncome, monthlyExpenses, existingDebt);

        return McpToolResult.success(Map.of(
            "max_mortgage", result.maxMortgage(),
            "recommended_ltv", result.recommendedLtv(),
            "estimated_monthly_payment", result.estimatedMonthlyPayment(),
            "assessment_date", result.assessmentDate().toString(),
            "regulatory_basis", result.regulatoryBasis()
        ));
    }
}
```

---

## Step 6: Add REST API

### 6.1 Create REST Controller

`src/main/java/com/acmebank/mortgage/api/MortgageAdviserController.java`

```java
package com.acmebank.mortgage.api;

import com.acmebank.mortgage.agent.MortgageAdviserAgent;
import com.acmebank.mortgage.agent.MortgageAdviserAgent.AffordabilityResult;
import com.acmebank.mortgage.agent.MortgageAdviserAgent.PropertyAffordabilityCheck;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/mortgage")
public class MortgageAdviserController {

    private final MortgageAdviserAgent agent;

    public MortgageAdviserController(MortgageAdviserAgent agent) {
        this.agent = agent;
    }

    @PostMapping("/affordability")
    public ResponseEntity<AffordabilityResult> calculateAffordability(
            @RequestBody AffordabilityRequest request
    ) {
        var result = agent.calculateAffordability(
            request.annualIncome(),
            request.monthlyExpenses(),
            request.existingDebt()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/property-check")
    public ResponseEntity<PropertyAffordabilityCheck> checkProperty(
            @RequestBody PropertyCheckRequest request
    ) {
        // First calculate affordability
        var affordability = agent.calculateAffordability(
            request.annualIncome(),
            request.monthlyExpenses(),
            request.existingDebt()
        );

        // Then check if property is affordable
        var result = agent.checkPropertyAffordability(
            request.propertyPrice(),
            request.deposit(),
            affordability
        );

        return ResponseEntity.ok(result);
    }

    // Request DTOs
    public record AffordabilityRequest(
        BigDecimal annualIncome,
        BigDecimal monthlyExpenses,
        BigDecimal existingDebt
    ) {}

    public record PropertyCheckRequest(
        BigDecimal annualIncome,
        BigDecimal monthlyExpenses,
        BigDecimal existingDebt,
        BigDecimal propertyPrice,
        BigDecimal deposit
    ) {}
}
```

---

## Step 7: Run and Test

### 7.1 Set Environment Variables

```bash
export GCP_PROJECT_ID=your-gcp-project
```

### 7.2 Run the Application

```bash
./gradlew bootRun
```

### 7.3 Test the REST API

```bash
# Calculate affordability
curl -X POST http://localhost:8080/api/v1/mortgage/affordability \
  -H "Content-Type: application/json" \
  -d '{
    "annualIncome": 75000,
    "monthlyExpenses": 2500,
    "existingDebt": 10000
  }'
```

**Response:**
```json
{
  "maxMortgage": 337500,
  "recommendedLtv": 0.85,
  "estimatedMonthlyPayment": 1973.45,
  "assessmentDate": "2025-01-15T10:30:00Z",
  "regulatoryBasis": "Assessment based on FCA MCOB 11 affordability rules"
}
```

### 7.4 Test MCP Tool Discovery

```bash
# List available tools
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list"
  }'
```

### 7.5 Test MCP Tool Invocation

```bash
# Call the affordability tool
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "calculate_mortgage_affordability",
      "arguments": {
        "annual_income": 75000,
        "monthly_expenses": 2500,
        "existing_debt": 10000
      }
    }
  }'
```

---

## Step 8: Test Safety Controls

### 8.1 Test Kill Switch

```bash
# The kill switch requires dual-control in production
# For testing, you can check the actuator endpoint
curl http://localhost:8080/actuator/health
```

### 8.2 Test Data Residency

The data residency enforcer automatically blocks requests to non-UK regions. If you configure an LLM endpoint in a non-approved region, you'll see:

```
DataResidencyViolationException: Data residency violation: region us-central1
not allowed for UK_REGULATED data
```

---

## Step 9: Production Checklist

Before deploying to production, ensure:

### Governance
- [ ] Agent registered in model inventory
- [ ] Risk tier assessed and documented
- [ ] Independent validation scheduled
- [ ] SS1/23 artefacts generated

### Safety
- [ ] Kill switch provider changed to `vault`
- [ ] Dual-control enabled with real approvers
- [ ] Data residency verified (UK regions only)
- [ ] PII redaction paths configured

### Operations
- [ ] Monitoring dashboards created
- [ ] Alerting configured
- [ ] Runbooks documented
- [ ] Kill switch drill scheduled

### Security
- [ ] mTLS configured for MCP/A2A
- [ ] OAuth scopes defined
- [ ] RBAC roles assigned
- [ ] Audit logging to Kafka/Splunk

---

## Next Steps

- [Spring Boot Starters](./starters.md) - Detailed configuration options
- [Kill Switch Design](../governance/kill-switch.md) - Dual-control implementation
- [Data Residency Guide](./data-residency.md) - UK GDPR compliance
- [ADK/MCP/A2A Integration](../architecture/adk-mcp-a2a.md) - Protocol details
- [Model Registry](../governance/model-registry.md) - SS1/23 compliance

---

## Troubleshooting

### "GCP credentials not found"
```bash
gcloud auth application-default login
```

### "Data residency violation"
Check your LLM endpoint is in an approved UK region (europe-west2, eu-west-2, uksouth).

### "Kill switch blocking requests"
Check if a kill switch is active:
```bash
curl http://localhost:8080/actuator/health | jq '.components.killSwitch'
```

### "MCP tools not discovered"
Ensure `@McpTool` annotated classes are in a package scanned by Spring Boot.
