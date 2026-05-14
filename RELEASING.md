# Releasing Regulus

This document is the canonical reference for cutting a Regulus release.
Read it once before the first release; refer back on each subsequent one.

## Audience

You're a maintainer with push access to `main` and the ability to manage
secrets on `github.com/neul-labs/regulus`. If you're a contributor, you
don't need this doc — your PR lands on `main`, the maintainer cuts the
release.

## What "release" actually means

Tagging `vX.Y.Z` on `main` triggers `.github/workflows/release.yml`,
which produces six artefacts in three tiers:

| Tier | Artefact | Where it goes | Secrets required |
|---|---|---|---|
| 1 | GitHub Release (Markdown body from CHANGELOG.md + `regulus-cli-X.Y.Z.jar` attached) | `github.com/neul-labs/regulus/releases` | None — uses `GITHUB_TOKEN` |
| 2 | Reference container image | `ghcr.io/neul-labs/regulus-adk-demo:X.Y.Z` | None — uses `GITHUB_TOKEN` |
| 3 | Maven Central artefacts (all `com.neullabs:*`) | `central.sonatype.com` → Maven Central | `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`, `SIGNING_KEY`, `SIGNING_PASSWORD` |
| 3 | Gradle Plugin Portal (`com.neullabs.compliance`) | `plugins.gradle.org` | `GRADLE_PUBLISH_KEY`, `GRADLE_PUBLISH_SECRET` |

**Tier 1 + Tier 2 work out of the box.** Tier 3 requires one-time admin
setup (see below) and is gated in the workflow on the relevant secrets
being present.

