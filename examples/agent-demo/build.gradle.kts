plugins {
    java
    application
    id("io.spring.dependency-management") version "1.1.5"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.0")
    }
}

dependencies {
    // Regulus Platform Core Modules
    implementation(project(":platform:core:regulus-ai-llm"))
    implementation(project(":platform:core:regulus-ai-policy"))
    implementation(project(":platform:core:regulus-ai-privacy"))
    implementation(project(":platform:core:regulus-ai-kill-switch"))
    implementation(project(":platform:core:regulus-ai-observability"))

    // Spring Boot (required by platform modules)
    implementation("org.springframework.boot:spring-boot-starter")

    // Logging
    implementation("org.slf4j:slf4j-api")
    runtimeOnly("ch.qos.logback:logback-classic")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

application {
    mainClass.set("com.neullabs.regulus.demo.AgentDemo")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
