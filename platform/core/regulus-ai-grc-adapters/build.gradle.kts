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

    // Vendor adapters use the JDK's built-in java.net.http.HttpClient so the
    // module stays light and Spring-agnostic. Callers that want Resilience4j
    // retry / circuit-breaker wrap the adapter on their side.

    implementation("com.fasterxml.jackson.core:jackson-databind")
    // Required to serialise java.time.Instant in the GrcEvidenceEnvelope.
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.mockito:mockito-core:5.11.0")
}
