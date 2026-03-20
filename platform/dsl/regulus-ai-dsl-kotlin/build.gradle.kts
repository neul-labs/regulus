plugins {
    kotlin("jvm") version "1.9.22"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    api(platform(project(":platform:regulus-ai-bom")))
    implementation("org.springframework.boot:spring-boot-starter:3.3.0")
}
