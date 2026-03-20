plugins {
    `java-library`
}

description = "Regulus AI LLM - Multi-provider LLM abstraction layer"

dependencies {
    // Spring Boot
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-webflux")

    // LangChain4j Core
    api("dev.langchain4j:langchain4j:0.35.0")
    api("dev.langchain4j:langchain4j-core:0.35.0")

    // LangChain4j Providers
    implementation("dev.langchain4j:langchain4j-open-ai:0.35.0")
    implementation("dev.langchain4j:langchain4j-anthropic:0.35.0")
    implementation("dev.langchain4j:langchain4j-vertex-ai-gemini:0.35.0")
    implementation("dev.langchain4j:langchain4j-azure-open-ai:0.35.0")

    // Token counting
    implementation("com.knuddels:jtokkit:1.0.0")

    // Resilience4j for circuit breakers
    api("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
    api("io.github.resilience4j:resilience4j-retry:2.2.0")
    api("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")
    api("io.github.resilience4j:resilience4j-timelimiter:2.2.0")

    // Observability
    implementation(project(":platform:core:regulus-ai-observability"))

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
