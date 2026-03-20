# OpenAI

Integration with OpenAI's GPT models.

!!! warning "Data Residency"
    OpenAI processes data in the United States. For UK financial services with strict data residency requirements, consider Google Vertex AI or Azure OpenAI with UK regions.

## Prerequisites

1. OpenAI account at [platform.openai.com](https://platform.openai.com)
2. API key generated

## Setup

### Get API Key

1. Log in to [platform.openai.com](https://platform.openai.com)
2. Navigate to API Keys
3. Create a new secret key

### Set Environment Variable

```bash
export OPENAI_API_KEY="sk-..."
```

## Configuration

### Basic Configuration

```yaml title="application.yml"
regulus:
  llm:
    provider: openai
    openai:
      api-key: ${OPENAI_API_KEY}
      model: gpt-4o
```

### Full Configuration

```yaml title="application.yml"
regulus:
  llm:
    provider: openai
    timeout: 60s
    openai:
      api-key: ${OPENAI_API_KEY}
      model: gpt-4o
      organization-id: ${OPENAI_ORG_ID:}  # Optional

      # Model parameters
      temperature: 0.7
      top-p: 1.0
      frequency-penalty: 0.0
      presence-penalty: 0.0
      max-tokens: 4096

      # Streaming
      streaming:
        enabled: true
```

## Available Models

| Model | Use Case | Context Window |
|-------|----------|----------------|
| `gpt-4o` | Best overall performance | 128K |
| `gpt-4o-mini` | Cost-effective, fast | 128K |
| `gpt-4-turbo` | Complex reasoning | 128K |
| `gpt-4` | Stable, reliable | 8K |

## Usage Examples

### Basic Chat

```java
@Service
public class OpenAIService {

    private final LlmClient llmClient;

    public Mono<String> chat(String message) {
        return llmClient.chat(message)
            .map(ChatResponse::content);
    }
}
```

### With Conversation History

```java
public Mono<String> chatWithHistory(List<ChatMessage> history, String newMessage) {
    List<ChatMessage> messages = new ArrayList<>(history);
    messages.add(ChatMessage.user(newMessage));

    return llmClient.chat(messages)
        .map(ChatResponse::content);
}
```

### JSON Mode

```java
public Mono<JsonNode> chatJson(String message) {
    ChatRequest request = ChatRequest.builder()
        .messages(List.of(ChatMessage.user(message)))
        .responseFormat(ResponseFormat.JSON_OBJECT)
        .build();

    return llmClient.chat(request)
        .map(response -> objectMapper.readTree(response.content()));
}
```

### Function Calling

```java
Tool calculateTool = Tool.builder()
    .name("calculate")
    .description("Perform mathematical calculations")
    .parameters(Map.of(
        "type", "object",
        "properties", Map.of(
            "expression", Map.of(
                "type", "string",
                "description", "Mathematical expression to evaluate"
            )
        ),
        "required", List.of("expression")
    ))
    .build();

public Mono<String> chatWithCalculator(String message) {
    return llmClient.chat(
        ChatRequest.builder()
            .messages(List.of(ChatMessage.user(message)))
            .tools(List.of(calculateTool))
            .build()
    ).flatMap(this::handleResponse);
}
```

## Streaming

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> streamChat(@RequestParam String message) {
    return llmClient.streamChat(message)
        .map(chunk -> ServerSentEvent.<String>builder()
            .data(chunk.content())
            .build());
}
```

## Rate Limiting

OpenAI has rate limits based on your tier. Regulus handles this with retry logic:

```yaml
regulus:
  llm:
    openai:
      retry:
        max-attempts: 3
        on-rate-limit:
          enabled: true
          initial-delay: 1s
          max-delay: 60s
```

## Cost Management

### Token Tracking

```java
@Service
public class CostTrackingService {

    private final MeterRegistry registry;

    public void recordUsage(ChatResponse response) {
        registry.counter("openai.tokens.input")
            .increment(response.usage().inputTokens());
        registry.counter("openai.tokens.output")
            .increment(response.usage().outputTokens());
    }
}
```

### Estimated Costs (as of 2024)

| Model | Input (per 1M tokens) | Output (per 1M tokens) |
|-------|----------------------|------------------------|
| gpt-4o | $5.00 | $15.00 |
| gpt-4o-mini | $0.15 | $0.60 |
| gpt-4-turbo | $10.00 | $30.00 |

## Troubleshooting

### Authentication Errors

```
Error code: 401 - Invalid API key
```

**Solution**: Verify your API key is correct and has not been revoked.

### Rate Limit Errors

```
Error code: 429 - Rate limit exceeded
```

**Solution**: Implement exponential backoff or upgrade your OpenAI plan.

### Model Access Errors

```
Error code: 404 - Model not found
```

**Solution**: Ensure your account has access to the requested model.
