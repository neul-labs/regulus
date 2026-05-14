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

        // Javadoc is best-effort while the API surfaces are stabilising.
        // Disable doclint's strict reference + missing-comment checks so a
        // single missing {@link} target doesn't fail the release build.
        // Tightened back up post-1.0.
        tasks.withType<Javadoc>().configureEach {
            (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
        }
    }

    // Publishing config — apply to platform modules (incl. BOM, excl. gradle-plugin which uses plugin-publish).
    // Wrapped in afterEvaluate because the BOM applies `java-platform` and the other modules apply
    // `java-library` in their own build.gradle.kts — neither component is registered in the root config
    // phase, only after the subproject script runs.
    if (isPlatform && !isGradlePlugin) {
        apply(plugin = "maven-publish")
        apply(plugin = "signing")

        afterEvaluate {
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
                isRequired = signingKey != null && signingPassword != null
                if (isRequired) {
                    useInMemoryPgpKeys(signingKey, signingPassword)
                    sign((extensions.getByType(PublishingExtension::class.java)).publications["maven"])
                }
                // When no signing key is configured (e.g. PR-time CI), skip signing entirely.
                // The release.yml Tier 3 step is gated on SIGNING_KEY being present so the
                // published artefacts always come out signed.
            }
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
