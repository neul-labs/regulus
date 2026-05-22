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

    // Google Agent Development Kit — the extension surface
    api("com.google.adk:google-adk")

    // Sibling Regulus modules whose mechanisms these plugins expose
    api(project(":platform:core:regulus-ai-identity"))
    api(project(":platform:core:regulus-ai-policy"))
    api(project(":platform:core:regulus-ai-privacy"))
    api(project(":platform:core:regulus-ai-kill-switch"))
    api(project(":platform:core:regulus-ai-observability"))
    api(project(":platform:core:regulus-ai-compliance"))
    api(project(":platform:core:regulus-ai-governance"))
    api(project(":platform:core:regulus-ai-grc-adapters"))

    // Lightweight utilities — no Spring dependency in this module
    implementation("com.fasterxml.jackson.core:jackson-databind")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.mockito:mockito-core:5.11.0")
}
