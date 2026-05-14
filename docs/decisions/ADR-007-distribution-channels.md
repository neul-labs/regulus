# ADR-007: Distribution channels

- Status: Accepted
- Date: 2026-04-02

## Context

For Regulus to be a "default reference" extension for ADK, it has to be
trivially consumable by ADK Java users. We need to decide where to publish
and what UX to mirror.

## Decision

Three primary channels, in priority order:

1. **Maven Central** — the only thing that matters for Java/Kotlin/Spring
   Boot consumers. Coordinates `com.regulus.platform:*`. Sonatype OSSRH
   staging with auto-release on `v*` tag.

2. **Gradle Plugin Portal** — plugin ID `com.regulus.compliance`. UX
   mirrors ADK's own `maven_plugin/` module so the cross-toolchain story
   is symmetric.

3. **GitHub Container Registry** — reference container image for the
   Vertex deploy example at `ghcr.io/neul-labs/regulus-adk-demo`.

Plus a mirror to GitHub Packages for users who prefer that.

## Not in scope

- **PyPI.** ADK Python is the most mature ADK SDK, but Regulus is Java-
  only by scope (see ADR-006). A Python sibling would be a separate
  project.
- **Homebrew.** Not appropriate for a library.
- **JCenter.** Sunset.

## Why these channels

- Maven Central is the only place a Gradle/Maven user expects to find a
  JVM library. Anything else is friction.
- The Gradle Plugin Portal is the only way to ship a Gradle plugin people
  will discover by ID. We could mirror to Maven Central but the portal is
  the source of truth.
- GHCR matches where Neul Labs already ships container images;
  picking a public registry that ADK users already trust matters.

## Pre-release admin

- Sonatype OSSRH namespace verification for `com.regulus.platform`
  (DNS TXT or GitHub-repo verification).
- GPG signing key generated and exposed to CI.
- Gradle Plugin Portal API key.

## Consequences

Positive: maximum discoverability for the audience we care about.

Negative: three CI publishing paths to operate. Mitigated by the single
`release.yml` workflow that runs all three.
