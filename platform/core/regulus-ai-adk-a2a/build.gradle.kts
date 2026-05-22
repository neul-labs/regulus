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

    api("com.google.adk:google-adk")

    api(project(":platform:core:regulus-ai-identity"))
    api(project(":platform:core:regulus-ai-adk-plugins"))
    api(project(":platform:core:regulus-ai-observability"))

    implementation("com.fasterxml.jackson.core:jackson-databind")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.mockito:mockito-core:5.11.0")
}
