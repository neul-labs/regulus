package com.neullabs.regulus.quickstart;

import com.neullabs.regulus.agents.mcp.McpClient;
import com.neullabs.regulus.killswitch.interceptor.KillSwitchManager;
import com.neullabs.regulus.killswitch.model.KillSwitchState;
import com.neullabs.regulus.policy.model.PolicyContext;
import com.neullabs.regulus.policy.model.PolicyResult;
import com.neullabs.regulus.policy.guard.PolicyEnforcer;
import com.neullabs.regulus.privacy.filter.PrivacyFilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST API demonstrating Regulus AI Platform capabilities.
 */
@RestController
@RequestMapping("/api")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final McpClient mcpClient;
    private final PolicyEnforcer policyEnforcer;
    private final PrivacyFilterChain privacyFilter;
    private final KillSwitchManager killSwitch;

    public PaymentController(
            McpClient mcpClient,
            PolicyEnforcer policyEnforcer,
            PrivacyFilterChain privacyFilter,
            KillSwitchManager killSwitch) {
        this.mcpClient = mcpClient;
        this.policyEnforcer = policyEnforcer;
        this.privacyFilter = privacyFilter;
        this.killSwitch = killSwitch;
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "mcpConnected", mcpClient.isConnected(),
            "killSwitchActive", killSwitch.getGlobalState().isActive()
        );
    }

    /**
     * Validate an ISO 20022 payment message.
     *
     * Example:
     *   curl -X POST http://localhost:8080/api/validate \
     *     -H "Content-Type: application/json" \
     *     -d '{"message": "<pain.001>...</pain.001>", "messageType": "pain.001"}'
     */
    @PostMapping("/validate")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> validatePayment(
            @RequestBody Map<String, Object> request) {

        // Check kill switch
        if (killSwitch.getGlobalState().isActive()) {
            return CompletableFuture.completedFuture(
                ResponseEntity.status(503).body(Map.of(
                    "error", "Service temporarily disabled",
                    "code", "KILL_SWITCH_ACTIVE"
                ))
            );
        }

        String message = (String) request.get("message");
        String messageType = (String) request.get("messageType");

        log.info("Validating {} message", messageType);

        return mcpClient.invoke("iso20022_validate", Map.of(
            "message", message,
            "messageType", messageType
        )).thenApply(response -> {
            if (response.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "result", response.content(),
                    "metadata", response.metadata()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", response.errorMessage()
                ));
            }
        });
    }

    /**
     * Calculate risk score for a transaction.
     *
     * Example:
     *   curl -X POST http://localhost:8080/api/risk \
     *     -H "Content-Type: application/json" \
     *     -d '{"transactionId": "TX123", "amount": 15000, "currency": "GBP"}'
     */
    @PostMapping("/risk")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> calculateRisk(
            @RequestBody Map<String, Object> request) {

        // Check kill switch for risk scoring
        if (killSwitch.isBlocked(null, null, "risk-scoring")) {
            return CompletableFuture.completedFuture(
                ResponseEntity.status(503).body(Map.of(
                    "error", "Risk scoring temporarily disabled"
                ))
            );
        }

        log.info("Calculating risk for transaction: {}", request.get("transactionId"));

        return mcpClient.invoke("risk_score", request)
            .thenApply(response -> {
                if (response.isSuccess()) {
                    return ResponseEntity.ok(Map.of(
                        "result", response.content(),
                        "metadata", response.metadata()
                    ));
                } else {
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", response.errorMessage()
                    ));
                }
            });
    }

    /**
     * Redact PII from text content.
     *
     * Example:
     *   curl -X POST http://localhost:8080/api/redact \
     *     -H "Content-Type: application/json" \
     *     -d '{"content": "Customer John Doe, sort code 12-34-56, card 4111-1111-1111-1111"}'
     */
    @PostMapping("/redact")
    public Map<String, Object> redactPii(@RequestBody Map<String, String> request) {
        String content = request.get("content");

        log.info("Redacting PII from content (length={})", content.length());

        String redacted = privacyFilter.filterContent(content, "text/plain");
        boolean hasRedactions = !redacted.equals(content);

        return Map.of(
            "original", content,
            "redacted", redacted,
            "hasRedactions", hasRedactions
        );
    }

    /**
     * Check policy compliance.
     *
     * Example:
     *   curl -X POST http://localhost:8080/api/policy/check \
     *     -H "Content-Type: application/json" \
     *     -d '{"lei": "529900T8BM49AURSDO55", "purposeCode": "MARKETING", "consentId": "CONSENT123"}'
     */
    @PostMapping("/policy/check")
    public Map<String, Object> checkPolicy(@RequestBody Map<String, String> request) {
        String lei = request.get("lei");
        String purposeCode = request.get("purposeCode");
        String consentId = request.get("consentId");

        PolicyContext context = PolicyContext.builder()
            .legalEntityIdentifier(lei)
            .purposeCode(purposeCode)
            .consentGranted(consentId != null && !consentId.isEmpty())
            .userId("api-user")
            .build();

        log.info("Checking policy: LEI={}, purpose={}", lei, purposeCode);

        PolicyResult result = policyEnforcer.enforceAll(context);

        return Map.of(
            "allowed", result.isAllowed(),
            "violations", result.getViolations().stream()
                .map(v -> Map.of(
                    "policy", v.policyName(),
                    "type", v.violationType(),
                    "message", v.message()
                ))
                .toList(),
            "checkedPolicies", policyEnforcer.getRegisteredPolicies()
        );
    }

    /**
     * Admin endpoint to toggle kill switch.
     *
     * Example:
     *   curl -X POST http://localhost:8080/api/admin/killswitch \
     *     -H "Content-Type: application/json" \
     *     -d '{"scope": "global", "enabled": true, "reason": "Emergency shutdown"}'
     */
    @PostMapping("/admin/killswitch")
    public Map<String, Object> toggleKillSwitch(@RequestBody Map<String, Object> request) {
        String scope = (String) request.getOrDefault("scope", "global");
        boolean enabled = (Boolean) request.getOrDefault("enabled", true);
        String reason = (String) request.getOrDefault("reason", "Manual toggle");

        log.warn("Kill switch toggle: scope={}, enabled={}, reason={}", scope, enabled, reason);

        if ("global".equals(scope)) {
            if (enabled) {
                killSwitch.activateGlobal(reason, "admin");
            } else {
                killSwitch.deactivateGlobal("admin");
            }
        } else {
            KillSwitchState.Scope killScope = KillSwitchState.Scope.valueOf(scope.toUpperCase());
            String targetId = (String) request.getOrDefault("targetId", "default");
            if (enabled) {
                killSwitch.activateScoped(killScope, targetId, reason, "admin");
            } else {
                killSwitch.deactivateScoped(killScope, targetId, "admin");
            }
        }

        return Map.of(
            "scope", scope,
            "enabled", enabled,
            "globallyDisabled", killSwitch.getGlobalState().isActive()
        );
    }
}
