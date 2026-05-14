plugins {
    `java-library`
    id("io.spring.dependency-management") version "1.1.5"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.0")
    }
}

dependencies {
    api(platform(project(":platform:regulus-ai-bom")))

    api(project(":platform:core:regulus-ai-governance"))
    api(project(":platform:core:regulus-ai-compliance"))

    // Reactive HTTP client for vendor adapters; ships as part of Spring WebFlux
    implementation("org.springframework:spring-webflux")
    implementation("org.springframework.boot:spring-boot-starter-reactor-netty")

    // Resilience for retry / circuit breaker on flaky vendor APIs
    implementation("io.github.resilience4j:resilience4j-reactor")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker")
    implementation("io.github.resilience4j:resilience4j-retry")

    implementation("com.fasterxml.jackson.core:jackson-databind")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("io.projectreactor:reactor-test")
}
