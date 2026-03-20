# Project Structure & Conventions

This document defines the organizational standards, file structure, naming conventions, and development hygiene practices for the Regulus platform. All contributors—human developers and AI coding agents—must follow these guidelines to maintain consistency and quality.

---

## Table of Contents

1. [Directory Structure](#directory-structure)
2. [File Naming Conventions](#file-naming-conventions)
3. [Documentation Standards](#documentation-standards)
4. [Code Organization](#code-organization)
5. [AI Agent Work Tracking](#ai-agent-work-tracking)
6. [Version Control Practices](#version-control-practices)
7. [Review & Approval Process](#review--approval-process)

---

## Directory Structure

### Root Level Organization

```
regulus/
├── docs/                      # All documentation
│   ├── agent-work/           # AI agent progress tracking (see below)
│   ├── architecture/         # Architecture documentation and diagrams
│   ├── guides/               # How-to guides and tutorials
│   ├── references/           # API references and specifications
│   └── decisions/            # Architecture Decision Records (ADRs)
├── platform/                 # Platform source code
│   ├── regulus-ai-bom/      # Bill of Materials
│   ├── starters/            # Spring Boot starters
│   │   ├── regulus-ai-agents-spring-boot-starter/
│   │   ├── regulus-ai-governance-starter/
│   │   ├── regulus-ai-payments-starter/
│   │   ├── regulus-ai-safety-starter/
│   │   └── regulus-ai-evals-client-starter/
│   ├── core/                # Core libraries
│   │   ├── regulus-ai-policy/
│   │   ├── regulus-ai-privacy/
│   │   ├── regulus-ai-observability/
│   │   └── regulus-ai-kill-switch/
│   ├── dsl/                 # Pipeline DSL parsers
│   │   ├── regulus-ai-dsl-kotlin/
│   │   └── regulus-ai-dsl-yaml/
│   ├── adapters/            # External system adapters
│   │   ├── model-inventory-adapter/
│   │   ├── grc-evidence-adapter/
│   │   ├── vendor-registry-adapter/
│   │   └── eval-service-adapter/
│   └── tooling/             # Developer tooling
│       ├── gradle-plugin/
│       ├── cli/
│       └── ide-extensions/
├── examples/                # Example agents and configurations
│   ├── payment-agent/
│   ├── kyc-agent/
│   └── support-agent/
├── tests/                   # Integration and end-to-end tests
│   ├── integration/
│   ├── contract/
│   └── performance/
├── devops/                  # DevOps and infrastructure
│   ├── docker/
│   ├── kubernetes/
│   ├── ci-cd/
│   └── observability/
├── scripts/                 # Utility scripts
├── .devcontainer/          # DevContainer configuration
├── gradle/                  # Gradle wrapper and configuration
├── .github/                # GitHub workflows and templates
├── ../CONTRIBUTING.md         # Contribution guidelines
├── ../CODE_OF_CONDUCT.md      # Code of conduct
├── ../SECURITY.md             # Security policy
├── LICENSE                 # License file
└── ../README.md               # Project overview
```

### Documentation Structure

```
docs/
├── agent-work/                    # AI coding agent work tracking
│   ├── sessions/                  # Individual agent sessions
│   │   └── YYYY-MM-DD-HHmm-agent-name.md
│   ├── progress/                  # Feature/epic progress tracking
│   │   └── feature-name.md
│   └── decisions/                 # Quick decisions made by agents
│       └── YYYY-MM-DD-decision-summary.md
├── architecture/                  # Architecture documentation
│   ├── architecture.md           # Main architecture doc
│   ├── architecture-diagram.md   # System diagrams
│   └── adk-mcp-a2a.md           # Integration patterns
├── guides/                        # How-to guides
│   ├── developer-checklist.md
│   ├── developer-experience.md
│   ├── starters.md
│   ├── pipeline-dsl.md
│   └── hybrid-python-evals.md
├── references/                    # Reference documentation
│   ├── api/                      # API specifications
│   │   ├── configuration-properties.md
│   │   ├── adapter-interfaces.md
│   │   └── openapi/             # OpenAPI specs
│   ├── integration-matrix.md
│   └── implementation-playbooks.md
├── governance/                    # Governance and compliance
│   ├── governance-security.md
│   ├── risk-control-matrix.md
│   ├── risk-simulation.md
│   └── kill-switch.md
├── planning/                      # Planning and roadmap
│   ├── rollout-plan.md
│   ├── product-roadmap.md
│   └── uk-fs-modules.md
├── decisions/                     # Architecture Decision Records
│   ├── README.md                 # ADR index
│   ├── template.md               # ADR template
│   └── NNNN-title.md            # Individual ADRs
├── project-conventions.md        # This file
└── DOCUMENTATION_INDEX.md        # Master documentation index
```

---

## File Naming Conventions

### Documentation Files

- **Format**: `kebab-case.md`
- **Examples**: `architecture/architecture.md`, `guides/developer-checklist.md`, `governance/kill-switch.md`
- **Sections**: Use level-2 headings (`##`) for major sections
- **Cross-references**: Always use relative paths starting with `docs/`

### Code Files

- **Java Classes**: `PascalCase.java`
  - Interfaces: `I` prefix optional (prefer descriptive names)
  - Abstract classes: `Abstract` prefix
  - Implementations: Descriptive names (e.g., `DefaultPolicyGuard`, `VaultSecretProvider`)

- **Kotlin Files**: `PascalCase.kt` for classes, `kebab-case.kt` for DSL files

- **Configuration**:
  - `application.yaml` or `application.yml` (prefer YAML)
  - `application-{profile}.yaml` for profile-specific config
  - `regulus-{feature}.yaml` for feature-specific config

- **Test Files**: Match source file with `Test` or `IT` suffix
  - Unit tests: `ClassNameTest.java`
  - Integration tests: `ClassNameIT.java`
  - Contract tests: `ClassNameContractTest.java`

### Module Naming

- **Gradle Modules**: `regulus-ai-{feature}[-{variant}]`
  - Examples: `regulus-ai-agents-spring-boot-starter`, `regulus-ai-policy`, `regulus-ai-dsl-kotlin`

- **Packages**: `com.regulus.platform.{module}.{component}`
  - Examples: `com.regulus.platform.policy.guards`, `com.regulus.platform.agents.planners`

---

## Documentation Standards

### Markdown Style Guide

1. **Headers**:
   - Use `#` for document title (only one per file)
   - Use `##` for major sections
   - Use `###` for subsections
   - Maximum depth: 4 levels (`####`)

2. **Code Blocks**:
   - Always specify language: ` ```java`, ` ```yaml`, ` ```kotlin`
   - Use `bash` for shell commands, not `sh` or `shell`

3. **Links**:
   - Internal: Use relative paths: `[text](architecture/architecture.md)`
   - External: Use full URLs with HTTPS
   - Section links: `[text](#section-name)` (lowercase, hyphens)

4. **Lists**:
   - Use `-` for unordered lists (not `*` or `+`)
   - Use `1.` for ordered lists (markdown will auto-number)
   - Indent nested lists with 2 spaces

5. **Formatting**:
   - **Bold**: Important concepts, emphasis
   - *Italic*: Subtle emphasis, first mention of terms
   - `Code`: Class names, file paths, configuration keys, commands
   - > Blockquotes: Callouts, notes, warnings

6. **Tables**:
   - Always include header row
   - Align columns with pipes for readability
   - Use `---` separator (no need for exact alignment in separator)

7. **Line Length**:
   - Prefer wrapping at 120 characters
   - Hard wrap at 150 characters
   - Exception: Tables, code blocks, URLs

8. **Whitespace**:
   - One blank line between sections
   - Two blank lines before major headings (`##`)
   - No trailing whitespace
   - Files must end with single newline

### Document Structure Template

```markdown
# Document Title

Brief introduction (1-2 sentences describing the purpose).

[Optional: Table of contents for documents >3 sections]

## Overview

High-level description of the topic.

## Section 1

Detailed content.

### Subsection 1.1

More specific content.

## Section 2

...

## References

- [Related Doc](related.md)
- [External Resource](https://example.com)

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | YYYY-MM-DD | Initial version | Name |
```

### Documentation Metadata

Every documentation file should include:

1. **Front Matter** (optional, for complex docs):
   ```yaml
   ---
   title: Document Title
   author: Author Name
   date: YYYY-MM-DD
   version: 1.0
   status: draft|review|approved
   reviewers: [Name1, Name2]
   ---
   ```

2. **Version History** (recommended for key docs):
   - Table at bottom of document
   - Track major changes only
   - Include author and date

---

## Code Organization

### Java/Kotlin Source Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/regulus/platform/{module}/
│   │       ├── config/          # Spring configuration classes
│   │       ├── model/           # Domain models and DTOs
│   │       ├── service/         # Business logic
│   │       ├── adapter/         # External adapters
│   │       ├── controller/      # REST controllers (if applicable)
│   │       └── util/            # Utilities
│   ├── kotlin/                  # Kotlin source (DSL, etc.)
│   └── resources/
│       ├── META-INF/
│       │   └── spring.factories # Spring Boot auto-configuration
│       ├── application.yaml     # Default configuration
│       └── banner.txt          # Optional Spring Boot banner
└── test/
    ├── java/                    # Test source
    └── resources/               # Test resources
```

### Configuration Properties

- **Namespace**: All properties under `regulus.ai.*`
- **Structure**:
  ```yaml
  regulus:
    ai:
      model:                    # Model routing
        provider: azure-openai
        name: gpt-4o
      policies:                 # Policy configuration
        enforced: [require.LEI, require.PurposeCode]
      privacy:                  # Privacy settings
        redact:
          - $.customer.nationalId
      {domain}:                 # Domain-specific (payments, etc.)
        ...
  ```

### Package Organization

1. **Layered Structure**:
   - `config`: Spring configuration, auto-configuration
   - `model`: DTOs, domain objects, events
   - `service`: Business logic, orchestration
   - `adapter`: External system integrations
   - `guard`: Policy guards, interceptors
   - `filter`: Privacy filters, transformations

2. **Dependency Rules**:
   - No circular dependencies between modules
   - Core modules have no dependencies on starters
   - Adapters depend on core, not on each other
   - Tests can depend on anything

---

## AI Agent Work Tracking

### Purpose

AI coding agents (like Claude Code) must document their work to ensure:
- Progress transparency
- Knowledge transfer to human developers
- Audit trail for changes
- Context preservation for future sessions

### Session Documentation

**Location**: `docs/agent-work/sessions/`

**Filename**: `YYYY-MM-DD-HHmm-{agent-name}.md`

**Example**: `2025-01-15-1430-claude-code.md`

**Template**:

```markdown
# Agent Work Session - YYYY-MM-DD HH:mm

**Agent**: Claude Code
**Session Start**: YYYY-MM-DD HH:mm UTC
**Session End**: YYYY-MM-DD HH:mm UTC
**User**: [Username]

## Objective

[Brief description of what was requested]

## Work Completed

### 1. [Task Name]

**Status**: ✅ Complete | ⏳ In Progress | ❌ Blocked

**Files Changed**:
- `path/to/file1.java` - [description of changes]
- `path/to/file2.md` - [description of changes]

**Details**:
[Detailed description of what was done and why]

**Decisions Made**:
- [Key decision 1 with rationale]
- [Key decision 2 with rationale]

### 2. [Next Task]

...

## Issues Encountered

- **Issue**: [Description]
  - **Resolution**: [How it was resolved or workaround]
  - **Status**: Resolved | Needs Follow-up

## TODO / Follow-up

- [ ] Item requiring follow-up
- [ ] Item for next session

## Files Modified

**Created**:
- `path/to/new/file1`
- `path/to/new/file2`

**Modified**:
- `path/to/modified/file1`
- `path/to/modified/file2`

**Deleted**:
- `path/to/deleted/file1`

## Testing Performed

- [x] Unit tests passed
- [x] Integration tests passed
- [ ] Manual testing performed
- [ ] Documentation updated

## References

- Related issue/ticket: #123
- Related PR: #456
- Documentation: `path/to/doc.md`

## Notes for Human Reviewers

[Any important context, caveats, or areas requiring human review]
```

### Progress Tracking

**Location**: `docs/agent-work/progress/`

**Purpose**: Track multi-session work on features or epics

**Filename**: `{feature-name}.md`

**Template**:

```markdown
# Feature Progress: [Feature Name]

**Epic/Feature ID**: [If applicable]
**Owner**: [Human owner]
**Started**: YYYY-MM-DD
**Target Completion**: YYYY-MM-DD
**Status**: Planning | In Progress | Review | Complete

## Overview

[Description of the feature and its goals]

## Success Criteria

- [ ] Criterion 1
- [ ] Criterion 2
- [ ] Criterion 3

## Work Breakdown

### Phase 1: [Phase Name]

**Status**: ✅ Complete | ⏳ In Progress | 📅 Planned

- [x] Task 1 - Completed YYYY-MM-DD (Session: [link])
- [x] Task 2 - Completed YYYY-MM-DD (Session: [link])
- [ ] Task 3 - In progress

### Phase 2: [Phase Name]

...

## Sessions

| Date | Agent | Summary | Session Notes |
|------|-------|---------|---------------|
| YYYY-MM-DD | Claude Code | [Brief summary] | [Link to session doc] |

## Decisions Made

| Date | Decision | Rationale | Impact |
|------|----------|-----------|--------|
| YYYY-MM-DD | [Decision] | [Why] | [What it affects] |

## Risks & Issues

| ID | Issue | Severity | Status | Notes |
|----|-------|----------|--------|-------|
| 1 | [Issue] | High/Med/Low | Open/Resolved | [Notes] |

## Testing Strategy

[How this feature will be tested]

## Documentation Updates Required

- [ ] API documentation
- [ ] User guide
- [ ] Architecture diagram
- [ ] Runbook

## Deployment Considerations

[Special deployment needs, migration steps, etc.]
```

### Decision Logging

**Location**: `docs/agent-work/decisions/`

**Purpose**: Quick decisions made during agent sessions

**Filename**: `YYYY-MM-DD-{brief-summary}.md`

**Template**:

```markdown
# Decision: [Brief Title]

**Date**: YYYY-MM-DD
**Agent**: Claude Code
**Context**: [Session or feature context]

## Problem

[What problem needed a decision]

## Options Considered

1. **Option 1**: [Description]
   - Pros: [List]
   - Cons: [List]

2. **Option 2**: [Description]
   - Pros: [List]
   - Cons: [List]

## Decision

[What was decided]

## Rationale

[Why this decision was made]

## Consequences

- **Positive**: [Expected benefits]
- **Negative**: [Trade-offs or drawbacks]
- **Risks**: [Potential risks]

## Follow-up Actions

- [ ] Action item 1
- [ ] Action item 2
```

### Agent Work Guidelines

1. **Session Documentation**:
   - Create session doc at start of work
   - Update continuously during session
   - Finalize before ending session
   - Link to related progress tracking docs

2. **File Organization**:
   - One session doc per distinct work session
   - Update progress docs at end of each session
   - Create decision docs for significant choices

3. **Transparency**:
   - Document all changes, even small ones
   - Explain rationale for decisions
   - Note any uncertainties or assumptions
   - Flag areas requiring human review

4. **Context Preservation**:
   - Include enough context for future agents/developers
   - Link to related work and documentation
   - Note any non-obvious implementation details

---

## Version Control Practices

### Branch Naming

- **Feature branches**: `feature/{ticket-id}-brief-description`
  - Example: `feature/REG-123-add-policy-guard`

- **Bug fixes**: `fix/{ticket-id}-brief-description`
  - Example: `fix/REG-456-null-pointer-in-eval`

- **Documentation**: `docs/{brief-description}`
  - Example: `docs/add-troubleshooting-guide`

- **Releases**: `release/v{major}.{minor}.{patch}`
  - Example: `release/v1.2.0`

### Commit Messages

Follow conventional commits format:

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Formatting, missing semicolons, etc. (no code change)
- `refactor`: Code change that neither fixes a bug nor adds a feature
- `perf`: Performance improvement
- `test`: Adding or updating tests
- `chore`: Maintenance tasks, dependency updates
- `ci`: CI/CD changes

**Scope**: Module or component affected (e.g., `policy`, `dsl`, `evals`)

**Examples**:
```
feat(policy): add LEI validation guard

Implements PolicyGuard for Legal Entity Identifier validation
following ISO 17442 standard.

Closes #123
```

```
docs(guides): add troubleshooting guide for eval failures

Addresses common eval service connection issues and
threshold configuration problems.
```

### Git Hygiene

1. **Before Committing**:
   - Run tests: `./gradlew test`
   - Check formatting: `./gradlew spotlessCheck`
   - Update documentation if needed
   - Review diff for debugging code or sensitive data

2. **Commit Frequency**:
   - Commit logical units of work
   - Each commit should be buildable
   - Avoid WIP commits on main branches

3. **Pull Requests**:
   - Keep PRs focused and reasonably sized (<500 lines ideal)
   - Include description of changes and testing performed
   - Link to related issues/tickets
   - Update CHANGELOG.md for user-facing changes
   - Ensure CI passes before requesting review

---

## Review & Approval Process

### Documentation Review

**Required Reviews**:
- **Architecture docs**: Platform Architect + Security Architect
- **Governance docs**: Risk & Compliance lead
- **Developer guides**: 2x senior developers
- **API references**: API owner + 1x developer

**Review Checklist**:
- [ ] Accurate and up-to-date information
- [ ] Consistent with existing documentation
- [ ] Follows markdown style guide
- [ ] Cross-references are valid
- [ ] Examples are tested and work
- [ ] No sensitive information (secrets, internal IPs)

### Code Review

**Required Reviews**:
- **Core modules**: 2x approvals (1 must be maintainer)
- **Starters**: 2x approvals (1 must be platform team)
- **Adapters**: 1x approval (subject matter expert)
- **Tests**: 1x approval
- **Examples**: 1x approval

**Review Checklist**:
- [ ] Code follows style guide and conventions
- [ ] Tests cover new functionality
- [ ] Documentation updated
- [ ] No security vulnerabilities
- [ ] Performance considerations addressed
- [ ] Error handling appropriate
- [ ] Logging appropriate (no PII)
- [ ] Configuration externalized

### Approval Workflow

1. **Create PR** with descriptive title and body
2. **Automated checks** (CI, tests, linting)
3. **Peer review** (minimum required approvals)
4. **Address feedback** and update PR
5. **Final approval** from maintainer
6. **Merge** using squash or rebase (no merge commits to main)
7. **Tag release** if applicable

---

## Development Hygiene Checklist

Use this checklist for every significant code change:

### Pre-Development

- [ ] Review related documentation
- [ ] Check for existing similar implementations
- [ ] Discuss design with team if needed
- [ ] Create ticket/issue if not exists

### During Development

- [ ] Follow coding conventions
- [ ] Write tests alongside code
- [ ] Update documentation as you go
- [ ] Commit frequently with clear messages
- [ ] Document AI agent sessions (if applicable)

### Pre-Commit

- [ ] Run full test suite: `./gradlew test`
- [ ] Check code formatting: `./gradlew spotlessCheck`
- [ ] Review diff for sensitive data
- [ ] Update relevant documentation
- [ ] No commented-out code or TODOs without tickets

### Pre-PR

- [ ] Rebase on latest main/develop
- [ ] Squash WIP commits if needed
- [ ] Write clear PR description
- [ ] Link to related issues
- [ ] Add screenshots/examples if UI changes
- [ ] Verify CI passes

### Post-Merge

- [ ] Verify deployment (if applicable)
- [ ] Update project board/tracker
- [ ] Close related issues
- [ ] Notify stakeholders if needed

---

## Tool Configuration

### EditorConfig

All editors should honor `.editorconfig`:

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true
indent_style = space

[*.{java,kt}]
indent_size = 4
max_line_length = 150

[*.{yaml,yml}]
indent_size = 2

[*.md]
max_line_length = 120
trim_trailing_whitespace = false
```

### Spotless (Code Formatting)

Configure in `build.gradle.kts`:

```kotlin
spotless {
    java {
        googleJavaFormat()
        importOrder()
        removeUnusedImports()
    }
    kotlin {
        ktlint()
    }
}
```

Run: `./gradlew spotlessApply` to auto-format

---

## Research & Publications

### Purpose

Regulus is not only a production platform but also a research contribution to the software engineering community. We share our knowledge, experience, and innovations through academic and industry publications.

### Publication Directory

**Location**: `publications/`

**Structure**:
```
publications/
├── papers/              # Conference and journal papers
├── presentations/       # Slides and talks
├── posters/            # Research posters
├── datasets/           # Public datasets
└── benchmarks/         # Performance benchmarks
```

### Contributing to Publications

**Authorship Eligibility**:
- Substantial code contributions (>1000 LOC or critical components)
- Architecture or design leadership
- Research contributions (evaluation, benchmarks)
- Significant writing (>25% of paper)
- Pilot deployment support

**Process**:
1. Review `../publications/PUBLICATION_GUIDELINES.md` for complete guidelines
2. Propose paper idea to research committee
3. Collaborate on drafting and revision
4. Obtain legal/compliance approval for banking content
5. Submit to target conference/journal

**Key Guidelines**:
- **No Customer Data**: Papers must not include any customer data
- **Anonymization**: Bank names and details must be anonymized
- **Accuracy**: All claims must be factually accurate and reviewed
- **Open Science**: Code and benchmarks publicly available

### Publication Types

**Conference Papers**:
- Industrial experience (ICSE SEIP, FSE Industry)
- Research papers (MLSys, SoCC)
- Tool demos (ICSE Demos)

**Journal Articles**:
- Practitioner-focused (IEEE Software, ACM Queue)
- Research journals (IEEE TSE, ACM TOSEM)

**Other Outputs**:
- Technical reports (arXiv)
- Blog posts and articles
- Conference tutorials

### Current Publications

See `../publications/README.md` for current status and pipeline.

**Main Paper**:
- *Regulus: A Governance-First Platform for Compliant AI Agents in Financial Services*
- Target: ICSE 2025 SEIP Track
- Outline: `../publications/papers/regulus-icse-2025/PAPER_OUTLINE.md`

### Research Ethics

**Confidentiality**:
- No customer or transaction data (even anonymized)
- Bank approvals required for pilot metrics
- Internal system details redacted

**Integrity**:
- No plagiarism, fabrication, or falsification
- All authors contributed; all contributors listed
- Conflicts of interest disclosed

**Open Science Commitment**:
- Platform code is open source
- Benchmark datasets publicly available
- Reproducibility packages with papers
- Artifact evaluation participation

### Resources

- **Guidelines**: `publications/PUBLICATION_GUIDELINES.md`
- **Paper Templates**: `publications/papers/templates/`
- **Presentation Templates**: `publications/presentations/templates/`
- **Dataset Documentation**: `../publications/benchmarks/README.md`

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2025-01-XX | Initial project conventions | Platform Team |
