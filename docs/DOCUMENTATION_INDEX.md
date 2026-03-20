# Regulus Documentation Index

Complete guide to all Regulus platform documentation. Use this index to quickly find what you need.

---

## Quick Links by Role

### For New Users

Start here to understand what Regulus is and how to get started:

- [README.md](../README.md) - Platform overview and quick start
- [Quickstart Tutorial](guides/quickstart-tutorial.md) - Build a complete UK FS agent step-by-step
- [Architecture Overview](architecture/architecture.md) - How Regulus works
- [Developer Checklist](guides/developer-checklist.md) - Checklist for building production agents

### For Developers

Building agents and extending the platform:

- [Quickstart Tutorial](guides/quickstart-tutorial.md) - Complete working example with code
- [Spring Boot Starters](guides/starters.md) - Available starters and configuration
- [Data Residency Guide](guides/data-residency.md) - UK GDPR/FCA data residency compliance
- [ADK/MCP/A2A Integration](architecture/adk-mcp-a2a.md) - Protocol details with examples
- [Developer Experience](guides/developer-experience.md) - Tooling and local development
- [Pipelines DSL](guides/pipeline-dsl.md) - Declarative agent configuration
- [Hybrid Python Evals](guides/hybrid-python-evals.md) - Evaluation and red-teaming integration
- [CONTRIBUTING.md](../CONTRIBUTING.md) - How to contribute to Regulus
- [Project Conventions](project-conventions.md) - Coding standards and project structure

### For Architects

Understanding the system design and making architectural decisions:

- [Architecture](architecture/architecture.md) - System architecture and core components
- [Architecture Diagram](architecture/architecture-diagram.md) - Visual system overview
- [ADK/MCP/A2A Integration](architecture/adk-mcp-a2a.md) - Interoperability patterns
- [Integration Matrix](references/integration-matrix.md) - External system dependencies
- [Architecture Decision Records](decisions/README.md) - ADRs for key architectural decisions
- [API Reference](references/api-reference.md) - Complete API documentation

### For Risk & Compliance

Governance, security, and regulatory alignment:

- [Governance & Security](governance/governance-security.md) - Regulatory compliance (SS1/23, PS21/3, SS2/21, Consumer Duty, ICO)
- [Model Registry](governance/model-registry.md) - SS1/23 compliant model inventory
- [Risk Control Matrix](governance/risk-control-matrix.md) - Controls mapped to regulations
- [Kill Switch Design](governance/kill-switch.md) - Dual-control (4-eyes) emergency shutdown
- [Consumer Duty Guide](governance/consumer-duty.md) - FCA Consumer Duty four outcomes implementation
- [Data Residency Guide](guides/data-residency.md) - UK GDPR/FCA data residency enforcement
- [Regulatory Reference](references/regulatory-reference.md) - All PRA/FCA/ICO citations with links
- [Risk Simulation](governance/risk-simulation.md) - Pre-production risk testing
- [Audit Evidence Templates](references/audit-evidence-templates.md) - JSON templates for regulatory evidence

### For Operations & SRE

Running and maintaining the platform in production:

- [Operational Runbooks](references/operational-runbooks.md) - Standard operating procedures for all scenarios
- [Troubleshooting Guide](guides/troubleshooting.md) - Common issues and solutions
- [Security Hardening Guide](guides/security-hardening.md) - Production security configuration
- [API Reference](references/api-reference.md) - Complete API documentation
- [Implementation Playbooks](references/implementation-playbooks.md) - Step-by-step integration guides
- [Integration Matrix](references/integration-matrix.md) - System integrations and ownership
- [Kill Switch Design](governance/kill-switch.md) - Operational procedures

### For Product & Planning

Understanding the product roadmap and planning:

- [Product Roadmap](planning/product-roadmap.md) - Complete platform evolution plan (37+ weeks)
- [Rollout Plan](planning/rollout-plan.md) - Initial 12-week delivery plan
- [ADK Compatibility Plan](planning/ADK_COMPATIBILITY_PLAN.md) - **PRIMARY** 4-week plan for Google ADK interoperability
- [Gap Closure Plan](planning/GAP_CLOSURE_PLAN.md) - Full gap analysis (reference for future phases)
- [UK FS Modules](planning/uk-fs-modules.md) - Domain-specific extensions

