# ADR-004: LangChain4j for LLM Abstraction

## Status
Accepted

## Date
2025-01-08

## Context

The Regulus platform needs to integrate with multiple Large Language Model (LLM) providers:
- **Google Vertex AI** (Gemini models) - Primary for UK data residency (europe-west2)
- **Azure OpenAI** - Alternative with UK region (uksouth)
- **AWS Bedrock** (Anthropic Claude) - Alternative with UK region (eu-west-2)
- **OpenAI** - For development/testing (with data residency considerations)

Building direct integrations with each provider presents challenges:
1. **API differences**: Each provider has different request/response formats
2. **Authentication**: GCP uses OAuth, AWS uses SigV4, Azure uses API keys
3. **Feature parity**: Streaming, function calling, vision capabilities vary
4. **Maintenance burden**: API changes require updates across all integrations
5. **Testing complexity**: Need to mock each provider separately

The platform needs an abstraction layer that:
- Provides uniform API across providers
- Handles provider-specific authentication
- Supports streaming responses
- Enables easy provider switching
- Maintains type safety (Java platform)

## Decision

We will use **LangChain4j** as the LLM abstraction layer with the following implementation:

### Core Design

1. **Unified ChatLanguageModel interface**: Single API for all providers
2. **Provider-specific modules**: langchain4j-vertex-ai, langchain4j-azure-openai, etc.
3. **Spring Boot integration**: Auto-configuration via langchain4j-spring-boot-starter
4. **Streaming support**: StreamingChatLanguageModel for token-by-token responses
5. **Governance wrapping**: Regulus interceptors applied to LangChain4j clients

### Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Regulus LLM Integration                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                    RegulusLlmClient                          │  │
│  │                                                              │  │
│  │   • Data Residency Enforcement                               │  │
│  │   • Audit Logging                                            │  │
│  │   • Kill Switch Integration                                  │  │
│  │   • Rate Limiting                                            │  │
│  │                                                              │  │
│  └──────────────────────────────────┬───────────────────────────┘  │
│                                     │                               │
│                                     ▼                               │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │              LangChain4j ChatLanguageModel                   │  │
│  │                                                              │  │
│  │   chat(messages) ──▶ AiMessage                               │  │
│  │   generate(prompt) ──▶ Response                              │  │
│  │                                                              │  │
│  └──────────────────────────────────┬───────────────────────────┘  │
│                                     │                               │
│              ┌──────────────────────┼──────────────────────┐       │
│              ▼                      ▼                      ▼       │
│      ┌─────────────┐       ┌─────────────┐        ┌─────────────┐ │
│      │  Vertex AI  │       │ Azure OpenAI│        │ AWS Bedrock │ │
│      │  (Gemini)   │       │  (GPT-4)    │        │  (Claude)   │ │
│      └─────────────┘       └─────────────┘        └─────────────┘ │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Provider Configuration

```yaml
regulus:
  ai:
    llm:
      primary-provider: gemini

      gemini:
        enabled: true
        project-id: ${GCP_PROJECT_ID}
        location: europe-west2  # UK data residency
        model-name: gemini-1.5-pro

      azure-openai:
        enabled: true
        endpoint: https://uksouth.api.cognitive.microsoft.com
        deployment-name: gpt-4
        api-key: ${AZURE_OPENAI_KEY}

      bedrock:
        enabled: true
        region: eu-west-2  # AWS London
        model-id: anthropic.claude-3-sonnet
```

### Streaming Implementation

```java
@Service
public class RegulusLlmClient {

    private final StreamingChatLanguageModel streamingModel;
    private final DataResidencyEnforcer residencyEnforcer;
    private final KillSwitchManager killSwitch;

    public Flux<String> streamChat(List<ChatMessage> messages) {
        // Check kill switch
        if (killSwitch.isActive()) {
            return Flux.error(new KillSwitchActiveException());
        }

        // Verify data residency
        residencyEnforcer.validateEndpoint(streamingModel.getEndpoint());

        // Stream with audit
        return Flux.create(sink -> {
            streamingModel.generate(messages, new StreamingResponseHandler<>() {
                @Override
                public void onNext(String token) {
                    sink.next(token);
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    auditLogger.logCompletion(response);
                    sink.complete();
                }

                @Override
                public void onError(Throwable error) {
                    auditLogger.logError(error);
                    sink.error(error);
                }
            });
        });
    }
}
```

### Provider Fallback

```java
@Configuration
public class LlmFallbackConfiguration {

    @Bean
    @Primary
    public ChatLanguageModel resilientChatModel(
            @Qualifier("gemini") ChatLanguageModel primary,
            @Qualifier("azureOpenAi") ChatLanguageModel secondary,
            @Qualifier("bedrock") ChatLanguageModel tertiary
    ) {
        return FallbackChatLanguageModel.builder()
            .primary(primary)
            .fallbacks(List.of(secondary, tertiary))
            .fallbackCondition(e -> e instanceof ServiceUnavailableException)
            .build();
    }
}
```

## Consequences

### Positive

1. **Provider agnostic**: Same code works with any supported provider
2. **Easy switching**: Change provider via configuration, not code
3. **Mature library**: Active development, good documentation
4. **Spring integration**: Native Spring Boot auto-configuration
5. **Feature rich**: Streaming, function calling, embeddings, RAG support
6. **Type safety**: Full Java type system support

### Negative

1. **Abstraction cost**: Some provider-specific features may be hidden
2. **Version coupling**: Must update LangChain4j to get provider updates
3. **Learning curve**: Teams must learn LangChain4j abstractions
4. **Dependency size**: Brings in transitive dependencies

### Mitigations

| Concern | Mitigation |
|---------|------------|
| Hidden features | Use provider-specific extensions when needed |
| Version lag | Monitor LangChain4j releases, maintain compatibility matrix |
| Learning curve | Provide internal documentation, examples |
| Dependencies | Use BOM for consistent versions, exclude unused modules |

## Alternatives Considered

### 1. Direct Provider SDKs

**Pros**: Full feature access, no abstraction overhead
**Cons**: Different API per provider, high maintenance burden
**Decision**: Rejected - doesn't scale with multiple providers

### 2. Custom Abstraction Layer

**Pros**: Full control, no external dependency
**Cons**: Significant development effort, maintenance burden
**Decision**: Rejected - reinventing the wheel

### 3. Spring AI

**Pros**: Spring native, good integration
**Cons**: Less mature than LangChain4j, fewer providers
**Decision**: Considered for future - may migrate when mature

### 4. Semantic Kernel (Java)

**Pros**: Microsoft backing, good Azure integration
**Cons**: Primarily .NET focused, Java version less mature
**Decision**: Rejected - Java support insufficient

### 5. Haystack

**Pros**: Good for RAG pipelines
**Cons**: Python only, no Java version
**Decision**: Rejected - platform is Java-based

## Version Compatibility

| Regulus Version | LangChain4j Version | Java | Spring Boot |
|-----------------|---------------------|------|-------------|
| 1.0.x | 0.35.x | 21+ | 3.3.x |
| 1.1.x | 0.36.x | 21+ | 3.4.x |

## References

- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [LangChain4j GitHub](https://github.com/langchain4j/langchain4j)
- [Spring Boot Integration](https://docs.langchain4j.dev/integrations/frameworks/spring-boot)
- [ADK/MCP/A2A Integration Guide](../architecture/adk-mcp-a2a.md)
- [LLM Integration Guide](../guides/quickstart-tutorial.md)
