# Developer Experience

Regulus aims to make agent development as frictionless as modern Python frameworks while staying within banking guardrails.

## Project Scaffolding

- **Spring Initializr Blueprint**: Generates a runnable payment agent with configured starters, sample policies, and pipeline DSL snippets.
- **Templates**: Include domain-specific variants (payments, KYC, support) to accelerate onboarding.
- **BOM Alignment**: Projects import `com.regulus.platform:regulus-ai-bom` to avoid version drift across starters and plugins.

## Local Environments

- **DevContainer**: Bundles JDK, Gradle, Docker, vector store, LLM stub, eval service, OTEL collector, and Grafana/Kibana dashboards.
- **Make Targets**: `make run`, `make test`, and `make eval` wire local services together for quick iteration.
- **JShell/REPL Snippets**: Provide interactive examples for invoking agents, tools, and the router.

## Tooling & Plugins

- **Gradle Plugin**: Adds tasks for eval checks, pipeline validation, and governance artefact generation.
- **CLI Utilities**: Optional command-line helpers to inspect policies, list MCP toolsets, or replay agent traces.
- **Git Hooks**: Pre-commit validation for DSL files and policy annotations to catch issues before CI.
- **Risk Alignment**: Developer tooling surfaces KRIs (eval pass rates, policy violations) and links directly to required artefact templates so teams can collaborate smoothly with risk and compliance.

## Documentation & Support

- **docs/** Directory: Houses architecture, starters, DSL reference, governance, and rollout guidance; updated each sprint.
- **Developer Checklist**: See `docs/developer-checklist.md` for an end-to-end integration and governance to-do list.
- **Codelabs**: Inspired by Google’s ADK codelabs, internal labs walk developers through MCP and A2A scenarios.
- **Support Model**: Define office hours, Slack channels, and escalation paths for platform issues.

## Adoption Metrics

- Track number of onboarded teams, eval pass rates, policy violations, and mean time to fix guardrail breaches.
- Use metrics to prioritise future platform investments (additional starters, tooling, or governance automation).
- Platform evolution roadmap, including developer experience enhancements, is detailed in `docs/product-roadmap.md`.
