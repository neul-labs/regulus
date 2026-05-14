# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records (ADRs) documenting significant architectural decisions made for the Regulus platform.

## What is an ADR?

An ADR is a short document that captures an important architectural decision along with its context and consequences. ADRs help teams:

- Understand why decisions were made
- Avoid revisiting settled decisions
- Onboard new team members
- Learn from past decisions

## ADR Index

| ID | Title | Status | Date |
|----|-------|--------|------|
| [ADR-001](./ADR-001-dual-control-kill-switch.md) | Dual-Control Kill Switch | Accepted | 2025-01-15 |
| [ADR-002](./ADR-002-data-residency-enforcement.md) | Data Residency at Platform Level | Accepted | 2025-01-15 |
| [ADR-003](./ADR-003-mcp-protocol-adoption.md) | MCP Protocol for Tool Exposure | Accepted | 2025-01-10 |
| [ADR-004](./ADR-004-langchain4j-llm-abstraction.md) | LangChain4j for LLM Abstraction | Superseded by ADR-006 | 2025-01-08 |
| [ADR-005](./ADR-005-eu-ai-act-mapping.md) | EU AI Act Control Mapping | Accepted | 2026-03-22 |
| [ADR-006](./ADR-006-adk-primary-runtime-and-extension-model.md) | Google ADK as Primary Runtime and Extension Model | Accepted | 2026-03-25 |
| [ADR-007](./ADR-007-distribution-channels.md) | Distribution Channels | Accepted | 2026-04-02 |
| [ADR-008](./ADR-008-residency-by-construction.md) | Residency by Construction | Accepted | 2026-04-09 |
| [ADR-009](./ADR-009-regtech-as-product-docs.md) | Regtech-as-Product-Docs Editorial Standard | Accepted | 2026-04-14 |
| [ADR-010](./ADR-010-ai-governance-framework-integration-model.md) | AI Governance Framework Integration Model | Accepted | 2026-05-12 |
| [ADR-011](./ADR-011-grc-integration-via-pluggable-adapters.md) | GRC Integration via Pluggable Adapters | Accepted | 2026-05-13 |
| [ADR-012](./ADR-012-three-lines-of-defence-operating-model.md) | Three Lines of Defence Operating Model | Accepted | 2026-05-13 |

## ADR Template

Use this template for new ADRs:

```markdown
# ADR-NNN: Title

## Status
[Proposed | Accepted | Deprecated | Superseded]

## Context
[What is the issue that we're seeing that is motivating this decision?]

## Decision
[What is the change that we're proposing and/or doing?]

## Consequences
[What becomes easier or more difficult to do because of this change?]

## Alternatives Considered
[What other options were evaluated?]

## References
[Links to relevant documentation, regulations, or discussions]
```

## Creating a New ADR

1. Copy the template
2. Assign the next available number
3. Fill in all sections
4. Submit for review
5. Update the index above

## Review Process

- **Technical ADRs**: Reviewed by Platform Architects
- **Governance ADRs**: Reviewed by Platform Architects + Risk/Compliance
- **Security ADRs**: Reviewed by Platform Architects + Security Architect
