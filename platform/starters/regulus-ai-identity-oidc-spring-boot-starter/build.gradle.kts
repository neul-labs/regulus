plugins {
    `java-library`
    id("org.springframework.boot") version "3.3.0" apply false
    id("io.spring.dependency-management") version "1.1.5"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.0")
    }
}

dependencies {
    api(platform(project(":platform:regulus-ai-bom")))

    api(project(":platform:core:regulus-ai-identity"))

    // Spring Boot autoconfig surface — required for the autoconfig classes
    api("org.springframework.boot:spring-boot-starter")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Spring Security OAuth2 resource server is the inbound JWT source.
    // Kept compileOnly so non-OIDC tenants don't pull the Spring Security
    // tree transitively. The @ConditionalOnClass guards make the autoconfig
    // dormant when these classes aren't on the runtime classpath.
    compileOnly("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    compileOnly("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Test the full inbound path: JWT → Identity → PolicyContext via the bridge.
    testImplementation(project(":platform:core:regulus-ai-identity-bridge"))
}
