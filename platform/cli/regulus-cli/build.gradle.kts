plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.5"
}

dependencies {
    implementation("info.picocli:picocli:4.7.6")
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")

    // No Spring, no ADK runtime dependency — the CLI is a thin scaffolding tool.
    // Keeping the fat jar small.
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.17.0")

    // Audit-chain verification: depend on the leaf identity module + the
    // chain SPI from observability. Spring transitives are excluded so the
    // shadow jar stays small.
    implementation(project(":platform:core:regulus-ai-identity"))
    implementation(project(":platform:core:regulus-ai-observability")) {
        exclude(group = "org.springframework")
        exclude(group = "org.springframework.boot")
        exclude(group = "io.micrometer")
        exclude(group = "io.opentelemetry")
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveBaseName.set("regulus-cli")
    archiveClassifier.set("")          // Replace the plain jar — see jar disable below.
    archiveVersion.set(project.version.toString())
    mergeServiceFiles()
    manifest {
        attributes(mapOf(
            "Main-Class" to "com.neullabs.regulus.cli.RegulusCli",
            "Implementation-Version" to project.version
        ))
    }
}

// Disable the plain jar so its output doesn't collide with shadowJar's
// (which uses the empty classifier). The fat jar is the canonical
// CLI distribution artefact.
tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named("build") {
    dependsOn(tasks.shadowJar)
}
