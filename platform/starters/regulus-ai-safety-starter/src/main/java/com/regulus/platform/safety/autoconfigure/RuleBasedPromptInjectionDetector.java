package com.regulus.platform.safety.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Rule-based prompt injection detector using pattern matching.
 * Detects common prompt injection attack patterns.
 */
public class RuleBasedPromptInjectionDetector implements PromptInjectionDetector {

    private static final Logger log = LoggerFactory.getLogger(RuleBasedPromptInjectionDetector.class);

    private static final List<InjectionPattern> PATTERNS = List.of(
        // Instruction override attempts
        new InjectionPattern(
            "ignore.*(?:previous|all|above|prior).*(?:instructions?|prompts?|rules?)",
            "Instruction override attempt"
        ),
        new InjectionPattern(
            "disregard.*(?:everything|all|prior|previous)",
            "Disregard instruction attempt"
        ),
        new InjectionPattern(
            "forget.*(?:everything|what|all|previous)",
            "Memory manipulation attempt"
        ),

        // Role/identity manipulation
        new InjectionPattern(
            "you\\s+are\\s+(?:now|actually|really)",
            "Role manipulation attempt"
        ),
        new InjectionPattern(
            "pretend\\s+(?:you(?:'re|\\s+are)?|to\\s+be)",
            "Pretend/roleplay instruction"
        ),
        new InjectionPattern(
            "act\\s+as\\s+(?:if|a|an)",
            "Acting instruction"
        ),

        // System prompt extraction
        new InjectionPattern(
            "(?:reveal|show|display|print|output).*(?:system|initial|original).*(?:prompt|instruction)",
            "System prompt extraction attempt"
        ),
        new InjectionPattern(
            "what.*(?:is|are).*(?:your|the).*(?:instructions?|rules?|prompts?)",
            "Instruction query attempt"
        ),

        // Control character injection
        new InjectionPattern(
            "\\[(?:INST|/INST|SYS|/SYS)\\]",
            "Instruction delimiter injection"
        ),
        new InjectionPattern(
            "<\\|(?:im_start|im_end|system|user|assistant)\\|>",
            "Chat format injection"
        ),

        // New context injection
        new InjectionPattern(
            "---+\\s*(?:BEGIN|START|NEW).*(?:CONTEXT|PROMPT|INSTRUCTION)",
            "Context boundary injection"
        ),
        new InjectionPattern(
            "\\bsystem:\\s*",
            "System role injection"
        ),

        // Encoded injection attempts
        new InjectionPattern(
            "base64\\s*(?:decode|encoded?).*(?:instruction|command)",
            "Encoded instruction attempt"
        ),

        // Developer mode / jailbreak
        new InjectionPattern(
            "(?:enable|enter|activate).*(?:developer|admin|debug|jailbreak).*mode",
            "Jailbreak mode attempt"
        ),
        new InjectionPattern(
            "DAN\\s*(?:mode|prompt)?",
            "DAN jailbreak attempt"
        )
    );

    @Override
    public DetectionResult detect(String input) {
        if (input == null || input.isBlank()) {
            return DetectionResult.clean();
        }

        String normalizedInput = input.toLowerCase().trim();

        for (InjectionPattern pattern : PATTERNS) {
            if (pattern.matches(normalizedInput)) {
                log.warn("Prompt injection detected: pattern='{}', input='{}'",
                    pattern.description,
                    truncate(input, 100)
                );
                return DetectionResult.detected(
                    pattern.description,
                    1.0,
                    "Input matched injection pattern: " + pattern.description
                );
            }
        }

        return DetectionResult.clean();
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private record InjectionPattern(Pattern regex, String description) {
        InjectionPattern(String pattern, String description) {
            this(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL), description);
        }

        boolean matches(String input) {
            return regex.matcher(input).find();
        }
    }
}
