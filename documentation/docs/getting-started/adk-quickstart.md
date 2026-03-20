# ADK quickstart

From zero to a working ADK + Regulus agent in 10 minutes.

If you've never used ADK before, this page also covers the ADK basics.

## 1. Project setup

`build.gradle.kts`:

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.adk:google-adk:1.2.0")
    implementation(platform("com.regulus.platform:regulus-ai-bom:0.1.0"))
    implementation("com.regulus.platform:regulus-ai-adk-spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
}
```

## 2. Configuration

`src/main/resources/application.yaml`:

```yaml
regulus:
  compliance:
    profiles: [uk-gdpr]            # one profile is enough to start
  adk:
    name: my-first-agent
    session-service:
      kind: in-memory               # vertex-ai or firestore for prod
    audit:
      sink: stdout
    residency:
      allowed-regions: [europe-west2]
    model-risk:
      tenant-tier: STANDARD
```

## 3. The Spring Boot entrypoint

```java
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
```

## 4. A minimal agent

```java
@Component
class MyAgent {

    @Bean
    LlmAgent rootAgent() {
        return LlmAgent.builder()
            .name("greeter")
            .model("gemini-2.5-flash")
            .instruction("Greet the user, then ask how you can help.")
            .build();
    }
}
```

## 5. Run

```bash
./gradlew bootRun
```

Startup logs show each Regulus plugin registering. Make a request and
inspect the stdout audit lines.

## What just happened

In ~30 lines of code you got an ADK agent with policy guards, privacy
redaction, audit emission, kill-switch readiness, model-risk gating, and
residency-pinned to UK. Each control is a `BasePlugin` registered on the
ADK `App` by the Spring auto-config.

## Next

- [Your first compliant agent](first-compliant-agent.md) — add more
  profiles and see what changes.
- [Multi-agent with A2A](multi-agent-a2a.md) — agents talking to agents.
- [Deploy to Vertex AI Agent Engine](vertex-deploy.md) — production path.
