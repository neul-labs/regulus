package com.neullabs.regulus.safety.autoconfigure;

/**
 * Interface for detecting prompt injection attacks.
 */
public interface PromptInjectionDetector {

    /**
     * Analyze input for potential prompt injection.
     *
     * @param input the user input to analyze
     * @return detection result with details if injection detected
     */
    DetectionResult detect(String input);

    /**
     * Result of prompt injection detection.
     */
    record DetectionResult(
        boolean detected,
        String patternMatched,
        double confidence,
        String explanation
    ) {
        public static DetectionResult clean() {
            return new DetectionResult(false, null, 0.0, null);
        }

        public static DetectionResult detected(String pattern) {
            return new DetectionResult(true, pattern, 1.0, "Matched pattern: " + pattern);
        }

        public static DetectionResult detected(String pattern, double confidence, String explanation) {
            return new DetectionResult(true, pattern, confidence, explanation);
        }
    }
}
