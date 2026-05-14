# CLI (`regulus`)

The Regulus CLI scaffolds compliant ADK agents and sanity-checks
projects. Lives at `~/.regulus/bin/regulus` after install (see
[Install the CLI](../getting-started/install-cli.md)).

## Subcommands

### `regulus init <name>`

Scaffold a new ADK + Regulus agent project.

```bash
regulus init my-agent \
    --profiles=eu-ai-act,uk-gdpr,fca-sysc \
    --frameworks=nist-ai-rmf,iso-42001 \
    --grc-adapter=stdout \
    --region=europe-west2
```

Output:

```
my-agent/
├── build.gradle.kts          Regulus BOM + ADK + Spring Boot, wired
├── settings.gradle.kts
├── gradle.properties
├── .gitignore
├── README.md                 project README with run instructions
├── gradlew  gradlew.bat       shims; `gradle wrapper` materialises real wrapper
└── src/main/
    ├── java/<package>/AgentApplication.java   Spring Boot entrypoint
    ├── java/<package>/ChatController.java     POST /chat stub
    └── resources/
        ├── application.yaml   profiles, frameworks, grc, residency
        └── logback.xml
```

#### Flags

| Flag | Default | Description |
|---|---|---|
| `<name>` (positional) | — | Required. The directory and project name. |
| `--profiles`, `-p` | — | **Required.** Comma-separated `ComplianceProfile` ids. |
| `--frameworks`, `-f` | _empty_ | Comma-separated `GovernanceFramework` ids. |
| `--grc-adapter` | `stdout` | One of: `stdout`, `kafka`, `servicenow-irm`, `onetrust-ai-gov`, `metricstream`, `webhook`. |
| `--region` | `europe-west2` | Residency region pinned in `application.yaml`. |
| `--package` | `com.example.agent` | Java package for `AgentApplication`. |
| `--regulus-version` | bundled | Regulus version pinned in `build.gradle.kts`. |
| `--adk-version` | `1.2.0` | ADK version pinned in `build.gradle.kts`. |
| `--dir` | `.` | Parent directory to create the project under. |
| `--force` | `false` | Overwrite the target directory if it exists. |

#### Allowed values

**Profiles** (from `ComplianceProfiles`):

`eu-ai-act` · `gdpr` · `uk-gdpr` · `dora` · `nis2` · `fca-sysc` ·
`pra-ss1-23` · `pra-ss2-21` · `nhs-dspt` · `ehds`

**Frameworks** (from `GovernanceFrameworks`):

`nist-ai-rmf` · `nist-ai-rmf-600-1` · `nist-ai-rmf-agent-interop` ·
`iso-42001` · `iso-23894` · `iso-23053`

**GRC adapters**: see [GRC integration](../governance/grc/index.md).

### `regulus doctor`

Inspects a project directory and reports common configuration issues.
Same checks as the Gradle `regulusAdkDoctor` task, packaged for users
who don't have Gradle in front of them yet.

```bash
regulus doctor                  # current directory
regulus doctor --dir=my-agent   # specific directory
```

Output:

```
✓ build.gradle.kts present
✓ application.yaml present
✓ ADK dependency declared
✓ Regulus BOM / module declared
✓ Regulus YAML root present
✓ compliance section present
✓ profiles declared

regulus doctor: OK
```

Exit codes: `0` if all checks pass, `1` otherwise.

### `regulus --version` / `--help`

Standard Picocli surface. `regulus --help <sub>` for per-subcommand help.

## Exit codes

| Code | Meaning |
|---|---|
| `0` | Success. |
| `1` | Generic failure (doctor checks failed, etc.). |
| `2` | `init`: target directory exists; pass `--force` or pick a different name. |

## Environment variables

The CLI itself has no required env vars. The scaffolded project's
`application.yaml` references env vars for GRC adapter credentials —
documented per-adapter in [GRC integration](../governance/grc/index.md).

## What the CLI doesn't do

- **Run the scaffolded agent.** That's `./gradlew bootRun` inside the
  generated project (after `gradle wrapper`).
- **Publish to Maven Central.** Releases are CI-driven from
  `.github/workflows/release.yml`.
- **Talk to your GRC tool directly.** The scaffold wires the
  configured adapter; the running agent talks to the tool.
- **Touch existing source code.** `regulus init` always creates a new
  directory; it never edits files outside it.
