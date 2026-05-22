plugins {
    `java-library`
    id("org.springframework.boot") version "3.3.0" apply false
    id("io.spring.dependency-management") version "1.1.5"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.0")
    }
}

dependencies {
    api(platform(project(":platform:regulus-ai-bom")))

    // KeyProvider SPI shared with A2A signing
    api(project(":platform:core:regulus-ai-identity"))

    // Spring Boot
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.boot:spring-boot-starter-aop")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Micrometer for metrics
    api("io.micrometer:micrometer-core")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // OpenTelemetry for distributed tracing
    implementation("io.opentelemetry:opentelemetry-api")

    // Kafka for audit events (optional)
    compileOnly("org.springframework.kafka:spring-kafka")
    compileOnly("org.apache.kafka:kafka-clients:3.7.0")

    // JSON for audit event serialization
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
