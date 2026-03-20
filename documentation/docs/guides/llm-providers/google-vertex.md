# Google Vertex AI

Google Vertex AI with Gemini models is the recommended provider for UK financial services due to the `europe-west2` (London) region.

## Prerequisites

1. Google Cloud project with billing enabled
2. Vertex AI API enabled
3. Service account with appropriate permissions

## Setup

### Enable the API

```bash
gcloud services enable aiplatform.googleapis.com
```

### Create Service Account

```bash
# Create service account
gcloud iam service-accounts create regulus-agent \
    --display-name="Regulus Agent"

# Grant Vertex AI User role
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
    --member="serviceAccount:regulus-agent@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
    --role="roles/aiplatform.user"

# Download key
gcloud iam service-accounts keys create service-account.json \
    --iam-account=regulus-agent@YOUR_PROJECT_ID.iam.gserviceaccount.com
```

### Set Environment Variables

```bash
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account.json"
export GOOGLE_CLOUD_PROJECT="your-project-id"
```

## Configuration

### Basic Configuration

```yaml title="application.yml"
regulus:
  llm:
    provider: gemini
    gemini:
      project-id: ${GOOGLE_CLOUD_PROJECT}
      location: europe-west2  # London region for UK data residency
      model: gemini-2.0-flash
```

### Full Configuration

```yaml title="application.yml"
regulus:
  llm:
    provider: gemini
    timeout: 60s
    gemini:
      project-id: ${GOOGLE_CLOUD_PROJECT}
      location: europe-west2
      model: gemini-2.0-flash

      # Model parameters
      temperature: 0.7
      top-p: 0.95
      top-k: 40
      max-output-tokens: 8192

      # Safety settings
      safety-settings:
        harassment: BLOCK_MEDIUM_AND_ABOVE
        hate-speech: BLOCK_MEDIUM_AND_ABOVE
        sexually-explicit: BLOCK_MEDIUM_AND_ABOVE
        dangerous-content: BLOCK_MEDIUM_AND_ABOVE

      # Streaming
      streaming:
        enabled: true
        chunk-size: 1024
```

## Available Models

| Model | Use Case | Max Tokens |
|-------|----------|------------|
| `gemini-2.0-flash` | Fast, efficient responses | 8,192 |
| `gemini-1.5-flash` | Balanced performance | 8,192 |
| `gemini-1.5-pro` | Complex reasoning | 8,192 |

## Usage Examples

### Basic Chat

```java
@Service
public class GeminiService {

    private final LlmClient llmClient;

    public Mono<String> chat(String message) {
        return llmClient.chat(message)
            .map(ChatResponse::content);
    }
}
```

### With System Prompt

```java
public Mono<String> chatWithContext(String userMessage) {
    List<ChatMessage> messages = List.of(
        ChatMessage.system("You are a helpful banking assistant."),
        ChatMessage.user(userMessage)
    );

    return llmClient.chat(messages)
        .map(ChatResponse::content);
}
```

### Streaming Responses

```java
public Flux<String> streamChat(String message) {
    return llmClient.streamChat(message)
        .map(ChatChunk::content);
}
```

### Function Calling

```java
public Mono<String> chatWithTools(String message, List<Tool> tools) {
    ChatRequest request = ChatRequest.builder()
        .messages(List.of(ChatMessage.user(message)))
        .tools(tools)
        .build();

    return llmClient.chat(request)
        .flatMap(response -> {
            if (response.hasToolCall()) {
                // Execute tool and continue
                return executeToolAndContinue(response);
            }
            return Mono.just(response.content());
        });
}
```

## UK Data Residency

The `europe-west2` region ensures:

- All data processed in London
- Compliant with UK GDPR
- Meets FCA data residency expectations

!!! warning "Region Selection"
    Always use `europe-west2` for UK financial services workloads. Other regions may not meet regulatory requirements.

## Monitoring

### Metrics Available

- `regulus.llm.requests.total` - Total requests
- `regulus.llm.latency` - Request latency
- `regulus.llm.tokens.input` - Input tokens
- `regulus.llm.tokens.output` - Output tokens
- `regulus.llm.errors` - Error count by type

### Example Dashboard Query

```promql
rate(regulus_llm_requests_total{provider="gemini"}[5m])
```

## Cost Optimization

1. **Use Flash models** for simple tasks
2. **Enable caching** for repeated queries
3. **Set appropriate max tokens** to avoid overuse
4. **Monitor token usage** with observability

## Troubleshooting

### Authentication Errors

```
UNAUTHENTICATED: Request had invalid authentication credentials
```

**Solution**: Verify `GOOGLE_APPLICATION_CREDENTIALS` points to a valid service account key.

### Region Errors

```
Location europe-west2 is not supported
```

**Solution**: Ensure Vertex AI is available in your selected region and the API is enabled.

### Quota Errors

```
RESOURCE_EXHAUSTED: Quota exceeded
```

**Solution**: Request quota increase in Google Cloud Console or implement rate limiting.
