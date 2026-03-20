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

    // Spring Boot
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-aop")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-webflux")
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Jackson for JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // OAuth2 / Security (optional)
    compileOnly("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    compileOnly("org.springframework.security:spring-security-oauth2-jose")

    // LangChain4j
    api("dev.langchain4j:langchain4j")
    api("dev.langchain4j:langchain4j-spring-boot-starter")

    // Core modules
    api(project(":platform:core:regulus-ai-policy"))
    api(project(":platform:core:regulus-ai-privacy"))
    api(project(":platform:core:regulus-ai-observability"))
    api(project(":platform:core:regulus-ai-kill-switch"))

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
