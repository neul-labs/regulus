# Migration from LangChain4j

Regulus' original public version was built around LangChain4j. That path
is **retained** but **demoted**: ADK is now the recommended runtime.

## What changes for users on LangChain4j

Nothing breaks. The `regulus-ai-llm` module still exists; the LangChain4j
provider abstraction (Gemini / OpenAI / Anthropic / Azure) still works;
the existing Spring Boot starters
(`regulus-ai-agents-spring-boot-starter`, `regulus-ai-governance-starter`,
`regulus-ai-safety-starter`) still publish.

What's new is that the **same compliance controls** are available natively
through ADK plugins. If you migrate to ADK, you stop translating between
LangChain4j abstractions and ADK's official seams (callbacks, plugins,
services).

## Migration steps

1. Add the ADK starter alongside the existing LangChain4j starter.
2. Move agent definitions to ADK `LlmAgent` builders.
3. Replace LangChain4j-specific tooling with ADK tools / MCP servers.
4. Switch your audit pipeline to the ADK plugin's emission point.
5. Remove the LangChain4j dependencies when nothing reaches them.

## When to stay on LangChain4j

- You depend on a LangChain4j feature ADK doesn't have (e.g. specific
  embedding integrations).
- You need cross-runtime portability for an existing investment.
- You have agents you can't justify rewriting.

The two paths share the underlying compliance mechanisms — what differs
is which plugin / interceptor surface you use to expose them.

## Why ADK is the recommended path

- It's the *official* extension surface from Google. Regulus' controls
  hook into seams Google ships and maintains — rather than us inventing
  hooks.
- New ADK features (Plugin system, EventCompactor, ToolConfirmation,
  HITL, A2A) land naturally without translation.
- The Java ecosystem (`com.google.adk:google-adk`) is rapidly growing.

## See also

- [ADR-006](../../../docs/decisions/ADR-006-adk-primary-runtime-and-extension-model.md)
  — the decision and rationale.
