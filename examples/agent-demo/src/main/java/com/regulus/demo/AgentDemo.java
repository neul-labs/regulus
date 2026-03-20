package com.regulus.demo;

import com.regulus.platform.llm.LlmClient;
import com.regulus.platform.llm.LlmRequest;
import com.regulus.platform.llm.LlmResponse;
import com.regulus.platform.policy.guard.PolicyEnforcer;
import com.regulus.platform.policy.guard.PolicyGuard;
import com.regulus.platform.policy.guard.ConsentPolicyGuard;
import com.regulus.platform.policy.guard.PurposeCodePolicyGuard;
import com.regulus.platform.policy.model.PolicyContext;
import com.regulus.platform.policy.model.PolicyResult;
import com.regulus.platform.privacy.filter.PiiPatternFilter;
import com.regulus.platform.privacy.model.RedactionResult;
import com.regulus.platform.observability.audit.AuditLogger;
import com.regulus.platform.observability.audit.AuditEvent;

import java.util.*;

/**
 * Demo showcasing Regulus platform capabilities for compliant AI agents.
 *
 * This demo uses ACTUAL Regulus platform components:
 * - regulus-ai-llm: LlmClient, LlmRequest, LlmResponse
 * - regulus-ai-policy: PolicyEnforcer, ConsentPolicyGuard, PurposeCodePolicyGuard
 * - regulus-ai-privacy: PiiPatternFilter for PII redaction
 * - regulus-ai-observability: AuditLogger for compliance logging
 *
 * Run with: ./gradlew :examples:agent-demo:run
 */
public class AgentDemo {

    private static final String BANNER = """

        ╔═══════════════════════════════════════════════════════════════════╗
        ║                                                                   ║
        ║   ██████╗ ███████╗ ██████╗ ██╗   ██╗██╗     ██╗   ██╗███████╗    ║
        ║   ██╔══██╗██╔════╝██╔════╝ ██║   ██║██║     ██║   ██║██╔════╝    ║
        ║   ██████╔╝█████╗  ██║  ███╗██║   ██║██║     ██║   ██║███████╗    ║
        ║   ██╔══██╗██╔══╝  ██║   ██║██║   ██║██║     ██║   ██║╚════██║    ║
        ║   ██║  ██║███████╗╚██████╔╝╚██████╔╝███████╗╚██████╔╝███████║    ║
        ║   ╚═╝  ╚═╝╚══════╝ ╚═════╝  ╚═════╝ ╚══════╝ ╚═════╝ ╚══════╝    ║
        ║                                                                   ║
        ║              Regulus Platform Demo - AI Agent Governance          ║
        ╚═══════════════════════════════════════════════════════════════════╝

        """;

    public static void main(String[] args) {
        System.out.println(BANNER);

        // Demo 1: LLM Client abstraction
        demoLlmClient();

        // Demo 2: Policy Enforcement
        demoPolicyEnforcement();

        // Demo 3: Privacy/PII Filtering
        demoPrivacyFilter();

        // Demo 4: Audit Logging
        demoAuditLogging();

        // Demo 5: Full agent with all components
        demoGovernedAgent();

        printSummary();
    }

    /**
     * Demo: regulus-ai-llm - Provider-agnostic LLM interface
     */
    private static void demoLlmClient() {
        printSection("1. LLM CLIENT (regulus-ai-llm)");

        System.out.println("""
            The LlmClient interface provides a unified API across providers:
            - Gemini, OpenAI, Anthropic, Azure OpenAI
            - Support for function/tool calling
            - Streaming responses
            - Token usage tracking
            """);

        // Using FakeLlmClient which implements the real LlmClient interface
        LlmClient client = FakeLlmClient.withDefaultScript();

        System.out.println("  Provider: " + client.getProviderName());
        System.out.println("  Model: " + client.getModelName());
        System.out.println("  Available: " + client.isAvailable());
        System.out.println("  Capabilities: " + client.getCapabilities());

        // Make a request
        LlmRequest request = LlmRequest.builder()
            .prompt("What is my account balance?")
            .systemPrompt("You are a banking assistant.")
            .build();

        System.out.println("\n  Making LLM request...");
        LlmResponse response = client.generate(request);

        System.out.println("  Response success: " + response.success());
        System.out.println("  Finish reason: " + response.finishReason());
        System.out.println("  Token usage: " + response.tokenUsage().totalTokens() + " tokens");
        System.out.println("  Latency: " + response.latency().toMillis() + "ms");

        if (response.finishReason() == LlmResponse.FinishReason.TOOL_CALLS) {
            System.out.println("  Tool calls requested: " + response.toolCalls().size());
            for (var toolCall : response.toolCalls()) {
                System.out.println("    - " + toolCall.name() + ": " + toolCall.arguments());
            }
        }
    }

