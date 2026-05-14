# Contributing to Regulus

Thank you for your interest in contributing to the Regulus platform! This guide will help you get started.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [How to Contribute](#how-to-contribute)
- [Coding Standards](#coding-standards)
- [Testing Requirements](#testing-requirements)
- [Documentation](#documentation)
- [Pull Request Process](#pull-request-process)
- [AI Agent Contributions](#ai-agent-contributions)

## Project conventions (read first)

- **Google ADK is the primary integration target.** New controls ship as
  `com.google.adk.plugins.BasePlugin` implementations or as extensions of
  ADK's official service interfaces (`SessionService`, `MemoryService`,
  `ArtifactService`, `EventCompactor`, `BaseComputer`). See
  [ADR-006](docs/decisions/ADR-006-adk-primary-runtime-and-extension-model.md).
- **Compliance docs follow the 12-section regtech-explainer template.** PRs
  that add a control without its docs page following the template will be
  sent back. See
  [ADR-009](docs/decisions/ADR-009-regtech-as-product-docs.md).
- **The LangChain4j path is retained, not extended.** Bug fixes welcome;
  new features go on the ADK path. See
  [ADR-004](docs/decisions/ADR-004-langchain4j-llm-abstraction.md) (now
  superseded) and ADR-006.
- **Residency is fail-closed at startup.** Don't add controls that warn
  and continue when a residency invariant is broken. See
  [ADR-008](docs/decisions/ADR-008-residency-by-construction.md).

## Releasing

Triggered by a `v*` tag on `main`. CI runs Sonatype publish + Gradle
Plugin Portal publish + Jib container build. See
[`docs/operations/release-and-deploy.md`](documentation/docs/operations/release-and-deploy.md)
for the operating runbook. Pre-release admin (Sonatype OSSRH namespace
verification, GPG key, plugin portal API key) is documented in
[ADR-007](docs/decisions/ADR-007-distribution-channels.md).

## Code of Conduct

This project adheres to a code of conduct adapted from the Contributor Covenant. By participating, you are expected to uphold this code. Please report unacceptable behavior to the project maintainers.

**Our Pledge**: We are committed to making participation in this project a harassment-free experience for everyone, regardless of level of experience, gender, gender identity and expression, sexual orientation, disability, personal appearance, body size, race, ethnicity, age, religion, or nationality.

## Getting Started

### Prerequisites

- **JDK 21** or higher
- **Gradle 8.x** (wrapper included)
- **Docker** and **Docker Compose** (for local development)
- **Git 2.x** or higher

### Project Structure

Familiarize yourself with the project structure and conventions:

```bash
# Read the project conventions
cat docs/project-conventions.md

# Explore the documentation structure
ls -R docs/

# Review the architecture
cat docs/architecture.md
```

Key documentation:
- `docs/project-conventions.md` - Project structure, naming, standards
- `docs/architecture.md` - System architecture overview
- `docs/developer-checklist.md` - End-to-end development guide
- `docs/product-roadmap.md` - Platform evolution roadmap

## Development Setup

### 1. Fork and Clone

```bash
# Fork the repository on GitHub, then:
git clone https://github.com/YOUR_USERNAME/regulus.git
cd regulus

# Add upstream remote
git remote add upstream https://github.com/regulus-platform/regulus.git
```

### 2. DevContainer (Recommended)

The easiest way to get started:

```bash
# Open in VS Code with Dev Containers extension
code .

# Or use the DevContainer CLI
devcontainer open .
```

The DevContainer includes JDK, Gradle, Docker, and all required tools.

### 3. Manual Setup

If not using DevContainer:

```bash
# Verify Java version
java -version  # Should be 17+

# Build the project
./gradlew build

# Run tests
./gradlew test

# Start local services
make run  # Starts LLM stub, vector DB, eval service, etc.
```

### 4. IDE Setup

**IntelliJ IDEA**:
- Import as Gradle project
- Enable annotation processing
- Install recommended plugins: Lombok, Spring Boot, Kotlin

**VS Code**:
- Install Java Extension Pack
- Install Spring Boot Extension Pack
- Install Gradle Extension

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in [Issues](https://github.com/regulus-platform/regulus/issues)
2. If not, create a new issue using the bug report template
3. Include:
   - Clear title and description
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details (OS, Java version, etc.)
   - Logs or stack traces

### Suggesting Enhancements

1. Check [existing enhancement requests](https://github.com/regulus-platform/regulus/issues?q=is%3Aissue+label%3Aenhancement)
2. Create a new issue using the feature request template
3. Describe:
   - Use case and motivation
   - Proposed solution
   - Alternatives considered
   - Impact on existing functionality

### Contributing Code

1. **Find or create an issue** for the work
2. **Comment on the issue** to let others know you're working on it
3. **Create a branch** following naming conventions:
   ```bash
   git checkout -b feature/REG-123-brief-description
   ```
4. **Make your changes** following coding standards
5. **Write tests** for new functionality
6. **Update documentation** as needed
7. **Commit your changes** using conventional commits
8. **Push and create a PR**

## Coding Standards

### Java Style

We follow Google Java Style Guide with minor modifications:

- **Indentation**: 4 spaces (not 2)
- **Line length**: 150 characters max
- **Imports**: Organize and remove unused
- **Formatting**: Use Spotless for automatic formatting

```bash
# Apply formatting
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck
```

### Kotlin Style

We follow Kotlin coding conventions:

- Use ktlint for formatting
- Prefer immutable types (`val` over `var`)
- Use data classes for DTOs
- Leverage Kotlin's null safety

### Naming Conventions

- **Classes**: `PascalCase` (e.g., `PolicyGuard`, `ModelInventoryClient`)
- **Methods/Functions**: `camelCase` (e.g., `validateLEI()`, `applyPrivacyFilters()`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `MAX_RETRY_ATTEMPTS`)
- **Packages**: lowercase, dot-separated (e.g., `com.neullabs.regulus.policy`)
- **Test Classes**: `ClassNameTest` or `ClassNameIT`

### Code Organization

- Follow layered architecture (config, model, service, adapter)
- Keep classes focused and single-responsibility
- Prefer composition over inheritance
- Use dependency injection (Spring)
- Avoid static methods/fields except for constants

## Testing Requirements

### Test Coverage

- **Minimum**: 80% line coverage for new code
- **Target**: 90% line coverage
- **Critical paths**: 100% coverage (policy guards, privacy filters, kill switch)

### Test Types

1. **Unit Tests** (`*Test.java`):
   - Fast, isolated, no external dependencies
   - Mock external services
   - Test edge cases and error handling

2. **Integration Tests** (`*IT.java`):
   - Test component interactions
   - Use Spring Boot test slices
   - Use testcontainers for external dependencies

3. **Contract Tests** (`*ContractTest.java`):
   - Verify MCP/A2A contracts
   - Use Spring Cloud Contract or Pact

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests PolicyGuardTest

# Run integration tests
./gradlew integrationTest

# Generate coverage report
./gradlew jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html
```

### Writing Good Tests

```java
// Good test structure: Given-When-Then
@Test
void shouldRejectRequestWhenLEIMissing() {
    // Given
    AgentRequest request = AgentRequest.builder()
        .purpose("KYC")
        .consent(true)
        .build();  // No LEI

    PolicyGuard guard = new LEIPolicyGuard();

    // When
    PolicyResult result = guard.evaluate(request);

    // Then
    assertThat(result.isAllowed()).isFalse();
    assertThat(result.getReason()).contains("LEI required");
}
```

## Documentation

### When to Update Documentation

Update documentation when you:
- Add a new feature or starter
- Change public APIs or configuration
- Fix a bug that users should know about
- Modify architecture or design
- Add or change operational procedures

### Documentation Types

1. **Code Documentation**:
   - JavaDoc for public APIs
   - Inline comments for complex logic
   - README in each module

2. **User Documentation** (`docs/`):
   - How-to guides for common tasks
   - Configuration reference
   - Troubleshooting guides

3. **Architecture Documentation**:
   - Architecture decision records (ADRs)
   - Design documents
   - Diagrams (Mermaid preferred)

### Documentation Standards

Follow the style guide in `docs/project-conventions.md`:

- Use Markdown for all documentation
- Follow heading hierarchy (single `#`, then `##`, etc.)
- Include code examples that actually work
- Link to related documentation
- Keep documentation close to the code it describes

## Pull Request Process

### Before Creating a PR

- [ ] Code follows style guide (run `./gradlew spotlessCheck`)
- [ ] Tests pass (run `./gradlew test`)
- [ ] Coverage meets requirements
- [ ] Documentation updated
- [ ] Commit messages follow conventional commits
- [ ] Branch is up to date with main/develop

### PR Title and Description

**Title**: Use conventional commits format:
```
feat(policy): add LEI validation guard
fix(dsl): resolve NPE in YAML parser
docs(guides): add troubleshooting for eval failures
```

**Description Template**:
```markdown
## Summary
[Brief description of changes]

## Motivation
[Why this change is needed]

## Changes Made
- [Change 1]
- [Change 2]

## Testing
[How the changes were tested]

## Screenshots
[If UI changes, include before/after screenshots]

## Checklist
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] CHANGELOG.md updated (if user-facing change)
- [ ] Breaking changes documented

## Related Issues
Closes #123
Related to #456
```

### Review Process

1. **Automated checks** run (CI, tests, linting)
2. **Reviewers assigned** based on CODEOWNERS
3. **Review feedback** addressed
4. **Approvals obtained** (minimum required based on change type)
5. **Merge** by maintainer (squash or rebase, no merge commits)

### Review Guidelines for Contributors

When your PR is reviewed:
- Respond to all comments (even with just "done" or "ack")
- Push changes as new commits during review
- Squash commits before final merge
- Be receptive to feedback and ask questions if unclear

## AI Agent Contributions

If you're using an AI coding agent (like Claude Code):

1. **Create session documentation** in `docs/agent-work/sessions/`
   - Use template: `docs/agent-work/sessions/template.md`
   - Filename: `YYYY-MM-DD-HHmm-agent-name.md`

2. **Document decisions** in `docs/agent-work/decisions/`
   - Explain rationale for choices made
   - Note alternatives considered

3. **Track progress** in `docs/agent-work/progress/`
   - Link sessions to feature work
   - Update status regularly

4. **Link session docs in PRs**:
   ```markdown
   ## Agent Session
   This PR was created with assistance from Claude Code.
   Session documentation: `docs/agent-work/sessions/2025-01-22-1430-claude-code.md`
   ```

See `docs/project-conventions.md#ai-agent-work-tracking` for complete guidelines.

## Development Workflow

### Feature Development

```bash
# 1. Sync with upstream
git checkout main
git pull upstream main

# 2. Create feature branch
git checkout -b feature/REG-123-add-feature

# 3. Make changes and commit frequently
git add .
git commit -m "feat(module): add feature xyz"

# 4. Push to your fork
git push origin feature/REG-123-add-feature

# 5. Create PR on GitHub
```

### Keeping Your Branch Updated

```bash
# Rebase on latest main
git fetch upstream
git rebase upstream/main

# If conflicts, resolve and continue
git add .
git rebase --continue

# Force push to your branch
git push origin feature/REG-123-add-feature --force
```

## Release Process

Releases are managed by maintainers:

1. Version bumped in `gradle.properties`
2. CHANGELOG.md updated
3. Release branch created (`release/vX.Y.Z`)
4. Final testing and fixes
5. Tag created and pushed
6. Artifacts published to Maven Central
7. GitHub release created with notes

Contributors don't need to worry about releases, but user-facing changes should be noted in PR descriptions for CHANGELOG inclusion.

## Getting Help

- **Documentation**: Check `docs/` first
- **Discussions**: GitHub Discussions for questions
- **Issues**: GitHub Issues for bugs and features
- **Chat**: Slack channel (link in README)

## Recognition

Contributors are recognized in:
- CONTRIBUTORS.md file
- Release notes
- GitHub insights

Significant contributions may result in:
- Committer status
- Maintainer role
- Speaking opportunities

## License

By contributing, you agree that your contributions will be licensed under the same license as the project (see LICENSE file).

---

Thank you for contributing to Regulus! Your efforts help make AI agents safer and more accessible for regulated financial services.
