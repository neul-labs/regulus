# Security Policy

## Reporting a vulnerability

**Do not open a public GitHub issue for a security vulnerability.**

Email `security@neullabs.com` with:

- A description of the vulnerability and its impact.
- Steps to reproduce, ideally with a minimal proof-of-concept.
- The affected Regulus version (or commit hash).
- Your name and affiliation if you'd like credit in the advisory.

We aim to:

- Acknowledge receipt within **2 business days**.
- Triage and validate within **5 business days**.
- Publish a fix and advisory within **30 days** for confirmed
  high/critical-severity issues, or sooner if a fix is straightforward.

## Scope

In scope:

- Regulus code in this repository — all `platform/`, `examples/`, the CLI,
  the Gradle plugin, scaffold templates, and documentation example code.
- Default GRC adapter configurations and field mappings.
- The `install.sh` installer and the CLI binary it deploys.

Out of scope:

- Vulnerabilities in Google ADK itself — report to Google
  (`google/adk-java` issue tracker for non-security; their security team
  via the procedure they publish).
- Vulnerabilities in your upstream GRC tool (ServiceNow, OneTrust,
  MetricStream) — report to the vendor.
- Vulnerabilities in your downstream agent code — Regulus enforces a
  control surface; correctness of *your* agent logic is your
  responsibility.
- Misconfiguration in the deploying tenant (e.g. a residency allowlist
  that doesn't match the tenant's data sovereignty obligations).

## Supported versions

Until Regulus reaches `1.0.0`, **only the latest minor version receives
security patches**. After `1.0.0`, we'll maintain the latest two minor
versions.

| Version | Supported |
|---|---|
| `0.1.x` | ✅ Yes |
| `< 0.1.0` | ❌ No |

## Severity rubric

We follow CVSS 3.1. Indicative classes:

- **Critical** — remote code execution; secret disclosure; ability to
  forge audit evidence undetectably.
- **High** — bypass of a Regulus control (policy guard, kill switch,
  residency check, model-risk tier); injection that escapes redaction.
- **Medium** — partial bypass requiring privileged context; denial of
  service via a single malformed input.
- **Low** — information disclosure that doesn't include personal data or
  cryptographic material.

## Disclosure

We coordinate disclosure with the reporter and (if applicable) with the
upstream maintainers of affected dependencies. Public advisories are
published as GitHub Security Advisories on this repository with
CVE assignment via GitHub.

## Public key

For sensitive communications you can encrypt to our public key (forthcoming
— request via the security mailbox).
