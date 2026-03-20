# ADK Agent with Regulus Integration

```mermaid
flowchart TB
    subgraph DevService["Spring Boot Service"]
        A[Application Boot<br/>`@EnableAiAgents`] --> B[Regulus Auto-Config]
        B --> C[Pipelines DSL Beans<br/>(Kotlin/YAML)]
        B --> D[PolicyGuard AOP<br/>(LEI, Purpose, Consent)]
        B --> E[PrivacyFilter<br/>(Redaction, Tags)]
        B --> R[KillSwitch Interceptor<br/>ConfigHub/Vault Toggle]
        B --> F[Model Router<br/>SLM ➜ LLM]
        B --> G[Observability Hooks<br/>Micrometer/OTEL, Kafka Audit]
    end

    C --> H[ADK Agent<br/>(Planner, Memory, Tools)]
    D --> H
    E --> H
    F --> H
    G --> H

    subgraph Interop["Interop Layers"]
        I[MCP Client<br/>Tool Discovery/Calls]
        J[MCP Server<br/>Expose Validators]
        K[A2A Client<br/>External Agents]
        L[A2A Server<br/>Published Agent]
    end

    H <-->|Tool Calls| I
    H -->|Expose Capabilities| J
    H <-->|Collaborate| K
    H -->|Publish| L

    F --> M[SLM Runtime<br/>(DJL, On-Prem Models)]
    F --> N[LLM Provider<br/>(Vendor API)]

    D --> O[Enterprise Model Inventory<br/>SS1/23 Registry]
    G --> P[Telemetry Stack<br/>OTEL ➜ Grafana/Splunk]
    R --> S[Kill Switch Config Store<br/>(ConfigHub/Vault)]
    R --> G

    subgraph PythonServices["Hybrid Python Services"]
        Q[Eval Container<br/>(RAGAS, DeepEval)]
    end

    H -->|@AiGate Metrics| Q
    Q -->|Quality Signals| G
```

## Diagram Notes

- The Regulus starters auto-configure the ADK agent, policy/privacy aspects, model routing, and observability.
- MCP and A2A connectors provide bidirectional interoperability with external validators and partner agents.
- Model routing prefers the secure local SLM and escalates to vendor LLMs with logged decisions.
- Governance and telemetry integrations capture artefacts required by SS1/23, PS21/3, and SS2/21.
- Eval and red-team checks run out-of-band via the Python container, feeding gating metrics back into observability.
