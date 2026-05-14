# Install the CLI

The `regulus` CLI is a single fat jar plus a tiny shell shim. Three
install paths; pick whichever your environment likes.

## One-line installer (recommended)

```bash
curl -fsSL https://raw.githubusercontent.com/neul-labs/regulus/main/install.sh | sh
```

What it does:

- Downloads the latest release jar to `~/.regulus/bin/regulus-cli.jar`.
- Drops a small shell wrapper at `~/.regulus/bin/regulus`.
- Prints the line you need to add to your shell profile:
  `export PATH="$HOME/.regulus/bin:$PATH"`.

Verify:

```bash
regulus --version
```

## Manual jar download

If you'd rather not pipe a script to `sh`:

1. Grab the latest release jar from
   <https://github.com/neul-labs/regulus/releases>.
2. Save it as `regulus-cli.jar`.
3. Run it with `java -jar regulus-cli.jar init my-agent ...`.

Optionally, drop a shell function in your profile:

```bash
regulus() { java -jar "$HOME/regulus-cli.jar" "$@"; }
```

## Build from source

If you're already in the Regulus repo:

```bash
git clone https://github.com/neul-labs/regulus.git
cd regulus
./gradlew :platform:cli:regulus-cli:shadowJar

# Run the freshly-built jar:
java -jar platform/cli/regulus-cli/build/libs/regulus-cli-*.jar --version
```

Useful when you want to test pre-release behaviour or contribute back.

## Through Gradle (no CLI install needed)

Inside an existing Gradle project, apply the Regulus plugin and use the
equivalent task:

```kotlin
// build.gradle.kts
plugins {
    id("com.regulus.compliance") version "0.1.0"
}
```

Then:

```bash
./gradlew initRegulusAgent \
    -PagentName=my-agent \
    -Pprofiles=eu-ai-act,uk-gdpr,fca-sysc \
    -Pframeworks=nist-ai-rmf,iso-42001
```

Produces identical output to `regulus init`.

## Requirements

- **Java 21+** for both the CLI and the scaffolded projects.
- **Gradle** is required to actually run the scaffolded project (the CLI
  generates the project; `./gradlew bootRun` runs it). The scaffold's
  `gradlew` shim defers to your system `gradle`; run `gradle wrapper`
  once after scaffolding to materialise a project-local wrapper.

## Uninstall

```bash
rm -rf ~/.regulus
# and remove the PATH line you added to your shell profile
```

## What next

- [ADK quickstart](adk-quickstart.md) — run the scaffolded agent.
- [Show me — the diff](../show-me.md) — what changes after `regulus init`.
- [CLI reference](../reference/cli.md) — every flag, every subcommand.
