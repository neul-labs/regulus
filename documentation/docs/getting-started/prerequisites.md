# Prerequisites

Tools and accounts you need before any quickstart.

## Local tooling

- **Java 21**. Verify with `java -version`.
- **Gradle 8.5+** (or use the project's `./gradlew` wrapper).
- **`gcloud` CLI** for GCP authentication.

## GCP setup

- A GCP project with the **Vertex AI API** enabled.
- **Application Default Credentials** set up — run
  `gcloud auth application-default login` once.
- A **`europe-west2`** (London) or **`europe-west1`** (Belgium) region
  selected for Vertex AI, depending on whether you need UK or EU
  residency.

## Optional but recommended

- **Cloud KMS** with a CMEK in your chosen region — needed for the
  `dora`, `fca-sysc`, `pra-*`, `nhs-dspt`, `ehds` profiles.
- **Confluent Cloud / Google-managed Kafka** for the audit sink in
  production. Stdout is fine for development.

## Quick verification

```bash
java -version
gcloud auth application-default print-access-token  # should print a token
gcloud config get-value project                      # should print your project
```

If any of these fail, the quickstart will too. Fix here first.

## What you do NOT need

- Anything from `langchain4j`. Regulus' ADK path doesn't require it.
- A pre-existing audit pipeline. Regulus ships one (Kafka or stdout).
- A pre-existing policy DSL. Regulus' profiles cover the common ground;
  custom policies are an opt-in.
