plugins {
    kotlin("jvm") version "2.2.0"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(platform(project(":platform:regulus-ai-bom")))
    implementation("org.springframework.boot:spring-boot-starter:3.3.0")
}
