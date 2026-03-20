package com.regulus.platform.agents.mcp.server.tools;

import com.regulus.platform.agents.mcp.McpTool;
import com.regulus.platform.agents.mcp.server.McpServer;
import com.regulus.platform.agents.mcp.server.McpServer.ToolHandler;
import com.regulus.platform.agents.mcp.server.McpServer.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * ISO 20022 payment message validator tool.
 * Validates pain.001, pain.002, pacs.008, and other ISO 20022 message types.
 */
public class Iso20022ValidatorTool implements ToolHandler {

    private static final Logger log = LoggerFactory.getLogger(Iso20022ValidatorTool.class);

    // Supported message types and their basic validation patterns
    private static final Map<String, MessageTypeConfig> MESSAGE_TYPES = Map.of(
        "pain.001", new MessageTypeConfig(
            "Customer Credit Transfer Initiation",
            Pattern.compile("<CstmrCdtTrfInitn>.*</CstmrCdtTrfInitn>", Pattern.DOTALL),
            List.of("GrpHdr", "PmtInf", "CdtTrfTxInf")
        ),
        "pain.002", new MessageTypeConfig(
            "Customer Payment Status Report",
            Pattern.compile("<CstmrPmtStsRpt>.*</CstmrPmtStsRpt>", Pattern.DOTALL),
            List.of("GrpHdr", "OrgnlGrpInfAndSts")
        ),
        "pacs.008", new MessageTypeConfig(
            "FI to FI Customer Credit Transfer",
            Pattern.compile("<FIToFICstmrCdtTrf>.*</FIToFICstmrCdtTrf>", Pattern.DOTALL),
            List.of("GrpHdr", "CdtTrfTxInf")
        ),
        "pacs.002", new MessageTypeConfig(
            "FI to FI Payment Status Report",
            Pattern.compile("<FIToFIPmtStsRpt>.*</FIToFIPmtStsRpt>", Pattern.DOTALL),
            List.of("GrpHdr", "TxInfAndSts")
        ),
        "camt.053", new MessageTypeConfig(
            "Bank to Customer Statement",
            Pattern.compile("<BkToCstmrStmt>.*</BkToCstmrStmt>", Pattern.DOTALL),
            List.of("GrpHdr", "Stmt")
        )
    );

    // IBAN validation pattern
    private static final Pattern IBAN_PATTERN = Pattern.compile(
        "^[A-Z]{2}\\d{2}[A-Z0-9]{4}\\d{7}([A-Z0-9]?){0,16}$"
    );

    // BIC validation pattern
    private static final Pattern BIC_PATTERN = Pattern.compile(
        "^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$"
    );

    /**
     * Get the tool definition for registration.
     */
    public static McpTool getToolDefinition(String serverUrl) {
        return McpTool.builder()
            .name("iso20022_validate")
            .description("Validates ISO 20022 payment messages for compliance with the standard. " +
                "Supports pain.001 (credit transfer initiation), pain.002 (payment status), " +
                "pacs.008 (FI credit transfer), pacs.002 (FI status), and camt.053 (bank statement).")
            .serverUrl(serverUrl)
            .inputSchema(Map.of(
                "type", "object",
                "properties", Map.of(
                    "message", Map.of(
                        "type", "string",
                        "description", "The ISO 20022 XML message to validate"
                    ),
                    "messageType", Map.of(
                        "type", "string",
                        "description", "The message type (e.g., pain.001, pacs.008)",
                        "enum", List.of("pain.001", "pain.002", "pacs.008", "pacs.002", "camt.053")
                    ),
                    "strictMode", Map.of(
                        "type", "boolean",
                        "description", "Enable strict validation mode (default: false)",
                        "default", false
                    )
                ),
                "required", List.of("message", "messageType")
            ))
            .build();
    }

    @Override
    public ToolResult handle(Map<String, Object> arguments) {
        String message = (String) arguments.get("message");
        String messageType = (String) arguments.get("messageType");
        boolean strictMode = (Boolean) arguments.getOrDefault("strictMode", false);

        if (message == null || message.isBlank()) {
            return ToolResult.error("Message is required");
        }

        if (messageType == null || messageType.isBlank()) {
            return ToolResult.error("Message type is required");
        }

        log.info("Validating ISO 20022 message: type={}, strict={}", messageType, strictMode);

        try {
            ValidationResult result = validateMessage(message, messageType, strictMode);
            return ToolResult.success(result.toMap());
        } catch (Exception e) {
            log.error("Validation error: {}", e.getMessage(), e);
            return ToolResult.error("Validation failed: " + e.getMessage());
        }
    }

