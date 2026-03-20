# Regulus Architecture

Regulus wraps Google's Agent Development Kit (ADK), Model Context Protocol (MCP), and Agent-to-Agent (A2A) ecosystem with Spring Boot conventions, compliance guardrails, and developer tooling suited to regulated banking environments.

## Core Components

- **Spring Boot Starters**: Auto-configuration layers that expose agent, governance, payments, safety, and eval capabilities through familiar Spring annotations and configuration properties.
- **Policy & Privacy Layer**: Aspect-oriented guards that enforce LEI/purpose/consent requirements, redact personal data, and tag payloads with retention metadata.
- **Safety Model Layer**: Optional on-prem SLM/classifier toolset (e.g., vulnerability detection, mis-sell risk scoring) that feeds policy guards with structured risk signals before responses are emitted.
- **Observability & Audit**: Micrometer and OpenTelemetry instrumentation plus Kafka audit streams for `llm.call`, `route.slm|llm`, `mcp.tool`, and `a2a.call` events.
- **Kill Switch Layer**: Interceptors tied to the bank’s toggle service (e.g., ConfigHub or Vault-backed Spring Cloud Config) enable break-glass disablement of agents, tools, or external connectors with full audit trails.
- **Pipelines DSL**: Kotlin and YAML parsers that emit Spring beans describing retrieval-augmented generation (RAG), tool invocation, policies, and planners.
- **Model Router**: Strategy that prefers secure local SLMs via DJL and escalates to vendor LLMs when confidence thresholds or guardrails require.
- **Interop Connectors**: MCP client/server and A2A client/server adapters that enable tool discovery, remote invocation, and cross-team agent collaboration.

## Flow Overview

1. **Application Bootstrap**: Developers pull in the BOM and relevant starter(s). Auto-configured beans bring in agents, memory stores, planners, and governance policies with sensible defaults.
2. **Pipeline Declaration**: Teams describe RAG and tool flows using the DSL. Parsers translate declarations into Spring beans that ADK agents consume.
3. **Policy Enforcement**: Incoming tool calls or external requests pass through `PolicyGuard` and `PrivacyFilter` aspects ensuring LEI, purpose codes, consent, redaction, and retention tagging.
4. **Model Selection**: The router evaluates the request, runs on SLM first, and escalates to LLMs if needed. Routing decisions and model metadata are logged for audit and cost tracking.
5. **Interop Calls**: Agents discover MCP toolsets or collaborate with other agents via A2A. Both inbound and outbound traffic is wrapped with RBAC scopes, OAuth, and mTLS.
6. **Monitoring & Governance**: Micrometer metrics, OTEL traces, Kafka audit events, and governance registries capture lineage, approvals, and review cadences mandated by SS1/23 and related regulations.

## Deployment Considerations

- **Runtime**: Standard JVM services on Kubernetes, sharing bank observability, Vault/HSM, and networking controls.
- **Local Development**: DevContainer plus `make run` spin up stubs (LLM, vector DB, eval service, OTEL, Kibana/Grafana) for quick experimentation.
- **CI/CD**: Gradle plugin triggers eval/red-team tests with the Python container; builds fail if quality gates (faithfulness, toxicity, etc.) are not met.

## Extensibility

- Additional starters can be layered for domain-specific capabilities (e.g., trade surveillance) while reusing the common governance/observability scaffolding.
- DSL parsers support custom extensions by registering new retrieval sources, tool definitions, or planner types.
- MCP/A2A connectors accommodate external vendors or internal teams by publishing versioned contracts and enforcing rate limits and quotas.
