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

    // Spring Boot — only required for the optional auto-config layer
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-validation")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Google ADK
    api("com.google.adk:google-adk")
    // Optional dev UI bridge — surfaced when regulus.adk.dev-server.enabled=true
    compileOnly("com.google.adk:google-adk-dev")

    // Regulus ADK extension surface
    api(project(":platform:core:regulus-ai-adk-plugins"))
    api(project(":platform:core:regulus-ai-adk-services"))
    api(project(":platform:core:regulus-ai-adk-a2a"))
    api(project(":platform:core:regulus-ai-compliance"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
