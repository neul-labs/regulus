plugins {
    application
    id("com.gradleup.shadow") version "8.3.5"
}

application {
    mainClass.set("com.regulus.platform.cli.RegulusCli")
}

dependencies {
    implementation("info.picocli:picocli:4.7.6")
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")

    // No Spring, no ADK runtime dependency — the CLI is a thin scaffolding tool.
    // Keeping the fat jar small.
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

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
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    mergeServiceFiles()
    manifest {
        attributes(mapOf(
            "Main-Class" to "com.regulus.platform.cli.RegulusCli",
            "Implementation-Version" to project.version
        ))
    }
}

tasks.named("build") {
    dependsOn(tasks.shadowJar)
}
