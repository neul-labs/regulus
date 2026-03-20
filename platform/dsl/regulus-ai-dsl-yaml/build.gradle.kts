plugins {
    `java-library`
}

dependencies {
    api(platform(project(":platform:regulus-ai-bom")))
    implementation("org.yaml:snakeyaml:2.2")
}
