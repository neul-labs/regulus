# Release and deploy

How Regulus itself ships, and how to ship apps that depend on it.

## Releasing Regulus

Triggered by a `v*` tag on `main`. CI runs:

```
./gradlew \
  publishToSonatype \
  closeAndReleaseSonatypeStagingRepository \
  publishPlugins \
  jib
```

- Maven Central artifacts (everything under `platform/`) — staged at
  Sonatype OSSRH, auto-released.
- Gradle Plugin Portal — `com.regulus.compliance`.
- GHCR — reference container image for the Vertex deploy example.

## First-release prerequisites (one-time)

- Sonatype OSSRH namespace verification for `com.regulus.platform` (DNS
  TXT or GitHub repository verification).
- GPG signing key generated and exposed to CI as `SIGNING_KEY`,
  `SIGNING_PASSPHRASE`.
- Gradle Plugin Portal API key as `GRADLE_PUBLISH_KEY`,
  `GRADLE_PUBLISH_SECRET`.

## Versioning

- `0.x` — public preview. Breaking changes allowed at minor bumps.
- `1.x` — stable. SemVer-compatible patches and minors.
- ADK version compatibility: pin to a known-good ADK 1.x in the BOM;
  nightly CI runs against `1.+` to catch upstream drift.

## Releasing apps that use Regulus

Run `./gradlew regulusAdkDoctor` in CI before any deploy. It catches:

- Missing `com.google.adk:google-adk` from the classpath.
- Missing `regulusCompliance.profiles`.
- Misconfigured residency (location outside allowlist).
- CMEK required by the profile but no key configured.

Recommended `release.yml` step:

```yaml
- name: Pre-deploy doctor
  run: ./gradlew regulusAdkDoctor regulusComplianceScan
```
