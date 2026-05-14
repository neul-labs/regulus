package com.neullabs.regulus.safety.autoconfigure;

import com.neullabs.regulus.killswitch.interceptor.KillSwitchManager;
import com.neullabs.regulus.privacy.filter.PrivacyFilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central safety guard that combines all safety checks.
 * Use this to apply safety measures to agent inputs and outputs.
 */
public class SafetyGuard {

    private static final Logger log = LoggerFactory.getLogger(SafetyGuard.class);

    private final KillSwitchManager killSwitchManager;
    private final PrivacyFilterChain privacyFilterChain;
    private final PromptInjectionDetector promptInjectionDetector;

    public SafetyGuard(
            KillSwitchManager killSwitchManager,
            PrivacyFilterChain privacyFilterChain,
            PromptInjectionDetector promptInjectionDetector) {
        this.killSwitchManager = killSwitchManager;
        this.privacyFilterChain = privacyFilterChain;
        this.promptInjectionDetector = promptInjectionDetector;
    }

    /**
     * Check if operations are globally blocked.
     */
    public boolean isBlocked() {
        return killSwitchManager.getGlobalState().isActive();
    }

    /**
     * Check if a specific operation is blocked.
     */
    public boolean isBlocked(String agentId, String modelId, String toolName) {
        return killSwitchManager.isBlocked(agentId, modelId, toolName);
    }

    /**
     * Apply all input safety checks.
     *
     * @param input user input to check
     * @return safety check result
     */
    public SafetyCheckResult checkInput(String input) {
        // Check kill switch first
        if (isBlocked()) {
            return SafetyCheckResult.blocked("Kill switch is active");
        }

        // Check for prompt injection
        PromptInjectionDetector.DetectionResult injectionResult = promptInjectionDetector.detect(input);
        if (injectionResult.detected()) {
            return SafetyCheckResult.blocked(
                "Potential prompt injection detected: " + injectionResult.explanation()
            );
        }

        // Apply privacy filters to sanitize input
        String sanitizedInput = privacyFilterChain.filterContent(input, "text/plain");

        return SafetyCheckResult.safe(sanitizedInput);
    }

    /**
     * Apply all output safety checks.
     *
     * @param output LLM output to check
     * @return safety check result
     */
    public SafetyCheckResult checkOutput(String output) {
        // Apply privacy filters to sanitize output
        String sanitizedOutput = privacyFilterChain.filterContent(output, "text/plain");

        return SafetyCheckResult.safe(sanitizedOutput);
    }

    /**
     * Result of safety check.
     */
    public record SafetyCheckResult(
        boolean safe,
        String sanitizedContent,
        String blockReason
    ) {
        public static SafetyCheckResult safe(String content) {
            return new SafetyCheckResult(true, content, null);
        }

        public static SafetyCheckResult blocked(String reason) {
            return new SafetyCheckResult(false, null, reason);
        }

        public boolean isBlocked() {
            return !safe;
        }
    }

    /**
     * Get the kill switch manager.
     */
    public KillSwitchManager getKillSwitchManager() {
        return killSwitchManager;
    }

    /**
     * Get the privacy filter chain.
     */
    public PrivacyFilterChain getPrivacyFilterChain() {
        return privacyFilterChain;
    }

    /**
     * Get the prompt injection detector.
     */
    public PromptInjectionDetector getPromptInjectionDetector() {
        return promptInjectionDetector;
    }
}