    /**
     * Demo: regulus-ai-policy - Policy enforcement framework
     */
    private static void demoPolicyEnforcement() {
        printSection("2. POLICY ENFORCEMENT (regulus-ai-policy)");

        System.out.println("""
            PolicyEnforcer evaluates requests against registered PolicyGuards:
            - ConsentPolicyGuard: Ensures GDPR consent requirements
            - PurposeCodePolicyGuard: Validates ISO 20022 purpose codes
            - LeiPolicyGuard: Validates Legal Entity Identifiers
            """);

        // Create policy enforcer with real guards
        List<PolicyGuard> guards = List.of(
            new ConsentPolicyGuard(),
            new PurposeCodePolicyGuard()
        );
        PolicyEnforcer enforcer = new PolicyEnforcer(guards);

        System.out.println("  Registered policies: " + enforcer.getRegisteredPolicies());

        // Scenario 1: Valid consent
        System.out.println("\n  Scenario 1: Request WITH consent");
        PolicyContext contextWithConsent = PolicyContext.builder()
            .correlationId("demo-001")
            .userId("customer-123")
            .consentGranted(true)
            .lawfulBasis("CONSENT")
            .purposeCode("CONSENT")  // GDPR consent-based processing
            .build();

        PolicyResult result1 = enforcer.enforceAll(contextWithConsent);
        System.out.println("    Allowed: " + result1.isAllowed());

        // Scenario 2: Missing consent
        System.out.println("\n  Scenario 2: Request WITHOUT consent (should be denied)");
        PolicyContext contextNoConsent = PolicyContext.builder()
            .correlationId("demo-002")
            .userId("customer-456")
            .consentGranted(false)
            .lawfulBasis("CONSENT")  // Requires consent but not granted
            .build();

        PolicyResult result2 = enforcer.enforceAll(contextNoConsent);
        System.out.println("    Allowed: " + result2.isAllowed());
        if (result2.isDenied()) {
            System.out.println("    Violations:");
            for (var violation : result2.getViolations()) {
                System.out.println("      - " + violation.policyName() + ": " + violation.message());
            }
        }
    }

    /**
     * Demo: regulus-ai-privacy - PII detection and redaction
     */
    private static void demoPrivacyFilter() {
        printSection("3. PRIVACY FILTER (regulus-ai-privacy)");

        System.out.println("""
            PiiPatternFilter detects and redacts sensitive data:
            - UK National Insurance Numbers
            - Sort codes and account numbers
            - Credit card numbers
            - Phone numbers, emails, postcodes
            - IBAN and BIC/SWIFT codes
            """);

        PiiPatternFilter filter = new PiiPatternFilter();

        // Test with PII-containing text
        String sensitiveText = """
            Customer John Smith (NI: AB123456C) called about account 12345678
            at sort code 12-34-56. Please call back on 07700900123 or email
            john.smith@example.com. Address: 10 Downing Street, SW1A 2AA.
            """;

        System.out.println("  Original text:");
        System.out.println("    " + sensitiveText.replace("\n", "\n    "));

        RedactionResult result = filter.redact(sensitiveText);

        System.out.println("  Redacted text:");
        System.out.println("    " + result.redactedContent().replace("\n", "\n    "));

        System.out.println("\n  Redactions made: " + result.redactionCount());
        if (result.hasRedactions()) {
            System.out.println("  Redacted fields:");
            for (var field : result.redactedFields()) {
                System.out.println("    - " + field.fieldType() + " (" + field.path() + ")");
            }
        }
    }

    /**
     * Demo: regulus-ai-observability - Audit logging
     */
    private static void demoAuditLogging() {
        printSection("4. AUDIT LOGGING (regulus-ai-observability)");

        System.out.println("""
            AuditLogger provides compliance-grade logging:
            - Structured audit events
            - Multiple sinks (log, Kafka, database)
            - FCA SYSC 9 compliant record keeping
            """);

        AuditLogger auditLogger = new AuditLogger();

        // Log various event types
        System.out.println("  Logging audit events...\n");

        // LLM call event
        auditLogger.logLlmCall(
            "corr-001",
            "user-123",
            "gpt-4",
            "openai",
            150,
            50,
            250,
            true
        );
        System.out.println("  [LOGGED] LLM call event");

        // Policy violation event
        auditLogger.logPolicyViolation(
            "corr-002",
            "user-456",
            "require.Consent",
            "CONSENT_NOT_GRANTED",
            "Customer consent required but not provided"
        );
        System.out.println("  [LOGGED] Policy violation event");

        // General audit event
        auditLogger.log(AuditEvent.builder()
            .type(AuditEvent.EventType.TOOL_EXECUTION)
            .correlationId("corr-003")
            .userId("user-789")
            .operation("get_account_balance")
            .outcome(AuditEvent.Outcome.SUCCESS)
            .details(Map.of("accountId", "ACC-12345", "executionTime", 45))
            .build());
        System.out.println("  [LOGGED] Tool execution event");

        System.out.println("\n  (Events logged to structured audit log)");
    }

