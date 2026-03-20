package com.regulus.platform.agents.mcp.server.tools;

import com.regulus.platform.agents.mcp.McpTool;
import com.regulus.platform.agents.mcp.server.McpServer.ToolHandler;
import com.regulus.platform.agents.mcp.server.McpServer.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

/**
 * Risk scoring tool for transaction analysis.
 * Calculates risk scores based on transaction attributes and patterns.
 */
public class RiskScoringTool implements ToolHandler {

    private static final Logger log = LoggerFactory.getLogger(RiskScoringTool.class);

    // Risk thresholds
    private static final double HIGH_AMOUNT_GBP = 10000.0;
    private static final double VERY_HIGH_AMOUNT_GBP = 50000.0;

    // High-risk countries (sanctions-sensitive)
    private static final Set<String> HIGH_RISK_COUNTRIES = Set.of(
        "KP", "IR", "SY", "CU", "VE", "RU", "BY"
    );

    // Medium-risk countries
    private static final Set<String> MEDIUM_RISK_COUNTRIES = Set.of(
        "AE", "PA", "MT", "CY", "VG", "KY", "JE", "GG", "IM"
    );

    /**
     * Get the tool definition for registration.
     */
    public static McpTool getToolDefinition(String serverUrl) {
        return McpTool.builder()
            .name("risk_score")
            .description("Calculates risk score for financial transactions. " +
                "Analyzes amount, currency, counterparty, geography, and patterns " +
                "to produce a risk assessment with score (0-100) and risk level.")
            .serverUrl(serverUrl)
            .inputSchema(Map.of(
                "type", "object",
                "properties", Map.of(
                    "transactionId", Map.of(
                        "type", "string",
                        "description", "Unique transaction identifier"
                    ),
                    "amount", Map.of(
                        "type", "number",
                        "description", "Transaction amount"
                    ),
                    "currency", Map.of(
                        "type", "string",
                        "description", "ISO 4217 currency code (e.g., GBP, USD, EUR)"
                    ),
                    "senderCountry", Map.of(
                        "type", "string",
                        "description", "ISO 3166-1 alpha-2 sender country code"
                    ),
                    "receiverCountry", Map.of(
                        "type", "string",
                        "description", "ISO 3166-1 alpha-2 receiver country code"
                    ),
                    "paymentType", Map.of(
                        "type", "string",
                        "description", "Type of payment (SEPA, SWIFT, FASTER_PAYMENTS, etc.)"
                    ),
                    "isFirstTransaction", Map.of(
                        "type", "boolean",
                        "description", "Whether this is the first transaction with this counterparty"
                    ),
                    "customerSegment", Map.of(
                        "type", "string",
                        "description", "Customer segment (RETAIL, BUSINESS, CORPORATE, PB)"
                    )
                ),
                "required", List.of("transactionId", "amount", "currency")
            ))
            .build();
    }

    @Override
    public ToolResult handle(Map<String, Object> arguments) {
        String transactionId = (String) arguments.get("transactionId");
        Number amountNum = (Number) arguments.get("amount");
        String currency = (String) arguments.get("currency");

        if (transactionId == null || amountNum == null || currency == null) {
            return ToolResult.error("transactionId, amount, and currency are required");
        }

        double amount = amountNum.doubleValue();
        String senderCountry = (String) arguments.getOrDefault("senderCountry", "GB");
        String receiverCountry = (String) arguments.getOrDefault("receiverCountry", "GB");
        String paymentType = (String) arguments.getOrDefault("paymentType", "UNKNOWN");
        boolean isFirstTransaction = (Boolean) arguments.getOrDefault("isFirstTransaction", false);
        String customerSegment = (String) arguments.getOrDefault("customerSegment", "RETAIL");

        log.info("Calculating risk score for transaction: {} amount={} {}",
            transactionId, amount, currency);

        try {
            RiskAssessment assessment = calculateRisk(
                transactionId, amount, currency,
                senderCountry, receiverCountry, paymentType,
                isFirstTransaction, customerSegment
            );
            return ToolResult.success(assessment.toMap());
        } catch (Exception e) {
            log.error("Risk calculation error: {}", e.getMessage(), e);
            return ToolResult.error("Risk calculation failed: " + e.getMessage());
        }
    }

