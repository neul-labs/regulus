plugins {
    `java-library`
}

dependencies {
    api(platform(project(":platform:regulus-ai-bom")))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.25.3")
}

tasks.test {
    useJUnitPlatform()
}
