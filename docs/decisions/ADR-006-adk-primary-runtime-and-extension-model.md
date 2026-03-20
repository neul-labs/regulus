# ADR-006: Google ADK as the primary runtime and extension model

- Status: Accepted
- Date: 2026-03-25
- Supersedes: ADR-004 (LangChain4j as the primary LLM abstraction)

## Context

Regulus' original design used LangChain4j as the LLM abstraction and
implemented compliance controls via Spring AOP aspects. In March 2026
Google released ADK Java 1.0 with an `App` container, a `BasePlugin`
extension surface, official `SessionService` / `MemoryService` /
`ArtifactService` / `EventCompactor` / `BaseComputer` interfaces, and an
A2A protocol shipped via an official Java SDK. ADK 1.2.0 followed.

The presence of an official, opinionated extension surface changes the
right thing to do. We can either continue translating between
LangChain4j's abstractions and our compliance hooks, or implement Regulus
controls directly on ADK's official seams.

## Decision

Adopt Google ADK as the primary runtime. Implement every Regulus control
as a `BasePlugin` or a service-interface extension on ADK's official
seams. Demote LangChain4j (`regulus-ai-llm`) to "alternative runtime,
retained for legacy callers, not the recommended path."

Key consequences for the codebase:

- New modules — `regulus-ai-adk-plugins`, `regulus-ai-adk-services`,
  `regulus-ai-adk-a2a`, `regulus-ai-adk-spring-boot-starter` — implement
  the controls on `com.google.adk.*` interfaces.
- `regulus-ai-compliance` is pure Java (no ADK dependency) and is the
  data model the plugins consume.
- The Spring Boot starter is now *optional*. Regulus plugins work in any
  ADK app (Quarkus, Micronaut, plain `main`).
- The existing `regulus-ai-agents-spring-boot-starter` is kept, but new
  documentation leads with the ADK starter.

## Why we picked ADK over wrapping LangChain4j

- **Official extension surface.** `BasePlugin` is the seam Google
  endorses for cross-cutting concerns. Building on it means we don't get
  surprised by API changes — we co-evolve.
- **HITL primitive.** `ToolConfirmation` is the same shape Regulus' dual-
  control kill switch needs. No special-case API for users to learn.
- **A2A.** Official cross-agent protocol; Regulus envelope on top is
  natural.
- **Services with managed implementations.** `VertexAiSessionService`,
  `FirestoreSessionService`, `GcsArtifactService` — wrapping these is the
  high-leverage path to residency + CMEK enforcement.
- **Code executors.** ADK ships `ContainerCodeExecutor` and
  `VertexAiCodeExecutor`; Regulus' model risk plugin recognises them as
  high-risk by default.

## Why not Spring AOP aspects (the prior approach)

- Spring AOP couples controls to Spring. ADK does not require Spring;
  many ADK apps run on Quarkus, Micronaut, or plain Java.
- Aspects don't compose cleanly with ADK's callback chain — we'd be
  routing through two interception layers.
- The plugin API is more explicit and easier to test than aspect
  pointcuts.

## Out of scope (deliberately)

- Removing the LangChain4j module. Backwards-compatible; users on that
  path keep working.
- Cross-language ADK ports (Python / Go / TypeScript). Java-only by
  scope.

## Consequences

Positive: alignment with Google's official extension surface; simpler
mental model; cross-framework portability.

Negative: requires tracking ADK release cadence; CI runs nightly against
`com.google.adk:google-adk:1.+` to surface breakage.