### For AI Coding Agents

If you're an AI agent (like Claude Code) contributing to this project:

- [Agent Work Tracking](agent-work/README.md) - How to document your work
- [Session Template](agent-work/sessions/template.md) - Session documentation template
- [Example Session](agent-work/sessions/2025-01-22-1430-claude-code.md) - Real session example
- [Project Conventions](project-conventions.md#ai-agent-work-tracking) - AI agent guidelines

---

## Documentation by Category

### Getting Started

| Document | Description | Audience |
|----------|-------------|----------|
| [README.md](../README.md) | Platform overview, quick start, key capabilities | Everyone |
| [Quickstart Tutorial](guides/quickstart-tutorial.md) | Complete UK FS agent example with code | Developers |
| [Developer Checklist](guides/developer-checklist.md) | End-to-end guide to building and shipping agents | Developers |
| [CONTRIBUTING.md](../CONTRIBUTING.md) | Contributing guidelines and setup instructions | Contributors |

### Architecture & Design

| Document | Description | Audience |
|----------|-------------|----------|
| [Architecture](architecture/architecture.md) | Core components, flow overview, deployment | Architects, Developers |
| [Architecture Diagram](architecture/architecture-diagram.md) | Mermaid diagrams of system architecture | Architects |
| [ADK/MCP/A2A Integration](architecture/adk-mcp-a2a.md) | Integration patterns for interoperability | Architects, Developers |
| [Architecture Decision Records](decisions/README.md) | ADRs for major architectural decisions | Architects, Tech Leads |

### Development Guides

| Document | Description | Audience |
|----------|-------------|----------|
| [Quickstart Tutorial](guides/quickstart-tutorial.md) | Complete working example for UK FS agent | Developers |
| [Spring Boot Starters](guides/starters.md) | All starters with detailed configuration | Developers |
| [Data Residency Guide](guides/data-residency.md) | UK GDPR/FCA data residency enforcement | Developers, Compliance |
| [Project Conventions](project-conventions.md) | Directory structure, naming, coding standards | Developers, AI Agents |
| [Developer Experience](guides/developer-experience.md) | Local dev, tooling, productivity features | Developers |
| [Pipelines DSL](guides/pipeline-dsl.md) | Kotlin and YAML DSL for agent configuration | Developers |
| [Hybrid Python Evals](guides/hybrid-python-evals.md) | Evaluation and red-team service integration | Developers, QA |

### Implementation & Operations

| Document | Description | Audience |
|----------|-------------|----------|
| [Operational Runbooks](references/operational-runbooks.md) | SOPs for kill switch, failover, incidents | SRE, Operations |
| [Troubleshooting Guide](guides/troubleshooting.md) | Common issues and solutions | Developers, SRE |
| [Security Hardening Guide](guides/security-hardening.md) | Production security configuration | Security, SRE |
| [API Reference](references/api-reference.md) | REST and MCP API documentation | Developers, Integrators |
| [Implementation Playbooks](references/implementation-playbooks.md) | Step-by-step adapter implementation guides | Platform Team, SRE |
| [Integration Matrix](references/integration-matrix.md) | External systems, interfaces, ownership | Integration Teams |
| [Kill Switch Design](governance/kill-switch.md) | Emergency shutdown architecture and procedures | SRE, Operations |

### Governance & Compliance

| Document | Description | Audience |
|----------|-------------|----------|
| [Governance & Security](governance/governance-security.md) | SS1/23, PS21/3, SS2/21, Consumer Duty, ICO compliance | Risk, Compliance, Security |
| [Model Registry](governance/model-registry.md) | SS1/23 compliant model inventory with risk tiering | Model Risk, Compliance |
| [Consumer Duty Guide](governance/consumer-duty.md) | FCA Consumer Duty four outcomes implementation | Compliance, Developers |
| [Regulatory Reference](references/regulatory-reference.md) | All PRA/FCA/ICO citations with direct links | Compliance, Audit |
| [Audit Evidence Templates](references/audit-evidence-templates.md) | JSON templates for regulatory evidence generation | Audit, Compliance |
| [Risk Control Matrix](governance/risk-control-matrix.md) | Regulatory requirements mapped to controls | Risk, Audit |
| [Kill Switch Design](governance/kill-switch.md) | Dual-control (4-eyes) emergency shutdown | Platform Ops, Risk |
| [Risk Simulation](governance/risk-simulation.md) | Pre-production risk testing module | Risk, Model Validation |

### Planning & Roadmap

| Document | Description | Audience |
|----------|-------------|----------|
| [Product Roadmap](planning/product-roadmap.md) | 37+ week platform evolution plan | Product, Leadership |
| [Rollout Plan](planning/rollout-plan.md) | Initial 12-week delivery plan (Phase 0) | Engineering, Product |
| [Gap Closure Plan](planning/GAP_CLOSURE_PLAN.md) | Concrete tasks to close UK FS adoption gaps | Engineering, Product |
| [UK FS Modules](planning/uk-fs-modules.md) | Domain-specific starter modules | Product, Architects |

### AI Agent Work Tracking

| Document | Description | Audience |
|----------|-------------|----------|
| [Agent Work README](agent-work/README.md) | Guidelines for AI agent work documentation | AI Agents, Developers |
| [Session Template](agent-work/sessions/template.md) | Template for session documentation | AI Agents |
| [Example Session](agent-work/sessions/2025-01-22-1430-claude-code.md) | Real session log example | AI Agents, Developers |

---

## Documentation Organization

### Current Structure (Organized)

The documentation is organized into logical directories:

```
docs/
├── architecture/          # Architecture and design
│   ├── architecture.md
│   ├── architecture-diagram.md
│   └── adk-mcp-a2a.md         # MCP/A2A protocols with streaming
├── guides/               # How-to guides
│   ├── quickstart-tutorial.md  # Complete UK FS agent example
│   ├── starters.md             # All starters with config
│   ├── data-residency.md       # UK GDPR/FCA compliance
│   ├── troubleshooting.md      # Common issues and solutions
│   ├── security-hardening.md   # Production security config
│   ├── developer-checklist.md
│   ├── developer-experience.md
│   ├── pipeline-dsl.md
│   └── hybrid-python-evals.md
├── references/           # Reference documentation
│   ├── api-reference.md        # Complete REST/MCP API docs
│   ├── regulatory-reference.md # All PRA/FCA/ICO citations
│   ├── audit-evidence-templates.md # JSON templates for audits
│   ├── operational-runbooks.md # SOPs for operations
│   ├── integration-matrix.md
│   └── implementation-playbooks.md
├── governance/          # Governance and compliance
│   ├── governance-security.md  # With regulatory links
│   ├── consumer-duty.md        # FCA Consumer Duty guide
│   ├── model-registry.md       # SS1/23 model inventory
│   ├── risk-control-matrix.md  # Extended controls
│   ├── kill-switch.md          # Dual-control (4-eyes)
│   └── risk-simulation.md
├── planning/            # Planning and roadmap
│   ├── rollout-plan.md
│   ├── product-roadmap.md
│   ├── IMPLEMENTATION_ROADMAP.md
│   └── uk-fs-modules.md
├── decisions/           # Architecture Decision Records
│   ├── README.md
│   ├── ADR-001-dual-control-kill-switch.md
│   ├── ADR-002-data-residency-enforcement.md
│   ├── ADR-003-mcp-protocol-adoption.md
│   └── ADR-004-langchain4j-llm-abstraction.md
└── agent-work/          # AI agent work tracking
    ├── README.md
    ├── sessions/
    ├── progress/
    └── decisions/
```

---

## Common Tasks & Where to Look

### "I want to..."

**...understand what Regulus is**
→ [README.md](../README.md)

**...build my first agent**
→ [Quickstart Tutorial](guides/quickstart-tutorial.md) + [Developer Checklist](guides/developer-checklist.md)

**...configure an agent with policies**
→ [Pipelines DSL](guides/pipeline-dsl.md) + [Starters](guides/starters.md)

**...integrate with external systems**
→ [Implementation Playbooks](references/implementation-playbooks.md) + [Integration Matrix](references/integration-matrix.md)

**...set up local development**
→ [Developer Experience](guides/developer-experience.md)

**...understand governance requirements**
→ [Governance & Security](governance/governance-security.md) + [Risk Control Matrix](governance/risk-control-matrix.md)

**...implement dual-control kill switch**
→ [Kill Switch Design](governance/kill-switch.md) - includes 4-eyes principle, Java API, audit trail

**...enforce UK data residency**
→ [Data Residency Guide](guides/data-residency.md) - UK GDPR/FCA compliance with cloud region mapping

**...register models for SS1/23**
→ [Model Registry](governance/model-registry.md) - risk tiering, validation tracking, audit trail

**...see the product roadmap**
→ [Product Roadmap](planning/product-roadmap.md)

**...contribute code**
→ [CONTRIBUTING.md](../CONTRIBUTING.md) + [Project Conventions](project-conventions.md)

**...document my AI agent session**
→ [Agent Work README](agent-work/README.md) + [Session Template](agent-work/sessions/template.md)

**...know what starters are available**
→ [Starters](guides/starters.md) + [UK FS Modules](planning/uk-fs-modules.md)

**...understand the architecture**
→ [Architecture](architecture/architecture.md) + [Architecture Diagram](architecture/architecture-diagram.md)

**...understand why a design decision was made**
→ [Architecture Decision Records](decisions/README.md) - ADRs for kill switch, data residency, MCP, LangChain4j

**...set up MCP or A2A integration**
→ [ADK/MCP/A2A Integration](architecture/adk-mcp-a2a.md) - includes Tools, Resources, Prompts, Streaming

**...implement Consumer Duty requirements**
→ [Consumer Duty Guide](governance/consumer-duty.md) - four outcomes with code examples

**...prepare for regulatory audit**
→ [Audit Evidence Templates](references/audit-evidence-templates.md) + [Regulatory Reference](references/regulatory-reference.md)

**...troubleshoot an issue**
→ [Troubleshooting Guide](guides/troubleshooting.md)

**...handle a production incident**
→ [Operational Runbooks](references/operational-runbooks.md)

**...secure my deployment**
→ [Security Hardening Guide](guides/security-hardening.md)

**...use the REST or MCP API**
→ [API Reference](references/api-reference.md)

**...implement evaluations**
→ [Hybrid Python Evals](guides/hybrid-python-evals.md)

**...understand risk simulation**
→ [Risk Simulation](governance/risk-simulation.md)

---

## Documentation Standards

All documentation follows standards defined in [Project Conventions](project-conventions.md):

- **Format**: Markdown (.md)
- **Style**: GitHub-flavored Markdown
- **Naming**: kebab-case.md
- **Headers**: Single `#` for title, `##` for sections
- **Code Blocks**: Always specify language
- **Links**: Relative paths for internal docs
- **Line Length**: 120 characters soft limit, 150 hard limit
- **Whitespace**: No trailing spaces, final newline required

---

## Documentation Maintenance

### Ownership

| Category | Owner | Review Frequency |
|----------|-------|------------------|
| Architecture | Platform Architects | Quarterly |
| Governance | Risk & Compliance | Quarterly or on regulatory change |
| Developer Guides | Platform Team | Monthly |
| Operations | SRE Team | Monthly |
| Planning | Product Team | Sprint-based |
| AI Agent Work | Individual Contributors | Per session |

### Version History

Track changes in:
- **Individual documents**: Version history table at bottom
- **Cross-document changes**: This index file
- **Git history**: Commit messages following conventional commits

### Review Process

See [CONTRIBUTING.md](../CONTRIBUTING.md) for documentation review requirements:

- Architecture docs: Platform Architect + Security Architect
- Governance docs: Risk & Compliance lead
- Developer guides: 2x senior developers
- API references: API owner + 1x developer

---

## Feedback & Improvements

Found an issue or gap in documentation?

1. **Check existing issues**: [GitHub Issues](https://github.com/regulus-platform/regulus/issues)
2. **Create documentation issue**: Use "documentation" label
3. **Suggest improvements**: PRs welcome!

See [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2025-01-22 | Initial documentation index | Platform Team |
| 1.1 | 2025-01-25 | Added quickstart tutorial, data residency, model registry, updated all governance docs | Platform Team |
| 1.2 | 2025-01-26 | Added regulatory reference, Consumer Duty guide, audit evidence templates, ADRs, security hardening, API reference, operational runbooks, troubleshooting guide | Platform Team |

---

**Last Updated**: 2025-01-26
