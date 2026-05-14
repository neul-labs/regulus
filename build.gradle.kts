plugins {
    id("io.spring.dependency-management") version "1.1.5" apply false
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

group = "com.regulus.platform"
version = providers.gradleProperty("regulusVersion").getOrElse("0.1.0-SNAPSHOT")

allprojects {
    group = "com.regulus.platform"
    version = rootProject.version

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

// Apply java-library to publishable subprojects (everything in platform/, not BOM, not examples)
subprojects {
    val isBom = name == "regulus-ai-bom"
    val isExample = path.startsWith(":examples")
    val isPlatform = path.startsWith(":platform")
    val isGradlePlugin = path.startsWith(":platform:gradle-plugin")

    if (!isBom && !isExample) {
        apply(plugin = "java-library")

        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
            withSourcesJar()
            withJavadocJar()
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.compilerArgs.add("-parameters")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }

    // Publishing config — apply to platform modules (incl. BOM, excl. gradle-plugin which uses plugin-publish)
    if (isPlatform && !isGradlePlugin) {
        apply(plugin = "maven-publish")
        apply(plugin = "signing")

        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    if (isBom) {
                        from(components["javaPlatform"])
                    } else {
                        from(components["java"])
                    }
                    pom {
                        name.set("Regulus — ${project.name}")
                        description.set("EU & UK compliance plane for Google ADK — module: ${project.name}")
                        url.set("https://github.com/neul-labs/regulus")
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://opensource.org/licenses/MIT")
                            }
                        }
                        developers {
                            developer {
                                id.set("neul-labs")
                                name.set("Neul Labs")
                                email.set("opensource@neullabs.com")
                            }
                        }
                        scm {
                            connection.set("scm:git:git@github.com:neul-labs/regulus.git")
                            developerConnection.set("scm:git:git@github.com:neul-labs/regulus.git")
                            url.set("https://github.com/neul-labs/regulus")
                        }
                    }
                }
            }
        }

        configure<SigningExtension> {
            val signingKey: String? by project
            val signingPassword: String? by project
            if (signingKey != null && signingPassword != null) {
                useInMemoryPgpKeys(signingKey, signingPassword)
            } else {
                useGpgCmd()
            }
            sign((extensions.getByType(PublishingExtension::class.java)).publications["maven"])
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            // Credentials supplied via env: ORG_GRADLE_PROJECT_sonatypeUsername / sonatypePassword
        }
    }
}
