# ADK extension architecture

How Regulus plugs into ADK's official extension surface — `App`, plugins,
callbacks, and services — without inventing a parallel runtime.

> Supersedes the LangChain4j-centric content in `adk-mcp-a2a.md`.

## The picture

```
                    ┌──────────────────────────────────────────┐
                    │            ADK App (1.x)                 │
                    │  ┌────────────────────────────────────┐  │
                    │  │ Plugin chain (BasePlugin)          │  │
                    │  │ ── RegulusDataResidencyPlugin      │  │  startup fail-closed
                    │  │ ── RegulusKillSwitchPlugin         │  │  BeforeAgentCallback
                    │  │ ── RegulusPolicyPlugin             │  │  BeforeModelCallback
                    │  │ ── RegulusPrivacyPlugin            │  │  Before/AfterModelCallback (mutating)
                    │  │ ── RegulusModelRiskPlugin          │  │  BeforeModelCallback
                    │  │ ── RegulusAuditPlugin              │  │  After*Callback
                    │  └────────────────────────────────────┘  │
                    │  ┌────────────────────────────────────┐  │
                    │  │ Services                           │  │
                    │  │ ── RegulusVertexAiSessionService   │  │  extends VertexAiSessionService
                    │  │ ── RegulusGcsArtifactService       │  │  extends GcsArtifactService
                    │  │ ── RegulusRetentionEventCompactor  │  │  implements EventCompactor
                    │  │ ── RegulusComplianceBaseComputer   │  │  implements BaseComputer
                    │  └────────────────────────────────────┘  │
                    └──────────────────────────────────────────┘
                                       │
                                       ▼
                    ┌──────────────────────────────────────────┐
                    │  com.google.adk:google-adk:1.2.0         │
                    │  com.google.adk:google-adk-dev (opt)     │
                    └──────────────────────────────────────────┘
```

## Request lifecycle with all Regulus plugins active

```
client request
   │
   ▼
[1] BeforeAgentCallback
       RegulusDataResidencyPlugin   — per-call residency re-check
       RegulusKillSwitchPlugin      — per-agent kill state
   │ (short-circuit on block)
   ▼
[2] BeforeModelCallback
       RegulusPolicyPlugin          — purpose, consent, Art. 22, Consumer Duty
       RegulusPrivacyPlugin         — PII redaction (mutates request)
       RegulusModelRiskPlugin       — tier ceiling check
   │
   ▼
   model call to Vertex AI
   │
   ▼
[3] AfterModelCallback
       RegulusPrivacyPlugin         — output re-redaction
       RegulusAuditPlugin           — emit model-call event
   │
   ▼
   (optional) tool call(s)
       BeforeToolCallback
           RegulusPolicyPlugin      — tool allowlist + policy
           RegulusModelRiskPlugin   — code-executor tier check
       AfterToolCallback
           RegulusAuditPlugin       — emit tool-call event
   │
   ▼
[4] AfterAgentCallback
       RegulusAuditPlugin           — emit agent-completion event
       RegulusRetentionEventCompactor — eventually compacts older events
   │
   ▼
response to client
```

## A2A envelope

```
[caller agent]                                    [callee agent]
                                                       │
   RegulusRemoteA2AAgent.send(req)                     │
       Regulus envelope on request             ────►   RegulusAgentExecutor.execute(req)
       audit a2a-outbound                              Regulus envelope on inbound
                                                       audit a2a-inbound
                                                       │
                                                       (full lifecycle as above)
                                                       │
       audit a2a-inbound-response             ◄────    Regulus envelope on response
       Regulus envelope on response                    audit a2a-response
                                                       │
   return to caller agent's continuation               │
```

Both ends emit audit events with the same `correlation_id` so the trail
reconstructs end-to-end.

## Why this is the official extension surface

ADK 1.0 (Mar 2026) shipped the `App` container + `BasePlugin` system
explicitly as "an aspect-oriented way to intercept and modify agent, tool,
and LLM behaviors globally." Google's own reference plugins
(`LoggingPlugin`, `ContextFilterPlugin`, `GlobalInstructionPlugin`) sit
on the same seam. Regulus implements its controls on that seam — which is
why we can call it "the default ADK extension for EU + UK compliance"
without overreaching.

Spring AOP, bytecode rewriting, or a parallel interception layer would all
deliver similar behaviour, but at the cost of either (a) coupling to Spring
or (b) shadowing ADK's own evolution. The plugin approach co-evolves.

## See also

- [Plugins](../../documentation/docs/plugins/index.md)
- [Services](../../documentation/docs/services/index.md)
- [ADR-006](../decisions/ADR-006-adk-primary-runtime-and-extension-model.md)
