# Anthropic

Integration with Anthropic's Claude models.

!!! warning "Data Residency"
    Anthropic processes data in the United States. For UK financial services with strict data residency requirements, consider Google Vertex AI or Azure OpenAI with UK regions.

## Prerequisites

1. Anthropic account at [console.anthropic.com](https://console.anthropic.com)
2. API key generated

## Setup

### Get API Key

1. Log in to [console.anthropic.com](https://console.anthropic.com)
2. Navigate to API Keys
3. Create a new key

### Set Environment Variable

```bash
export ANTHROPIC_API_KEY="sk-ant-..."
```

## Configuration

### Basic Configuration

```yaml title="application.yml"
regulus:
  llm:
    provider: anthropic
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      model: claude-3-5-sonnet-20241022
```

### Full Configuration

```yaml title="application.yml"
regulus:
  llm:
    provider: anthropic
    timeout: 60s
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      model: claude-3-5-sonnet-20241022

      # Model parameters
      temperature: 0.7
      top-p: 0.9
      top-k: 50
      max-tokens: 4096

      # Streaming
      streaming:
        enabled: true
```

## Available Models

| Model | Use Case | Context Window |
|-------|----------|----------------|
| `claude-3-5-sonnet-20241022` | Best balance of speed and capability | 200K |
| `claude-3-opus-20240229` | Most capable, complex tasks | 200K |
| `claude-3-sonnet-20240229` | Balanced performance | 200K |
| `claude-3-haiku-20240307` | Fastest, simple tasks | 200K |

## Usage Examples

### Basic Chat

```java
@Service
public class ClaudeService {

    private final LlmClient llmClient;

    public Mono<String> chat(String message) {
        return llmClient.chat(message)
            .map(ChatResponse::content);
    }
}
```

### With System Prompt

Claude handles system prompts distinctly:

```java
public Mono<String> chatWithSystem(String systemPrompt, String userMessage) {
    List<ChatMessage> messages = List.of(
        ChatMessage.system(systemPrompt),
        ChatMessage.user(userMessage)
    );

    return llmClient.chat(messages)
        .map(ChatResponse::content);
}
```

### Multi-turn Conversation

```java
public Mono<String> continueConversation(
        List<ChatMessage> history,
        String newMessage) {

    List<ChatMessage> messages = new ArrayList<>(history);
    messages.add(ChatMessage.user(newMessage));

    return llmClient.chat(messages)
        .doOnNext(response -> {
            history.add(ChatMessage.user(newMessage));
            history.add(ChatMessage.assistant(response.content()));
        })
        .map(ChatResponse::content);
}
```

### Tool Use

```java
Tool weatherTool = Tool.builder()
    .name("get_weather")
    .description("Get current weather for a location")
    .inputSchema(Map.of(
        "type", "object",
        "properties", Map.of(
            "location", Map.of(
                "type", "string",
                "description", "City name"
            )
        ),
        "required", List.of("location")
    ))
    .build();

public Mono<String> chatWithWeather(String message) {
    return llmClient.chat(
        ChatRequest.builder()
            .messages(List.of(ChatMessage.user(message)))
            .tools(List.of(weatherTool))
            .build()
    ).flatMap(this::handleToolResponse);
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

## Claude-Specific Features

### Extended Thinking

For complex reasoning tasks:

```yaml
regulus:
  llm:
    anthropic:
      extended-thinking:
        enabled: true
        budget-tokens: 10000
```

### Vision (Image Input)

```java
public Mono<String> analyzeImage(byte[] imageBytes, String question) {
    ChatMessage imageMessage = ChatMessage.builder()
        .role(Role.USER)
        .content(List.of(
            ContentBlock.image(imageBytes, "image/png"),
            ContentBlock.text(question)
        ))
        .build();

    return llmClient.chat(List.of(imageMessage))
        .map(ChatResponse::content);
}
```

## Rate Limits

Anthropic has tier-based rate limits:

| Tier | Requests/min | Tokens/min |
|------|--------------|------------|
| Free | 5 | 10,000 |
| Build | 50 | 40,000 |
| Scale | 1,000 | 400,000 |

Configure retry behavior:

```yaml
regulus:
  llm:
    anthropic:
      retry:
        max-attempts: 3
        on-rate-limit:
          enabled: true
          respect-retry-after: true
```

## Cost Management

### Token Tracking

```java
@Service
public class UsageTracker {

    private final MeterRegistry registry;

    public void track(ChatResponse response) {
        registry.counter("anthropic.tokens.input")
            .increment(response.usage().inputTokens());
        registry.counter("anthropic.tokens.output")
            .increment(response.usage().outputTokens());
    }
}
```

### Estimated Costs (as of 2024)

| Model | Input (per 1M tokens) | Output (per 1M tokens) |
|-------|----------------------|------------------------|
| claude-3-5-sonnet | $3.00 | $15.00 |
| claude-3-opus | $15.00 | $75.00 |
| claude-3-haiku | $0.25 | $1.25 |

## Troubleshooting

### Authentication Errors

```
Error: Invalid API key
```

**Solution**: Verify your API key is correct and active.

### Rate Limit Errors

```
Error: Rate limit exceeded
```

**Solution**: Implement backoff or upgrade your tier.

### Context Length Errors

```
Error: Input too long
```

**Solution**: Reduce input size or use a model with larger context window.