    private RiskAssessment calculateRisk(
            String transactionId, double amount, String currency,
            String senderCountry, String receiverCountry, String paymentType,
            boolean isFirstTransaction, String customerSegment) {

        List<RiskFactor> factors = new ArrayList<>();
        double totalScore = 0;

        // Convert to GBP equivalent for thresholds
        double amountGbp = convertToGbp(amount, currency);

        // Amount risk
        if (amountGbp >= VERY_HIGH_AMOUNT_GBP) {
            factors.add(new RiskFactor("amount", "VERY_HIGH_AMOUNT", 30,
                "Transaction amount exceeds £50,000 threshold"));
            totalScore += 30;
        } else if (amountGbp >= HIGH_AMOUNT_GBP) {
            factors.add(new RiskFactor("amount", "HIGH_AMOUNT", 20,
                "Transaction amount exceeds £10,000 threshold"));
            totalScore += 20;
        } else if (amountGbp >= 5000) {
            factors.add(new RiskFactor("amount", "ELEVATED_AMOUNT", 10,
                "Transaction amount above £5,000"));
            totalScore += 10;
        }

        // Geographic risk - sender
        if (HIGH_RISK_COUNTRIES.contains(senderCountry.toUpperCase())) {
            factors.add(new RiskFactor("geography", "HIGH_RISK_SENDER_COUNTRY", 35,
                "Sender country is high-risk: " + senderCountry));
            totalScore += 35;
        } else if (MEDIUM_RISK_COUNTRIES.contains(senderCountry.toUpperCase())) {
            factors.add(new RiskFactor("geography", "MEDIUM_RISK_SENDER_COUNTRY", 15,
                "Sender country is medium-risk: " + senderCountry));
            totalScore += 15;
        }

        // Geographic risk - receiver
        if (HIGH_RISK_COUNTRIES.contains(receiverCountry.toUpperCase())) {
            factors.add(new RiskFactor("geography", "HIGH_RISK_RECEIVER_COUNTRY", 35,
                "Receiver country is high-risk: " + receiverCountry));
            totalScore += 35;
        } else if (MEDIUM_RISK_COUNTRIES.contains(receiverCountry.toUpperCase())) {
            factors.add(new RiskFactor("geography", "MEDIUM_RISK_RECEIVER_COUNTRY", 15,
                "Receiver country is medium-risk: " + receiverCountry));
            totalScore += 15;
        }

        // Cross-border risk
        if (!senderCountry.equalsIgnoreCase(receiverCountry)) {
            factors.add(new RiskFactor("geography", "CROSS_BORDER", 5,
                "Cross-border transaction"));
            totalScore += 5;
        }

        // First transaction risk
        if (isFirstTransaction) {
            factors.add(new RiskFactor("relationship", "FIRST_TRANSACTION", 10,
                "First transaction with this counterparty"));
            totalScore += 10;
        }

        // Payment type risk
        if ("SWIFT".equalsIgnoreCase(paymentType)) {
            factors.add(new RiskFactor("payment_type", "SWIFT_PAYMENT", 5,
                "SWIFT international payment"));
            totalScore += 5;
        }

        // Cap score at 100
        int finalScore = (int) Math.min(100, Math.max(0, totalScore));

        // Determine risk level
        String riskLevel;
        String recommendation;
        if (finalScore >= 70) {
            riskLevel = "HIGH";
            recommendation = "MANUAL_REVIEW";
        } else if (finalScore >= 40) {
            riskLevel = "MEDIUM";
            recommendation = "ENHANCED_MONITORING";
        } else {
            riskLevel = "LOW";
            recommendation = "PROCEED";
        }

        return new RiskAssessment(
            transactionId,
            finalScore,
            riskLevel,
            recommendation,
            factors,
            Map.of(
                "amountGbp", amountGbp,
                "originalAmount", amount,
                "currency", currency,
                "customerSegment", customerSegment,
                "calculatedAt", Instant.now().toString()
            )
        );
    }

    private double convertToGbp(double amount, String currency) {
        // Simplified FX rates (in production, use real-time rates)
        return switch (currency.toUpperCase()) {
            case "GBP" -> amount;
            case "EUR" -> amount * 0.86;
            case "USD" -> amount * 0.79;
            case "CHF" -> amount * 0.90;
            case "JPY" -> amount * 0.0053;
            default -> amount; // Assume 1:1 for unknown currencies
        };
    }

    // Risk factor record
    private record RiskFactor(
        String category,
        String code,
        int score,
        String description
    ) {
        Map<String, Object> toMap() {
            return Map.of(
                "category", category,
                "code", code,
                "score", score,
                "description", description
            );
        }
    }

    // Risk assessment result
    private record RiskAssessment(
        String transactionId,
        int score,
        String riskLevel,
        String recommendation,
        List<RiskFactor> factors,
        Map<String, Object> metadata
    ) {
        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("transactionId", transactionId);
            map.put("score", score);
            map.put("riskLevel", riskLevel);
            map.put("recommendation", recommendation);
            map.put("factors", factors.stream().map(RiskFactor::toMap).toList());
            map.put("metadata", metadata);
            return map;
        }
    }
}