    /**
     * Demo: Full governed agent combining all components
     */
    private static void demoGovernedAgent() {
        printSection("5. GOVERNED AGENT (Full Integration)");

        System.out.println("""
            Combining all Regulus components into a governed agent:
            1. LLM request → Privacy filter (redact input PII)
            2. Tool call → Policy enforcer (check consent/purpose)
            3. Tool result → Privacy filter (redact output PII)
            4. All actions → Audit logger (compliance trail)
            """);

        // Initialize Regulus components
        LlmClient llmClient = FakeLlmClient.withDefaultScript();
        PolicyEnforcer policyEnforcer = new PolicyEnforcer(List.of(
            new ConsentPolicyGuard(),
            new PurposeCodePolicyGuard()
        ));
        PiiPatternFilter privacyFilter = new PiiPatternFilter();
        AuditLogger auditLogger = new AuditLogger();

        // Simulate agent execution
        String userQuery = "What's my balance for account 12345678?";
        String correlationId = UUID.randomUUID().toString().substring(0, 8);

        System.out.println("\n  User query: \"" + userQuery + "\"\n");

        // Step 1: Filter PII from input
        System.out.println("  Step 1: Privacy filter on input");
        RedactionResult inputRedaction = privacyFilter.redact(userQuery);
        System.out.println("    Filtered: \"" + inputRedaction.redactedContent() + "\"");
        System.out.println("    PII found: " + inputRedaction.redactionCount());

        // Step 2: LLM generates response (requests tool call)
        System.out.println("\n  Step 2: LLM processing");
        LlmRequest request = LlmRequest.builder()
            .prompt(inputRedaction.redactedContent())
            .build();
        LlmResponse response = llmClient.generate(request);
        System.out.println("    LLM response: " + response.finishReason());

        if (!response.toolCalls().isEmpty()) {
            var toolCall = response.toolCalls().get(0);
            System.out.println("    Tool requested: " + toolCall.name());

            // Step 3: Policy check before tool execution
            System.out.println("\n  Step 3: Policy enforcement");
            PolicyContext context = PolicyContext.builder()
                .correlationId(correlationId)
                .userId("customer-001")
                .consentGranted(true)  // Customer has consented
                .lawfulBasis("CONTRACT")  // Processing for contract performance
                .purposeCode("CUSTOMER_SERVICE")  // Account inquiry for customer service
                .build();

            PolicyResult policyResult = policyEnforcer.enforceAll(context);
            System.out.println("    Policy check: " + (policyResult.isAllowed() ? "PASSED" : "BLOCKED"));

            if (policyResult.isAllowed()) {
                // Step 4: Execute tool (simulated)
                System.out.println("\n  Step 4: Tool execution");
                String toolResult = "Account 12345678 balance: £2,450.00";
                System.out.println("    Raw result: \"" + toolResult + "\"");

                // Step 5: Filter PII from output
                RedactionResult outputRedaction = privacyFilter.redact(toolResult);
                System.out.println("    Filtered: \"" + outputRedaction.redactedContent() + "\"");

                // Step 6: Audit logging
                System.out.println("\n  Step 5: Audit logging");
                auditLogger.log(AuditEvent.builder()
                    .type(AuditEvent.EventType.TOOL_EXECUTION)
                    .correlationId(correlationId)
                    .userId("customer-001")
                    .operation(toolCall.name())
                    .outcome(AuditEvent.Outcome.SUCCESS)
                    .build());
                System.out.println("    Logged: TOOL_EXECUTION event");
            }
        }
    }

    private static void printSection(String title) {
        System.out.println("\n" + "═".repeat(70));
        System.out.println(" " + title);
        System.out.println("═".repeat(70) + "\n");
    }

    private static void printSummary() {
        System.out.println("\n" + "═".repeat(70));
        System.out.println(" DEMO COMPLETE");
        System.out.println("═".repeat(70));
        System.out.println("""

            This demo showcased ACTUAL Regulus platform components:

            ┌──────────────────────────┬─────────────────────────────────────────┐
            │ Module                   │ Components Used                         │
            ├──────────────────────────┼─────────────────────────────────────────┤
            │ regulus-ai-llm           │ LlmClient, LlmRequest, LlmResponse      │
            │ regulus-ai-policy        │ PolicyEnforcer, ConsentPolicyGuard      │
            │ regulus-ai-privacy       │ PiiPatternFilter, RedactionResult       │
            │ regulus-ai-observability │ AuditLogger, AuditEvent                 │
            └──────────────────────────┴─────────────────────────────────────────┘

            For production use:
            - Add @EnableAiAgents to your Spring Boot application
            - Configure providers in application.yaml
            - Platform auto-configures all components

            See: docs/architecture/architecture.md for full details
            """);
    }
}
