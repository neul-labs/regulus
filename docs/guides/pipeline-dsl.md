# Pipelines DSL

Regulus offers twin declarative options—Kotlin DSL and YAML—to describe retrieval-augmented generation (RAG), tool orchestration, policy requirements, and planner selection. Both compilers emit Spring beans that ADK agents consume at runtime, keeping Java services free from plumbing boilerplate.

## Design Goals

- **Ergonomic**: Reads like popular Python frameworks while compiling to type-safe Java/Spring constructs.
- **Consistent**: Kotlin and YAML map to the same internal model, ensuring identical behavior irrespective of authoring format.
- **Secure**: DSL objects integrate with policy guards and privacy filters automatically.
- **Extensible**: Teams can register custom retrieval backends, tool references, or planners.

## Kotlin DSL

```kotlin
aiPipeline("kycAssistant") {
  retrieval {
    fromPgVector { topK = 6; namespace = "kyc" }
    privacy { pseudonymise("customerId"); retentionDays = 30 }
  }
  tools {
    tool("sanctionsScreen") { ref = SanctionsClient::class }
    tool("pepLookup") { ref = PepService::class }
    tool("vulnerabilityClassifier") {
      ref = VulnerabilityClassifier::class
      outputs { flag("vulnerabilityFlag") }
    }
  }
  policy { denyIfMissing("consent"); require("purpose=KYC") }
  planner { type = "react" }
}
```

- Functions such as `retrieval`, `tools`, `policy`, and `planner` map directly to ADK configurations.
- The DSL performs compile-time validation where possible (e.g., missing namespaces, unsupported planners).
- Safety models can be declared as tools that emit structured flags (`vulnerabilityFlag`, `misSellScore`). Policy blocks consume these outputs to enforce Consumer Duty or conduct rules before responses leave the agent.

## YAML DSL

```yaml
pipelines:
  - name: kycAssistant
    retrieval:
      store: pgvector
      topK: 6
      privacy: { pseudonymise: ["customerId"], retentionDays: 30 }
    tools: [sanctionsScreen, pepLookup]
    policy: { require: ["purpose=KYC", "consent=true"] }
    planner: react
```

- YAML files are validated on startup; errors surface with actionable messages (file, line, offending property).
- Environment-specific overrides are supported through Spring profile-based property sources.

## Compilation Pipeline

1. YAML documents parse into the same intermediate representation as the Kotlin DSL.
2. The representation registers Spring beans for retrieval sources, privacy filters, tool wiring, policy requirements, and planners.
3. The agent starter auto-wires these beans into ADK agent definitions.

## Validation & Testing

- DSL definitions participate in CI via the Gradle plugin, ensuring new pipelines keep passing eval quality gates.
- Unit tests can load DSL fragments with Spring’s `ApplicationContextRunner` to verify wiring and guard behavior.

## Extension Hooks

- Register custom retrieval stores (e.g., Elastic, proprietary vector DB) by implementing the retrieval interface and referencing it in DSL definitions.
- Extend policy vocabularies by mapping new keywords to guard beans inside the governance starter.
- Add planners beyond `react` (e.g., tree-of-thought) by contributing planner factories.
