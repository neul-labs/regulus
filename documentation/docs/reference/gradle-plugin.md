# Gradle plugin (`com.regulus.compliance`)

Apply:

```kotlin
plugins {
    id("com.regulus.compliance") version "0.1.0"
}

regulusCompliance {
    profiles = listOf("eu-ai-act", "uk-gdpr", "fca-sysc")
    policySources = listOf("src/main/resources/policies")
    matrixOutput = "build/regulus/coverage-matrix.md"
    adkVersion = "1.2.0"
}
```

## Tasks

### `regulusComplianceScan`

Fails the build if `regulusCompliance.profiles` is empty. Wire into your
verification phase:

```kotlin
tasks.named("check") { dependsOn("regulusComplianceScan") }
```

### `regulusPolicyCompile`

Compiles YAML / Kotlin DSL policies under `policySources` into a packaged
resource consumed by `RegulusPolicyPlugin.withDecider`.

### `regulusComplianceMatrix`

Renders the regulation × control matrix as Markdown. Useful for
checking in alongside docs:

```bash
./gradlew regulusComplianceMatrix
# writes build/regulus/coverage-matrix.md
```

### `regulusAdkDoctor`

Sanity-checks the project's ADK + Regulus wiring. Warns on:

- `com.google.adk:google-adk` missing from the classpath.
- `regulusCompliance.profiles` empty.

Run before `adk deploy`:

```bash
./gradlew regulusAdkDoctor
```

## Why this mirrors ADK's own `maven_plugin/`

ADK ships an official Maven plugin in `google/adk-java/maven_plugin/` with
similar UX. Regulus' Gradle plugin matches the shape so the cross-
toolchain story is symmetric for any developer moving between ADK's own
tooling and the Regulus extensions.