    private ValidationResult validateMessage(String message, String messageType, boolean strictMode) {
        List<ValidationIssue> issues = new ArrayList<>();
        boolean isValid = true;

        // Check message type is supported
        MessageTypeConfig config = MESSAGE_TYPES.get(messageType.toLowerCase());
        if (config == null) {
            return new ValidationResult(false, messageType, List.of(
                new ValidationIssue("ERROR", "UNSUPPORTED_TYPE",
                    "Unsupported message type: " + messageType,
                    "Use one of: " + MESSAGE_TYPES.keySet())
            ), Map.of());
        }

        // Basic XML structure validation
        if (!message.trim().startsWith("<?xml") && !message.trim().startsWith("<")) {
            issues.add(new ValidationIssue("ERROR", "INVALID_XML",
                "Message does not appear to be valid XML", null));
            isValid = false;
        }

        // Check for root element
        if (!config.rootPattern().matcher(message).find()) {
            issues.add(new ValidationIssue("ERROR", "MISSING_ROOT",
                "Missing or invalid root element for " + messageType,
                "Expected: " + config.description()));
            isValid = false;
        }

        // Check for required elements
        for (String required : config.requiredElements()) {
            if (!message.contains("<" + required + ">") && !message.contains("<" + required + " ")) {
                issues.add(new ValidationIssue("ERROR", "MISSING_ELEMENT",
                    "Missing required element: " + required, null));
                isValid = false;
            }
        }

        // Extract and validate IBANs
        List<String> ibans = extractPattern(message, "<IBAN>([^<]+)</IBAN>");
        for (String iban : ibans) {
            if (!IBAN_PATTERN.matcher(iban.replaceAll("\\s", "")).matches()) {
                issues.add(new ValidationIssue("ERROR", "INVALID_IBAN",
                    "Invalid IBAN format: " + maskSensitive(iban), null));
                isValid = false;
            }
        }

        // Extract and validate BICs
        List<String> bics = extractPattern(message, "<BIC>([^<]+)</BIC>");
        for (String bic : bics) {
            if (!BIC_PATTERN.matcher(bic.trim()).matches()) {
                issues.add(new ValidationIssue("ERROR", "INVALID_BIC",
                    "Invalid BIC format: " + bic, null));
                isValid = false;
            }
        }

        // Strict mode additional checks
        if (strictMode) {
            // Check for proper namespace declaration
            if (!message.contains("xmlns")) {
                issues.add(new ValidationIssue("WARNING", "MISSING_NAMESPACE",
                    "Missing XML namespace declaration", null));
            }

            // Check for message ID
            if (!message.contains("<MsgId>")) {
                issues.add(new ValidationIssue("WARNING", "MISSING_MSG_ID",
                    "Missing Message ID element", null));
            }

            // Check for creation date time
            if (!message.contains("<CreDtTm>")) {
                issues.add(new ValidationIssue("WARNING", "MISSING_CRDT_TM",
                    "Missing Creation Date Time element", null));
            }
        }

        // Add info about validation
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("messageType", messageType);
        metadata.put("messageTypeDescription", config.description());
        metadata.put("strictMode", strictMode);
        metadata.put("ibansFound", ibans.size());
        metadata.put("bicsFound", bics.size());

        if (issues.isEmpty()) {
            issues.add(new ValidationIssue("INFO", "VALID",
                "Message passes all validation checks", null));
        }

        return new ValidationResult(isValid, messageType, issues, metadata);
    }

    private List<String> extractPattern(String text, String patternStr) {
        List<String> matches = new ArrayList<>();
        Pattern pattern = Pattern.compile(patternStr);
        var matcher = pattern.matcher(text);
        while (matcher.find()) {
            matches.add(matcher.group(1));
        }
        return matches;
    }

    private String maskSensitive(String value) {
        if (value == null || value.length() < 8) return "****";
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    // Configuration for message types
    private record MessageTypeConfig(
        String description,
        Pattern rootPattern,
        List<String> requiredElements
    ) {}

    // Validation result
    private record ValidationResult(
        boolean valid,
        String messageType,
        List<ValidationIssue> issues,
        Map<String, Object> metadata
    ) {
        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("valid", valid);
            map.put("messageType", messageType);
            map.put("issues", issues.stream().map(ValidationIssue::toMap).toList());
            map.put("metadata", metadata);
            return map;
        }
    }

    // Validation issue
    private record ValidationIssue(
        String severity,
        String code,
        String message,
        String suggestion
    ) {
        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("severity", severity);
            map.put("code", code);
            map.put("message", message);
            if (suggestion != null) {
                map.put("suggestion", suggestion);
            }
            return map;
        }
    }
}