> **Note on Maven Central.** Sonatype's legacy OSSRH staging-repo flow
> (`s01.oss.sonatype.org`) was **sunset on 30 June 2025**. We publish
> exclusively via the new Central Portal at `central.sonatype.com`,
> using the community-maintained `com.vanniktech.maven.publish` Gradle
> plugin (there's no official Sonatype Gradle plugin yet). The plugin
> assembles a Maven-layout bundle, signs each artefact with the GPG
> key in-memory, POSTs to the Portal's REST API, polls for validation,
> and auto-releases on success — no web-UI clicks per release.

## One-time admin setup

These are the things you (or whoever owns the org) have to do once,
before any tag-triggered Maven Central or Gradle Plugin Portal
publication will succeed. **None of this is in scope for the repo
itself — it's outside-the-code work.**

### 1. Sonatype Central Portal — namespace verification

We publish under the `com.neullabs` Maven groupId. The Portal requires
you to prove ownership of the `neullabs.com` domain.

1. Log in to <https://central.sonatype.com>.
2. Username menu → **View Namespaces** → **Add Namespace** → enter
   `com.neullabs`. State becomes "Unverified".
3. Click **Verify Namespace**. The Portal generates a verification key
   (UUID-shaped string).
4. At your DNS registrar for `neullabs.com`, add a **TXT record**:
   - **Host/Name:** `neullabs.com` (the apex — sometimes shown as `@`).
   - **Value:** the verification key.
   - **TTL:** default (300s or whatever the registrar suggests).
5. Back on the Portal, confirm the verification. Propagation typically
   takes minutes; can take a few hours.

You only need the TXT record on the apex `neullabs.com`. Subdomains
like `docs.neullabs.com` / `regulus.neullabs.com` are unrelated.

### 2. Generate a Portal user token

Username menu → **View Account** → **Generate User Token**.

The Portal gives you a **token username** + **token password**. These
are *not* your Portal account login — they're machine credentials used
by the publishing plugin.

Store them as GitHub secrets:
- `SONATYPE_USERNAME` ← token username
- `SONATYPE_PASSWORD` ← token password

(We keep the secret names from the OSSRH era so the workflow doesn't
need renames; the values themselves are different.)

### 3. GPG signing key

Maven Central requires every artefact to be GPG-signed.

```bash
gpg --full-generate-key                          # RSA 4096, no expiry (or set one)
gpg --list-secret-keys --keyid-format=long
gpg --armor --export-secret-keys <KEY_ID>        # → SIGNING_KEY value
gpg --keyserver hkp://keys.openpgp.org --send-keys <KEY_ID>
# Mirror to a backup keyserver for redundancy:
gpg --keyserver hkp://keyserver.ubuntu.com --send-keys <KEY_ID>
```

Store as GitHub secrets:
- `SIGNING_KEY` ← full ASCII-armored block (includes the
  `-----BEGIN/END PGP PRIVATE KEY BLOCK-----` lines)
- `SIGNING_PASSWORD` ← passphrase

The workflow passes these in as
`ORG_GRADLE_PROJECT_signingInMemoryKey` and
`ORG_GRADLE_PROJECT_signingInMemoryKeyPassword`, so the plugin signs
without needing GPG on the runner.

### 4. GHCR package permissions (first push only)

The first push to `ghcr.io/neul-labs/regulus-adk-demo` requires the
package to exist with write access for the `regulus` repository's
`GITHUB_TOKEN`. GitHub's auto-creation usually works, but for an
org-level namespace it can fail with `blob upload unknown to registry`
on the first attempt.

If that happens:

1. Open <https://github.com/orgs/neul-labs/packages> → create the
   package manually (push a placeholder image once with an org-admin
   PAT, or use the Packages UI's "create" flow once available).
2. In the package's "Manage Actions Access" settings, grant the
   `neul-labs/regulus` repository **Write** access.
3. Re-run the failed `ghcr` job from the workflow run page.

After the first successful push the package's permissions persist, so
subsequent releases just work.

The release workflow marks the `ghcr` job `continue-on-error: true`, so
a GHCR failure doesn't fail the overall release run. The GitHub Release
+ CLI jar (Tier 1) is the canonical artefact.

### 5. Gradle Plugin Portal API key

<https://plugins.gradle.org/> → create account → API Keys → generate.
You'll get a key + secret. Store as `GRADLE_PUBLISH_KEY` and
`GRADLE_PUBLISH_SECRET` GitHub secrets.

### 6. Configure GitHub secrets

In the repo settings → Secrets and variables → Actions, add:

- `SONATYPE_USERNAME` — Portal token username (§2)
- `SONATYPE_PASSWORD` — Portal token password (§2)
- `SIGNING_KEY` — ASCII-armored GPG private key block (§3)
- `SIGNING_PASSWORD` — GPG passphrase (§3)
- `GRADLE_PUBLISH_KEY` — Plugin Portal key (§5)
- `GRADLE_PUBLISH_SECRET` — Plugin Portal secret (§5)

`GITHUB_TOKEN` is provided automatically.

## Cutting a release

Once admin setup is done (or skipped — Tier 3 just won't fire):

### 1. Land all changes on `main`

PRs merged. CI green. CHANGELOG.md updated under `## [Unreleased]` for
every change as it lands. (We don't auto-generate; we curate.)

### 2. Move the `[Unreleased]` block to `[X.Y.Z]`

```bash
# Edit CHANGELOG.md:
# - Rename ## [Unreleased] heading to ## [X.Y.Z] — YYYY-MM-DD
# - Add a fresh empty ## [Unreleased] section above it
# - Update the link references at the bottom
git add CHANGELOG.md
git commit -m "Release vX.Y.Z"
git push origin main
```

### 3. Tag and push

```bash
git tag -a vX.Y.Z -m "vX.Y.Z" -s            # -s signs with your GPG key
git push origin vX.Y.Z
```

Tag-push triggers `release.yml`.

### 4. Verify the release

- Watch the GitHub Actions run. All steps should be green or
  conditionally-skipped (Tier 3 steps if their secrets aren't set).
- Check `github.com/neul-labs/regulus/releases/tag/vX.Y.Z`: the release
  page is auto-populated from CHANGELOG.md; the CLI fat jar is
  attached as `regulus-cli-X.Y.Z.jar`.
- Check `ghcr.io/neul-labs/regulus-adk-demo:X.Y.Z` is reachable
  (`docker pull` test).
- If Tier 3 was enabled:
  - **Maven Central**: check the Deployments tab on the Central Portal
    at <https://central.sonatype.com/publishing/deployments>. With
    `automaticRelease = true` (our default), validation runs and the
    artefacts land in Maven Central. Propagation to
    `repo1.maven.org/maven2/com/neullabs/...` takes 30 min – 4 h.
    Namespace landing page: <https://central.sonatype.com/namespace/com.neullabs>.
  - **Gradle Plugin Portal**:
    <https://plugins.gradle.org/plugin/com.neullabs.compliance> —
    usually live within minutes.

### 5. Smoke-test the installer

```bash
curl -fsSL https://raw.githubusercontent.com/neul-labs/regulus/main/install.sh | sh
~/.regulus/bin/regulus --version          # should print vX.Y.Z
```

## Versioning policy

Following [SemVer](https://semver.org).

- `0.x.y` — public preview. Minor (`0.x → 0.(x+1)`) may include
  breaking changes. Patches (`0.x.y → 0.x.(y+1)`) do not.
- `1.x.y` onwards — strict SemVer.

ADK pin: tested against a known-good `com.google.adk:google-adk` version
in the BOM. CI runs nightly against `1.+` to surface upstream drift; if
ADK ships a breaking change we follow with a Regulus minor.

## Hotfixes

For a security-critical fix on a released version:

1. Branch from the most recent release tag.
2. Apply the fix; bump the patch version in `CHANGELOG.md`.
3. Tag and push as above. The release.yml workflow runs the same way.
4. Cherry-pick to `main` if applicable.

## What to do when something goes wrong

- **Maven Central publish fails on validation** — open the deployment
  at <https://central.sonatype.com/publishing/deployments>. The
  Validation Issues panel lists the specific failures (missing POM
  fields, unsigned artefacts, namespace mismatch, etc.). Fix locally
  and re-run the failing `maven-central` job from the workflow run
  page; the plugin idempotently re-uploads.
- **Maven Central publish fails on Portal token** — regenerate via
  username menu → View Account → Generate User Token, then update the
  `SONATYPE_USERNAME` / `SONATYPE_PASSWORD` secrets.
- **Maven Central publish fails on signing** — confirm the
  `SIGNING_KEY` secret is the *full* ASCII-armored block including
  the BEGIN/END lines. The most common failure is a key that wasn't
  exported with `--armor`.
- **Plugin Portal publish fails** — usually a key issue. Regenerate at
  <https://plugins.gradle.org/u/<account>/api-keys>.
- **GHCR push fails** — token scope. Confirm the workflow has
  `packages: write` permission. First-push package-permission setup is
  documented in §4 above.
- **GitHub Release didn't get the jar** — check the `Release` step of
  the workflow; the `gh release upload` may have raced ahead of the
  jar's build. Re-run the workflow against the existing tag.

## Pre-release checklist

Before every tag:

- [ ] All PRs for the release are merged.
- [ ] `CHANGELOG.md` `## [Unreleased]` section is populated with every
      change, grouped sensibly.
- [ ] `gradle.properties` `regulusVersion` reflects the target version
      (or rely on CI's `-PregulusVersion=` override — see release.yml).
- [ ] CI on `main` is green (PR workflow and any nightlies).
- [ ] `./gradlew build` runs cleanly locally.
- [ ] `./gradlew :platform:cli:regulus-cli:shadowJar` produces a jar
      that runs (`java -jar build/libs/regulus-cli-*.jar --version`).
- [ ] **Verify ADK 1.2.0 is available on Maven Central:**
      `curl -sf https://repo1.maven.org/maven2/com/google/adk/google-adk/1.2.0/`
      (returns 200; the page may be a directory listing or 404 redirect).
      The nightly workflow tracks the latest version; if `adk-drift`
      issues are open, decide whether to release against the pinned
      version or bump.
- [ ] **Verify the `com.neullabs` namespace is still verified** at
      <https://central.sonatype.com/namespaces>. DNS TXT records can
      get deleted; if the namespace shows as Unverified, re-verify
      before tagging.
- [ ] The funnel pages still mention the correct version in copy-paste
      blocks where they don't auto-link via `{{regulusVersion}}`.
- [ ] No `Skelf-Research` / `skelfresearch.com` references reintroduced
      (this is a recurring footgun — `git grep -i skelf` to verify).
- [ ] No `com.regulus.platform` references reintroduced
      (`git grep com.regulus.platform` returns zero).
- [ ] No `s01.oss.sonatype.org` references reintroduced (OSSRH is gone;
      `git grep oss.sonatype.org` returns zero).
