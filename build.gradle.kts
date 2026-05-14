plugins {
    id("io.spring.dependency-management") version "1.1.5" apply false
    // Publishes to Maven Central via the new Central Portal
    // (central.sonatype.com). Replaces the OSSRH-only
    // io.github.gradle-nexus.publish-plugin we used before OSSRH
    // sunset on 30 June 2025.
    id("com.vanniktech.maven.publish") version "0.36.0" apply false
    // Pinned at the root so the Vanniktech plugin can see the Kotlin
    // plugin classes when it's applied to the same Kotlin module.
    kotlin("jvm") version "2.2.0" apply false
}

group = "com.neullabs"
version = providers.gradleProperty("regulusVersion").getOrElse("0.1.0-SNAPSHOT")

allprojects {
    group = "com.neullabs"
    version = rootProject.version

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

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
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.compilerArgs.add("-parameters")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }

        // Javadoc is best-effort while the API surfaces are stabilising.
        // Tightened back up post-1.0.
        tasks.withType<Javadoc>().configureEach {
            (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
        }
    }

    // Publish every platform/* module (incl. BOM) to Maven Central, EXCEPT:
    //   - the Gradle plugin module (uses plugin-publish for the Gradle Plugin Portal)
    //   - the CLI module (distributed as a fat jar via GitHub Releases; not a library)
    // The Vanniktech plugin handles bundle assembly, GPG signing, POST to the
    // Portal API, validation polling, and auto-release.
    val isCli = path == ":platform:cli:regulus-cli"
    if (isPlatform && !isGradlePlugin && !isCli) {
        apply(plugin = "com.vanniktech.maven.publish")

        configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
            publishToMavenCentral(automaticRelease = true)
            signAllPublications()

            coordinates("com.neullabs", project.name, project.version.toString())

            pom {
                name.set("Regulus — ${project.name}")
                description.set("EU & UK compliance plane for Google ADK — module: ${project.name}")
                inceptionYear.set("2026")
                url.set("https://github.com/neul-labs/regulus")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
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
