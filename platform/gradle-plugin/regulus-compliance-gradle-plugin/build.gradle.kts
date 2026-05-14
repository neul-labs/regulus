plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.1"
}

dependencies {
    implementation(gradleApi())
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("org.yaml:snakeyaml:2.2")

    // Re-use the CLI's Scaffold logic so `gradle initRegulusAgent` produces
    // identical output to `regulus init`.
    implementation(project(":platform:cli:regulus-cli"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    website = "https://github.com/neul-labs/regulus"
    vcsUrl = "https://github.com/neul-labs/regulus.git"

    plugins {
        create("regulusCompliance") {
            id = "com.neullabs.compliance"
            implementationClass = "com.neullabs.regulus.gradle.RegulusCompliancePlugin"
            displayName = "Regulus Compliance Gradle Plugin"
            description = "Build-time checks and codegen for ADK agents using the Regulus EU+UK compliance plane."
            tags = listOf("adk", "ai", "compliance", "gdpr", "eu-ai-act", "fca", "regtech")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
