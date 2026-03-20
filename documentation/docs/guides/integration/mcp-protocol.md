# MCP Protocol

Model Context Protocol (MCP) integration for exposing tools to AI agents.

## Overview

MCP is a JSON-RPC 2.0 based protocol that allows AI agents to discover and invoke tools. Regulus provides both MCP server (expose tools) and client (consume tools) capabilities.

## MCP Server

### Basic Setup

```yaml title="application.yml"
regulus:
  mcp:
    server:
      enabled: true
      port: 8081
      transport: stdio  # stdio, http, or websocket
```

### Defining Tools

```java
@McpTool
@Component
public class AccountTool {

    @Tool(
        name = "get_account_balance",
        description = "Get the current balance for a customer account"
    )
    public Mono<AccountBalance> getBalance(
            @ToolParam(description = "The account ID") String accountId) {

        return accountService.getBalance(accountId);
    }

    @Tool(
        name = "get_transactions",
        description = "Get recent transactions for an account"
    )
    public Mono<List<Transaction>> getTransactions(
            @ToolParam(description = "The account ID") String accountId,
            @ToolParam(description = "Number of transactions", required = false)
            Integer limit) {

        return accountService.getTransactions(accountId, limit != null ? limit : 10);
    }
}
```

### Tool Registration

```java
@Configuration
public class McpConfig {

    @Bean
    public McpServer mcpServer(List<Object> tools) {
        McpServer server = McpServer.builder()
            .name("regulus-tools")
            .version("1.0.0")
            .build();

        tools.stream()
            .filter(t -> t.getClass().isAnnotationPresent(McpTool.class))
            .forEach(server::registerTool);

        return server;
    }
}
```

### Transport Configuration

#### STDIO Transport

```yaml
regulus:
  mcp:
    server:
      transport: stdio
```

#### HTTP Transport

```yaml
regulus:
  mcp:
    server:
      transport: http
      http:
        port: 8081
        path: /mcp
        cors:
          enabled: true
          allowed-origins: ["*"]
```

#### WebSocket Transport

```yaml
regulus:
  mcp:
    server:
      transport: websocket
      websocket:
        port: 8081
        path: /mcp/ws
```

## MCP Client

### Connecting to MCP Servers

```yaml title="application.yml"
regulus:
  mcp:
    client:
      enabled: true
      servers:
        - name: banking-tools
          url: http://localhost:8081/mcp
          transport: http
        - name: market-data
          command: ["python", "market_data_server.py"]
          transport: stdio
```

### Using MCP Tools

```java
@Service
public class AgentWithTools {

    private final McpClient mcpClient;
    private final LlmClient llmClient;

    public Mono<String> processWithTools(String userMessage) {
        // Get available tools
        List<Tool> tools = mcpClient.listTools();

        // Create chat request with tools
        ChatRequest request = ChatRequest.builder()
            .messages(List.of(ChatMessage.user(userMessage)))
            .tools(tools)
            .build();

        return llmClient.chat(request)
            .flatMap(response -> handleToolCalls(response, userMessage));
    }

    private Mono<String> handleToolCalls(ChatResponse response, String originalMessage) {
        if (!response.hasToolCalls()) {
            return Mono.just(response.content());
        }

        // Execute tool calls
        return Flux.fromIterable(response.toolCalls())
            .flatMap(this::executeTool)
            .collectList()
            .flatMap(results -> continueChat(originalMessage, results));
    }

    private Mono<ToolResult> executeTool(ToolCall toolCall) {
        return mcpClient.callTool(toolCall.name(), toolCall.arguments())
            .map(result -> new ToolResult(toolCall.id(), result));
    }
}
```

## Tool Schema

MCP tools use JSON Schema for parameter definitions:

