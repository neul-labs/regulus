plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    id("com.google.cloud.tools.jib") version "3.4.3"
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
    val adkVersion: String = providers.gradleProperty("adkVersion").getOrElse("1.2.0")
    implementation("com.google.adk:google-adk:$adkVersion")
    implementation("com.google.adk:google-adk-dev:$adkVersion")

    implementation(project(":platform:starters:regulus-ai-adk-spring-boot-starter"))
    implementation(project(":platform:core:regulus-ai-adk-plugins"))
    implementation(project(":platform:core:regulus-ai-adk-services"))
    implementation(project(":platform:core:regulus-ai-compliance"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

jib {
    to {
        image = "ghcr.io/neul-labs/regulus-adk-demo"
        tags = setOf("latest", project.version.toString())
    }
    container {
        jvmFlags = listOf(
            "-XX:+UseContainerSupport",
            "-XX:MaxRAMPercentage=75.0"
        )
        ports = listOf("8080")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
