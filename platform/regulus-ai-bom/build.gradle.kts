plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform("org.springframework.boot:spring-boot-dependencies:3.3.0"))

    constraints {
        // Google Agent Development Kit — primary runtime
        api("com.google.adk:google-adk:1.2.0")
        api("com.google.adk:google-adk-dev:1.2.0")

        // LangChain4j — alternative runtime, retained for legacy paths
        api("dev.langchain4j:langchain4j:0.35.0")
        api("dev.langchain4j:langchain4j-core:0.35.0")
        api("dev.langchain4j:langchain4j-spring-boot-starter:0.35.0")
        api("dev.langchain4j:langchain4j-open-ai:0.35.0")
        api("dev.langchain4j:langchain4j-azure-open-ai:0.35.0")
        api("dev.langchain4j:langchain4j-ollama:0.35.0")
        api("dev.langchain4j:langchain4j-embeddings:0.35.0")

        // Regulus Platform Modules — ADK extension surface
        api("com.regulus.platform:regulus-ai-adk-plugins:0.1.0-SNAPSHOT")
        api("com.regulus.platform:regulus-ai-adk-services:0.1.0-SNAPSHOT")
        api("com.regulus.platform:regulus-ai-adk-a2a:0.1.0-SNAPSHOT")
        api("com.regulus.platform:regulus-ai-compliance:0.1.0-SNAPSHOT")
        api("com.regulus.platform:regulus-ai-governance:0.1.0-SNAPSHOT")
        api("com.regulus.platform:regulus-ai-grc-adapters:0.1.0-SNAPSHOT")
        api("com.regulus.platform:regulus-ai-adk-spring-boot-starter:0.1.0-SNAPSHOT")

        // Regulus Platform Modules — existing
        api("com.regulus.platform:regulus-ai-agents-spring-boot-starter:0.1.0-SNAPSHOT")
        api("com.regulus.platform:regulus-ai-governance-starter:0.1.0-SNAPSHOT")
        api("com.regulus.platform:regulus-ai-safety-starter:0.1.0-SNAPSHOT")
        api("com.regulus.platform:regulus-ai-policy:0.1.0-SNAPSHOT")
        api("com.regulus.platform:regulus-ai-privacy:0.1.0-SNAPSHOT")
        api("com.regulus.platform:regulus-ai-kill-switch:0.1.0-SNAPSHOT")
        api("com.regulus.platform:regulus-ai-observability:0.1.0-SNAPSHOT")

        // Spring Boot Starters
        api("org.springframework.boot:spring-boot-starter-aop")
        api("org.springframework.boot:spring-boot-starter-validation")
        api("org.springframework.boot:spring-boot-starter-actuator")
        api("org.springframework.boot:spring-boot-configuration-processor")

        // Resilience
        api("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
        api("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
        api("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")
        api("io.github.resilience4j:resilience4j-micrometer:2.2.0")

        // Observability
        api("io.micrometer:micrometer-core:1.13.0")
        api("io.micrometer:micrometer-registry-prometheus:1.13.0")
        api("io.opentelemetry:opentelemetry-api:1.38.0")
        api("io.opentelemetry:opentelemetry-sdk:1.38.0")
        api("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter:2.4.0-alpha")

        // Kafka for audit logging
        api("org.springframework.kafka:spring-kafka:3.2.0")

        // JSON processing
        api("com.jayway.jsonpath:json-path:2.9.0")
        api("com.fasterxml.jackson.core:jackson-databind:2.17.0")

        // Validation
        api("org.hibernate.validator:hibernate-validator:8.0.1.Final")

        // Testing
        api("org.springframework.boot:spring-boot-starter-test")
        api("org.assertj:assertj-core:3.25.3")
        api("org.mockito:mockito-core:5.11.0")
    }
}
