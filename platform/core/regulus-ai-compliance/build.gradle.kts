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

    // Pure-Java compliance descriptors — no ADK or Spring required here.
    // Plugins and services depend on this module, not the other way around.

    api(project(":platform:core:regulus-ai-identity"))

    implementation("com.fasterxml.jackson.core:jackson-databind")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.assertj:assertj-core:3.25.3")
}
