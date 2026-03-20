# Installation

This guide covers adding Regulus to your project.

## Using the BOM

Regulus provides a Bill of Materials (BOM) for consistent dependency management:

=== "Gradle (Kotlin DSL)"

    ```kotlin title="build.gradle.kts"
    plugins {
        id("org.springframework.boot") version "3.3.0"
        id("io.spring.dependency-management") version "1.1.4"
        kotlin("jvm") version "1.9.22"
    }

    dependencyManagement {
        imports {
            mavenBom("com.regulus:regulus-ai-bom:0.1.0-SNAPSHOT")
        }
    }

    dependencies {
        // Core agent starter
        implementation("com.regulus:regulus-ai-agents-spring-boot-starter")

        // Safety features (kill switch, PII redaction)
        implementation("com.regulus:regulus-ai-safety-starter")

        // Governance (SS1/23 compliance)
        implementation("com.regulus:regulus-ai-governance-starter")
    }
    ```

=== "Gradle (Groovy)"

    ```groovy title="build.gradle"
    plugins {
        id 'org.springframework.boot' version '3.3.0'
        id 'io.spring.dependency-management' version '1.1.4'
        id 'java'
    }

    dependencyManagement {
        imports {
            mavenBom 'com.regulus:regulus-ai-bom:0.1.0-SNAPSHOT'
        }
    }

    dependencies {
        implementation 'com.regulus:regulus-ai-agents-spring-boot-starter'
        implementation 'com.regulus:regulus-ai-safety-starter'
        implementation 'com.regulus:regulus-ai-governance-starter'
    }
    ```

=== "Maven"

    ```xml title="pom.xml"
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.regulus</groupId>
                <artifactId>regulus-ai-bom</artifactId>
                <version>0.1.0-SNAPSHOT</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>com.regulus</groupId>
            <artifactId>regulus-ai-agents-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.regulus</groupId>
            <artifactId>regulus-ai-safety-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.regulus</groupId>
            <artifactId>regulus-ai-governance-starter</artifactId>
        </dependency>
    </dependencies>
    ```

## Available Starters

### regulus-ai-agents-spring-boot-starter

Core agent capabilities:

- LLM client abstraction (Gemini, OpenAI, Anthropic, Azure)
- ADK integration for agent orchestration
- MCP server/client for tool exposure
- A2A protocol for cross-agent communication
- Streaming response support

### regulus-ai-safety-starter

Safety and privacy controls:

- Kill switch with dual-control (4-eyes principle)
- PII detection and redaction (UK patterns)
- Data residency enforcement
- Prompt injection detection
- Circuit breakers and fallbacks

### regulus-ai-governance-starter

Regulatory compliance:

- SS1/23 model registry
- Model card generation
- Approval workflow integration
- Audit trail generation
- Consumer Duty mapping

### regulus-ai-payments-starter

Payment processing:

- ISO 20022 message support (PAIN, PACS, CAMT)
- CHAPS integration
- Open Banking connectors
- Transaction validation

## Repository Configuration

If using SNAPSHOT versions, add the Sonatype snapshot repository:

=== "Gradle"

    ```kotlin title="build.gradle.kts"
    repositories {
        mavenCentral()
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }
    }
    ```

=== "Maven"

    ```xml title="pom.xml"
    <repositories>
        <repository>
            <id>sonatype-snapshots</id>
            <url>https://s01.oss.sonatype.org/content/repositories/snapshots/</url>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
    </repositories>
    ```

## Verifying Installation

Create a simple health check to verify the installation:

```java
@RestController
@RequestMapping("/health")
public class HealthController {

    private final LlmClient llmClient;

    public HealthController(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @GetMapping("/llm")
    public Map<String, Object> checkLlm() {
        return Map.of(
            "status", "UP",
            "provider", llmClient.getProvider(),
            "model", llmClient.getModel()
        );
    }
}
```

Start your application and verify:

```bash
curl http://localhost:8080/health/llm
```

## Next Steps

- [Quick Start](quickstart.md) - Build your first agent
- [First Agent](first-agent.md) - Complete tutorial
