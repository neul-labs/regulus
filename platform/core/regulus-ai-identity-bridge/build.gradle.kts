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

    // The bridge is the single chokepoint that knows about both PolicyContext
    // shapes. Kept in its own module so regulus-ai-identity stays a true leaf.
    api(project(":platform:core:regulus-ai-identity"))
    api(project(":platform:core:regulus-ai-policy"))
    api(project(":platform:core:regulus-ai-adk-plugins"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.25.3")
}

tasks.test {
    useJUnitPlatform()
}
