# LLM Providers

Regulus provides a provider-agnostic LLM interface, allowing you to switch between providers with configuration changes only.

## Supported Providers

| Provider | Models | UK Data Residency | Notes |
|----------|--------|-------------------|-------|
| **Google Vertex AI** | Gemini 2.0, 1.5 | `europe-west2` | Recommended for UK FS |
| **OpenAI** | GPT-4o, GPT-4 | No | US-based processing |
| **Anthropic** | Claude 3.5, 3 | No | US-based processing |
| **Azure OpenAI** | GPT-4, GPT-3.5 | `uksouth`, `ukwest` | Enterprise option |

## Choosing a Provider

### For UK Financial Services

**Google Vertex AI** is recommended because:

- `europe-west2` region is in London
- First-party Google infrastructure
- Strong compliance certifications

**Azure OpenAI** is an alternative:

- `uksouth` and `ukwest` regions available
- Enterprise Azure compliance
- Familiar for Microsoft-centric organisations

### For Development/Testing

Any provider works for non-production use where data residency is less critical.

## Configuration Overview

All providers follow the same configuration pattern:

```yaml
regulus:
  llm:
    provider: <provider-name>  # gemini, openai, anthropic, azure-openai
    <provider-name>:
      # Provider-specific settings
```

## Provider-Specific Guides

- [Google Vertex AI](google-vertex.md)
- [OpenAI](openai.md)
- [Anthropic](anthropic.md)
- [Azure OpenAI](azure-openai.md)

## Common Configuration

### Timeouts

```yaml
regulus:
  llm:
    timeout: 30s
    connect-timeout: 10s
```

### Retry Policy

```yaml
regulus:
  llm:
    retry:
      max-attempts: 3
      backoff:
        initial: 1s
        multiplier: 2
        max: 10s
```

### Token Limits

```yaml
regulus:
  llm:
    max-tokens: 4096
    max-input-tokens: 8192
```

## Programmatic Provider Selection

For dynamic provider selection:

```java
@Service
public class MultiProviderService {

    private final Map<String, LlmClient> clients;

    public MultiProviderService(
            @Qualifier("geminiClient") LlmClient gemini,
            @Qualifier("openaiClient") LlmClient openai) {
        this.clients = Map.of(
            "gemini", gemini,
            "openai", openai
        );
    }

    public Mono<String> chat(String provider, String message) {
        return clients.get(provider).chat(message);
    }
}
```
