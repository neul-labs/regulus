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
| 3 | Maven Central artefacts (all `com.regulus.platform:*`) | `central.sonatype.com` | `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`, `SIGNING_KEY`, `SIGNING_PASSWORD` |
| 3 | Gradle Plugin Portal (`com.regulus.compliance`) | `plugins.gradle.org` | `GRADLE_PUBLISH_KEY`, `GRADLE_PUBLISH_SECRET` |

**Tier 1 + Tier 2 work out of the box.** Tier 3 requires one-time admin
setup (see below) and is gated in the workflow on the relevant secrets
being present.

## One-time admin setup

These are the things you (or whoever owns the org) have to do once,
before any tag-triggered Maven Central or Gradle Plugin Portal
publication will succeed. **None of this is in scope for the repo
itself — it's outside-the-code work.**

### 1. Sonatype OSSRH namespace verification

Maven Central requires you to prove ownership of the `com.regulus.platform`
coordinate. Two options:

- **DNS TXT** — fastest if you own a domain matching the namespace
  (you'd need `regulus.platform`; we don't, so skip).
- **GitHub repo verification (Recommended)** — Sonatype Central supports
  publishing from the `io.github.<username>` namespace by verifying the
  GitHub username. To get `com.regulus.platform` instead, you'll need a
  custom-namespace request to Central Sonatype. Reach out via their
  intake at <https://central.sonatype.org/register/central-portal/>.

Once verified, you'll have:

- A Sonatype username (your portal login).
- A user token (their "user token" feature in the portal UI) — use this
  as `SONATYPE_PASSWORD`.

### 2. GPG signing key

Maven Central requires every artefact to be GPG-signed.

```bash
gpg --full-generate-key                          # RSA 4096, no expiry (or set one)
gpg --list-secret-keys --keyid-format=long
gpg --armor --export-secret-keys <KEY_ID>        # → SIGNING_KEY value
gpg --keyserver hkp://keys.openpgp.org --send-keys <KEY_ID>
# Mirror to a backup keyserver for redundancy:
gpg --keyserver hkp://keyserver.ubuntu.com --send-keys <KEY_ID>
```

Store the armored private key as the `SIGNING_KEY` secret. Store the
passphrase as `SIGNING_PASSWORD`.

### 3. GHCR package permissions (first push only)

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

### 4. Gradle Plugin Portal API key

<https://plugins.gradle.org/> → create account → API Keys → generate.
You'll get a key + secret. Store as `GRADLE_PUBLISH_KEY` and
`GRADLE_PUBLISH_SECRET` GitHub secrets.

### 5. Configure GitHub secrets

In the repo settings → Secrets and variables → Actions, add:

- `SONATYPE_USERNAME`
- `SONATYPE_PASSWORD`
- `SIGNING_KEY` (full ASCII-armored block, with `-----BEGIN/END PGP PRIVATE KEY BLOCK-----`)
- `SIGNING_PASSWORD`
- `GRADLE_PUBLISH_KEY`
- `GRADLE_PUBLISH_SECRET`

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
  - Maven Central: check <https://central.sonatype.com/namespace/com.regulus.platform>
    — propagation can take 30 min to 4 hours.
  - Gradle Plugin Portal:
    <https://plugins.gradle.org/plugin/com.regulus.compliance> —
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

- **Sonatype publish fails** — the staging repository is left open. Log
  in to <https://s01.oss.sonatype.org/> (or the Central Portal),
  inspect, drop the staging repo, re-run.
- **Plugin Portal publish fails** — usually a key issue. Regenerate at
  <https://plugins.gradle.org/u/<account>/api-keys>.
- **GHCR push fails** — token scope. Confirm the workflow has
  `packages: write` permission.
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
- [ ] The funnel pages still mention the correct version in copy-paste
      blocks where they don't auto-link via `{{regulusVersion}}`.
- [ ] No `Skelf-Research` / `skelfresearch.com` references reintroduced
      (this is a recurring footgun — `git grep -i skelf` to verify).
