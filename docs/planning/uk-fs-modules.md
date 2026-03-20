# Additional UK Financial Services Modules

Beyond the core starters, Regulus can offer specialised modules that codify UK financial services controls and validators. The goal is to give regulated teams plug-and-play compliance while keeping each domain loosely coupled.

## Proposed Starters

- **`regulus-ai-trade-surveillance-starter`**
  - Adds MAR/MiFID surveillance rules, market abuse scenarios, and suspicious transaction reporting (STR) templates.
  - Integrates with MCP toolsets for trade pattern analysis and voice transcription review.
- **`regulus-ai-lending-starter`**
  - Provides FCA affordability/creditworthiness checks, income verification connectors, and mortgage offer validations.
  - Ships policy guards for Consumer Duty outcomes and affordability thresholds.
- **`regulus-ai-fincrime-starter`**
  - Bundles sanctions screening, PEP monitoring, adverse media scoring, and suspicious activity report (SAR) enrichment.
  - Exposes connectors to external intelligence providers and internal case management MCP servers.
- **`regulus-ai-insurance-starter`**
  - Implements claims fraud heuristics, Solvency II data checks, and risk exposure aggregations.
  - Includes privacy overlays tailored to sensitive health and claims data.
- **`regulus-ai-tax-reporting-starter`**
  - Validates CRS/FATCA data, HMRC submissions, and withholding tax calculations.
  - Provides automatic audit trail generation aligned with HMRC inspection requirements.
- **`regulus-ai-risk-simulation`** *(module)*
  - Executes historical replays, scenario libraries, dependency stress tests, shadow mode evaluations, and guardrail probing to evidence model risk controls.
  - Generates artefacts required by SS1/23 and PS21/3 before production go-live.

## Shared Patterns

- Each starter reuses the governance, privacy, and observability layers from the core platform.
- Domain validators surface through MCP toolsets so other agents can consume them without direct dependencies.
- Property namespaces follow the `regulus.ai.<domain>.*` convention for consistent configuration.
- Documentation should include regulatory references (FCA, PRA, HMRC) and maintenance ownership for each module.

## Next Steps

1. Prioritise modules based on demand signals (e.g., payments, financial crime) and available SME capacity.
2. Define the canonical validation rules and external integrations required for each domain.
3. Prototype one starter (likely FinCrime) to validate the pattern before expanding to the full list.
