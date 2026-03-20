package com.regulus.demo;

import java.util.List;
import java.util.Map;

/**
 * Sample banking tools for the agent demo.
 * These simulate real banking operations without actual backend calls.
 */
public class BankingTools {

    /**
     * Tool to get account balance.
     */
    public static Tool getAccountBalanceTool() {
        return new Tool() {
            @Override
            public String getName() {
                return "get_account_balance";
            }

            @Override
            public String getDescription() {
                return "Get the current balance of a bank account. " +
                       "Use this when the user asks about their balance or account status.";
            }

            @Override
            public Map<String, Object> getInputSchema() {
                return Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "account_id", Map.of(
                            "type", "string",
                            "description", "The account ID to query"
                        )
                    ),
                    "required", List.of("account_id")
                );
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                String accountId = (String) arguments.get("account_id");

                // Simulate account lookup
                if (accountId == null || accountId.isBlank()) {
                    return ToolResult.error("Account ID is required");
                }

                // Return fake data
                return ToolResult.success(
                    String.format(
                        "Account %s balance: £2,450.00 (Available: £2,300.00)",
                        accountId
                    ),
                    Map.of(
                        "account_id", accountId,
                        "balance", 2450.00,
                        "available", 2300.00,
                        "currency", "GBP"
                    )
                );
            }
        };
    }

    /**
     * Tool to get recent transactions.
     */
    public static Tool getTransactionsTool() {
        return new Tool() {
            @Override
            public String getName() {
                return "get_transactions";
            }

            @Override
            public String getDescription() {
                return "Get recent transactions for a bank account. " +
                       "Use this when the user asks about their transaction history or recent activity.";
            }

            @Override
            public Map<String, Object> getInputSchema() {
                return Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "account_id", Map.of(
                            "type", "string",
                            "description", "The account ID to query"
                        ),
                        "limit", Map.of(
                            "type", "integer",
                            "description", "Maximum number of transactions to return",
                            "default", 10
                        )
                    ),
                    "required", List.of("account_id")
                );
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                String accountId = (String) arguments.get("account_id");
                int limit = arguments.containsKey("limit")
                    ? ((Number) arguments.get("limit")).intValue()
                    : 10;

                if (accountId == null || accountId.isBlank()) {
                    return ToolResult.error("Account ID is required");
                }

                // Return fake transaction data
                String transactions = """
                    Recent transactions for %s:
                    1. Dec 10: SALARY DEPOSIT     +£3,000.00
                    2. Dec 09: TESCO GROCERIES    -£85.50
                    3. Dec 08: BRITISH GAS        -£120.00
                    4. Dec 07: PIZZA EXPRESS      -£45.00
                    5. Dec 06: ATM WITHDRAWAL     -£200.00
                    """.formatted(accountId);

                return ToolResult.success(
                    transactions,
                    Map.of(
                        "account_id", accountId,
                        "transaction_count", Math.min(limit, 5)
                    )
                );
            }
        };
    }

    /**
     * Tool to initiate a payment.
     */
    public static Tool makePaymentTool() {
        return new Tool() {
            @Override
            public String getName() {
                return "make_payment";
            }

            @Override
            public String getDescription() {
                return "Initiate a payment from one account to another. " +
                       "Use this when the user wants to transfer money or pay someone.";
            }

            @Override
            public Map<String, Object> getInputSchema() {
                return Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "from_account", Map.of(
                            "type", "string",
                            "description", "The source account ID"
                        ),
                        "to_account", Map.of(
                            "type", "string",
                            "description", "The destination account ID or sort code + account number"
                        ),
                        "amount", Map.of(
                            "type", "number",
                            "description", "The amount to transfer"
                        ),
                        "reference", Map.of(
                            "type", "string",
                            "description", "Payment reference"
                        )
                    ),
                    "required", List.of("from_account", "to_account", "amount")
                );
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                String fromAccount = (String) arguments.get("from_account");
                String toAccount = (String) arguments.get("to_account");
                double amount = ((Number) arguments.get("amount")).doubleValue();
                String reference = (String) arguments.getOrDefault("reference", "Payment");

                // Simulate validation
                if (amount <= 0) {
                    return ToolResult.error("Amount must be greater than zero");
                }
                if (amount > 10000) {
                    return ToolResult.error(
                        "Amount exceeds daily limit. Large payments require additional authorization."
                    );
                }

                // Simulate successful payment
                String paymentId = "PAY-" + System.currentTimeMillis();
                return ToolResult.success(
                    String.format(
                        "Payment initiated: £%.2f from %s to %s. Reference: %s. Payment ID: %s. " +
                        "Status: PENDING (will complete within 2 hours)",
                        amount, fromAccount, toAccount, reference, paymentId
                    ),
                    Map.of(
                        "payment_id", paymentId,
                        "status", "PENDING",
                        "amount", amount,
                        "from", fromAccount,
                        "to", toAccount
                    )
                );
            }
        };
    }
}
