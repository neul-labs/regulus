plugins {
    `java-library`
}

description = "Regulus AI Persistence - Database layer for audit, kill switch, and compliance data"

dependencies {
    // Spring Boot Data JPA
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    // PostgreSQL
    runtimeOnly("org.postgresql:postgresql:42.7.3")

    // Flyway migrations
    implementation("org.flywaydb:flyway-core:10.10.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.10.0")

    // H2 for testing
    testRuntimeOnly("com.h2database:h2:2.2.224")

    // Observability module for audit events
    implementation(project(":platform:core:regulus-ai-observability"))

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.testcontainers:postgresql:1.19.7")
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")
}
