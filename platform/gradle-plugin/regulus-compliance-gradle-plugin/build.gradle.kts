plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.1"
}

dependencies {
    implementation(gradleApi())
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("org.yaml:snakeyaml:2.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    website = "https://github.com/Skelf-Research/regulus"
    vcsUrl = "https://github.com/Skelf-Research/regulus.git"

    plugins {
        create("regulusCompliance") {
            id = "com.regulus.compliance"
            implementationClass = "com.regulus.platform.gradle.RegulusCompliancePlugin"
            displayName = "Regulus Compliance Gradle Plugin"
            description = "Build-time checks and codegen for ADK agents using the Regulus EU+UK compliance plane."
            tags = listOf("adk", "ai", "compliance", "gdpr", "eu-ai-act", "fca", "regtech")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
