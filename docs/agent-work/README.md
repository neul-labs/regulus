# AI Agent Work Tracking

This directory contains documentation of work performed by AI coding agents (like Claude Code) on the Regulus platform.

## Purpose

AI agents must document their work to ensure:
- **Transparency**: Clear record of what was changed and why
- **Knowledge Transfer**: Human developers can understand agent decisions
- **Audit Trail**: Complete history of automated changes
- **Context Preservation**: Future sessions can build on previous work

## Directory Structure

```
agent-work/
├── sessions/        # Individual work session logs
├── progress/        # Multi-session feature/epic tracking
└── decisions/       # Significant technical decisions
```

## Usage Guidelines

### When Starting a Work Session

1. Create a new session document in `sessions/`
2. Use filename format: `YYYY-MM-DD-HHmm-agent-name.md`
3. Use the template from `sessions/template.md`
4. Update the session document continuously as you work

### During Development

1. **Document decisions**: Create decision docs for significant choices
2. **Update progress**: If working on a tracked feature, update the progress doc
3. **Note issues**: Document any problems encountered and their resolutions
4. **List changes**: Keep accurate list of files created/modified/deleted

### When Ending a Session

1. **Finalize session doc**: Complete all sections
2. **Update progress tracking**: Link session to related feature progress
3. **Create TODOs**: List any follow-up items for humans or future sessions
4. **Flag review areas**: Note anything requiring human review

## Best Practices

### Be Specific

❌ Bad: "Fixed the policy guard"
✅ Good: "Implemented LEI validation in PolicyGuard using ISO 17442 regex pattern"

### Explain Rationale

Always include *why* decisions were made:

```markdown
**Decision**: Used AspectJ for policy guards instead of Spring AOP
**Rationale**: AspectJ supports compile-time weaving needed for low-latency requirements
```

### Link Context

Reference related work:

```markdown
**Related**:
- Feature progress: `docs/agent-work/progress/policy-guards.md`
- Architecture: `docs/architecture/architecture.md#policy--privacy-layer`
- Previous session: `docs/agent-work/sessions/2025-01-14-1530-claude-code.md`
```

### Flag Uncertainties

Be explicit about areas of uncertainty:

```markdown
## Needs Human Review

- ⚠️ **Performance**: Haven't benchmarked the regex validation - may need optimization
- ⚠️ **Security**: LEI validation logic should be reviewed by security team
```

## Templates

- **Session Log**: `sessions/template.md`
- **Progress Tracking**: `progress/template.md`
- **Decision Log**: `decisions/template.md`

## Examples

See `sessions/` for example session documentation from actual agent work.

## Integration with Development Process

1. **Pull Requests**: Link to session docs in PR descriptions
2. **Code Reviews**: Reviewers can reference session rationale
3. **Documentation**: Session notes inform user-facing documentation
4. **Retrospectives**: Session logs provide data for process improvement

## Questions?

Refer to `docs/project-conventions.md` for full guidelines on AI agent work tracking.