```java
@Tool(
    name = "transfer_funds",
    description = "Transfer funds between accounts"
)
public Mono<TransferResult> transfer(
        @ToolParam(
            description = "Source account ID",
            schema = @Schema(type = "string", pattern = "^[A-Z0-9]{8}$")
        )
        String fromAccount,

        @ToolParam(
            description = "Destination account ID",
            schema = @Schema(type = "string", pattern = "^[A-Z0-9]{8}$")
        )
        String toAccount,

        @ToolParam(
            description = "Amount to transfer",
            schema = @Schema(type = "number", minimum = 0.01, maximum = 10000)
        )
        BigDecimal amount,

        @ToolParam(
            description = "Currency code",
            schema = @Schema(type = "string", enum = {"GBP", "EUR", "USD"})
        )
        String currency) {

    return transferService.transfer(fromAccount, toAccount, amount, currency);
}
```

## Security

### Tool Authorization

```java
@McpTool
@Component
public class SecureAccountTool {

    @Tool(name = "get_sensitive_data")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<SensitiveData> getSensitiveData(
            @ToolParam String dataId) {
        return dataService.getSensitive(dataId);
    }
}
```

### Rate Limiting

```yaml
regulus:
  mcp:
    server:
      rate-limit:
        enabled: true
        requests-per-minute: 100
        per-client: true
```

### Input Validation

```java
@Tool(name = "query_account")
public Mono<Account> queryAccount(
        @ToolParam
        @NotBlank
        @Pattern(regexp = "^[A-Z0-9]{8}$")
        String accountId) {

    return accountService.findById(accountId);
}
```

## Error Handling

```java
@Tool(name = "get_balance")
public Mono<Balance> getBalance(@ToolParam String accountId) {
    return accountService.getBalance(accountId)
        .onErrorMap(AccountNotFoundException.class, e ->
            new McpToolException(
                McpErrorCode.INVALID_PARAMS,
                "Account not found: " + accountId
            )
        )
        .onErrorMap(ServiceException.class, e ->
            new McpToolException(
                McpErrorCode.INTERNAL_ERROR,
                "Service unavailable"
            )
        );
}
```

## Monitoring

### Metrics

- `regulus.mcp.tools.invocations.total` - Tool invocation count
- `regulus.mcp.tools.latency` - Tool execution latency
- `regulus.mcp.tools.errors.total` - Tool errors by type

```promql
# Tool invocation rate
rate(regulus_mcp_tools_invocations_total[5m])

# Tool latency p99
histogram_quantile(0.99, regulus_mcp_tools_latency_bucket)
```

### Audit Logging

```java
@Aspect
@Component
public class McpAuditAspect {

    @Around("@annotation(Tool)")
    public Object auditToolCall(ProceedingJoinPoint pjp) throws Throwable {
        String toolName = getToolName(pjp);
        Object[] args = pjp.getArgs();

        auditLogger.logToolInvocation(toolName, args);

        try {
            Object result = pjp.proceed();
            auditLogger.logToolSuccess(toolName);
            return result;
        } catch (Exception e) {
            auditLogger.logToolError(toolName, e);
            throw e;
        }
    }
}
```

## Testing

```java
@SpringBootTest
class McpToolTest {

    @Autowired
    private McpServer mcpServer;

    @Test
    void shouldListTools() {
        List<ToolDefinition> tools = mcpServer.listTools();

        assertThat(tools)
            .extracting(ToolDefinition::name)
            .contains("get_account_balance", "get_transactions");
    }

    @Test
    void shouldExecuteTool() {
        JsonNode args = objectMapper.createObjectNode()
            .put("accountId", "12345678");

        StepVerifier.create(mcpServer.callTool("get_account_balance", args))
            .assertNext(result -> {
                assertThat(result.get("balance")).isNotNull();
            })
            .verifyComplete();
    }
}
```

## Best Practices

1. **Clear descriptions** - Write clear tool and parameter descriptions
2. **Validate inputs** - Use schema validation for all parameters
3. **Handle errors gracefully** - Return meaningful error messages
4. **Limit scope** - Each tool should do one thing well
5. **Monitor usage** - Track tool invocations and errors
6. **Secure access** - Apply appropriate authorization
