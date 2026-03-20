plugins {
    java
    id("org.springframework.boot") version "3.3.0"
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

dependencies {
    implementation("com.google.adk:google-adk:1.2.0")

    implementation(project(":platform:starters:regulus-ai-adk-spring-boot-starter"))
    implementation(project(":platform:core:regulus-ai-adk-a2a"))
    implementation(project(":platform:core:regulus-ai-adk-plugins"))
    implementation(project(":platform:core:regulus-ai-compliance"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
