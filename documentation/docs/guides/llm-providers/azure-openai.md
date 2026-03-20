# Azure OpenAI

Azure OpenAI provides enterprise-grade OpenAI models with Azure infrastructure, including UK region options.

## Prerequisites

1. Azure subscription
2. Azure OpenAI resource created
3. Model deployed in your resource

## Setup

### Create Azure OpenAI Resource

```bash
# Create resource group
az group create --name regulus-rg --location uksouth

# Create Azure OpenAI resource
az cognitiveservices account create \
    --name regulus-openai \
    --resource-group regulus-rg \
    --kind OpenAI \
    --sku S0 \
    --location uksouth
```

### Deploy a Model

```bash
az cognitiveservices account deployment create \
    --name regulus-openai \
    --resource-group regulus-rg \
    --deployment-name gpt-4o \
    --model-name gpt-4o \
    --model-version "2024-05-13" \
    --model-format OpenAI \
    --sku-capacity 10 \
    --sku-name Standard
```

### Get Credentials

```bash
# Get endpoint
az cognitiveservices account show \
    --name regulus-openai \
    --resource-group regulus-rg \
    --query "properties.endpoint" -o tsv

# Get key
az cognitiveservices account keys list \
    --name regulus-openai \
    --resource-group regulus-rg \
    --query "key1" -o tsv
```

### Set Environment Variables

```bash
export AZURE_OPENAI_ENDPOINT="https://regulus-openai.openai.azure.com"
export AZURE_OPENAI_API_KEY="..."
```

## Configuration

### Basic Configuration

```yaml title="application.yml"
regulus:
  llm:
    provider: azure-openai
    azure-openai:
      endpoint: ${AZURE_OPENAI_ENDPOINT}
      api-key: ${AZURE_OPENAI_API_KEY}
      deployment-name: gpt-4o
```

### Full Configuration

```yaml title="application.yml"
regulus:
  llm:
    provider: azure-openai
    timeout: 60s
    azure-openai:
      endpoint: ${AZURE_OPENAI_ENDPOINT}
      api-key: ${AZURE_OPENAI_API_KEY}
      deployment-name: gpt-4o
      api-version: "2024-02-01"

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

### Using Managed Identity

For production, use Managed Identity instead of API keys:

```yaml title="application.yml"
regulus:
  llm:
    provider: azure-openai
    azure-openai:
      endpoint: ${AZURE_OPENAI_ENDPOINT}
      auth-type: managed-identity
      deployment-name: gpt-4o
```

```java
@Configuration
public class AzureConfig {

    @Bean
    public TokenCredential azureCredential() {
        return new DefaultAzureCredentialBuilder().build();
    }
}
```

## UK Data Residency

Azure OpenAI is available in UK regions:

| Region | Location | Code |
|--------|----------|------|
| UK South | London | `uksouth` |
| UK West | Cardiff | `ukwest` |

!!! tip "Recommended Region"
    Use `uksouth` for lowest latency from London-based infrastructure.

## Available Models

Models must be deployed before use. Available models vary by region:

| Model | Availability | Notes |
|-------|--------------|-------|
| gpt-4o | UK South | Latest, recommended |
| gpt-4-turbo | UK South | 128K context |
| gpt-4 | UK South | Stable |
| gpt-35-turbo | UK South | Cost-effective |

## Usage Examples

### Basic Chat

```java
@Service
public class AzureOpenAIService {

    private final LlmClient llmClient;

    public Mono<String> chat(String message) {
        return llmClient.chat(message)
            .map(ChatResponse::content);
    }
}
```

### With Conversation

```java
public Mono<String> chatWithHistory(List<ChatMessage> history, String newMessage) {
    List<ChatMessage> messages = new ArrayList<>(history);
    messages.add(ChatMessage.user(newMessage));

    return llmClient.chat(messages)
        .map(ChatResponse::content);
}
```

### Function Calling

```java
Tool searchTool = Tool.builder()
    .name("search_accounts")
    .description("Search customer accounts")
    .parameters(Map.of(
        "type", "object",
        "properties", Map.of(
            "query", Map.of(
                "type", "string",
                "description", "Search query"
            )
        ),
        "required", List.of("query")
    ))
    .build();

public Mono<String> chatWithSearch(String message) {
    return llmClient.chat(
        ChatRequest.builder()
            .messages(List.of(ChatMessage.user(message)))
            .tools(List.of(searchTool))
            .build()
    ).flatMap(this::handleToolResponse);
}
```

## Enterprise Features

### Content Filtering

Azure OpenAI includes built-in content filtering:

```yaml
regulus:
  llm:
    azure-openai:
      content-filter:
        # These are Azure defaults, customize as needed
        hate: medium
        self-harm: medium
        sexual: medium
        violence: medium
```

### Private Endpoints

For network isolation:

```yaml
regulus:
  llm:
    azure-openai:
      endpoint: https://regulus-openai.privatelink.openai.azure.com
      # Ensure your app can reach the private endpoint
```

### Diagnostic Logging

Enable Azure diagnostics:

```bash
az monitor diagnostic-settings create \
    --name regulus-diagnostics \
    --resource /subscriptions/.../regulus-openai \
    --logs '[{"category": "RequestResponse", "enabled": true}]' \
    --workspace /subscriptions/.../log-analytics-workspace
```

## Quota Management

Azure OpenAI has quota limits per region and model:

```bash
# Check quota
az cognitiveservices usage list \
    --location uksouth \
    --query "[?name.value=='OpenAI.Standard.gpt-4o']"
```

Configure throttling response:

```yaml
regulus:
  llm:
    azure-openai:
      retry:
        max-attempts: 3
        on-throttle:
          enabled: true
          respect-retry-after: true
```

## Cost Management

### Token Tracking

```java
@Service
public class AzureCostTracker {

    private final MeterRegistry registry;

    public void track(ChatResponse response) {
        registry.counter("azure.openai.tokens.input",
            "deployment", response.deployment())
            .increment(response.usage().inputTokens());
        registry.counter("azure.openai.tokens.output",
            "deployment", response.deployment())
            .increment(response.usage().outputTokens());
    }
}
```

### Cost Alerts

Set up Azure Cost Management alerts:

```bash
az consumption budget create \
    --budget-name regulus-openai-budget \
    --amount 1000 \
    --time-grain Monthly \
    --category Cost \
    --resource-group regulus-rg
```

## Troubleshooting

### Authentication Errors

```
Error: 401 Unauthorized
```

**Solutions**:
- Verify API key is correct
- Check Managed Identity has Cognitive Services User role
- Ensure endpoint URL is correct

### Deployment Not Found

```
Error: Deployment 'gpt-4o' not found
```

**Solution**: Verify deployment name matches exactly (case-sensitive).

### Quota Exceeded

```
Error: 429 Too Many Requests
```

**Solution**: Request quota increase or implement throttling.

### Region Availability

```
Error: Model not available in region
```

**Solution**: Check model availability in your region or deploy in a supported region.
