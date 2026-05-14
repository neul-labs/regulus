# BOM coordinates

The Regulus BOM pins versions for every Regulus module plus the ADK
versions Regulus is tested against.

## Use the BOM

```kotlin
dependencies {
    implementation(platform("com.neullabs:regulus-ai-bom:0.1.0"))

    // ADK
    implementation("com.google.adk:google-adk")
    implementation("com.google.adk:google-adk-dev")  // optional dev UI

    // Regulus ADK extension surface
    implementation("com.neullabs:regulus-ai-adk-plugins")
    implementation("com.neullabs:regulus-ai-adk-services")
    implementation("com.neullabs:regulus-ai-adk-a2a")
    implementation("com.neullabs:regulus-ai-compliance")

    // Spring Boot starter — optional
    implementation("com.neullabs:regulus-ai-adk-spring-boot-starter")
}
```

## What's pinned

- `com.google.adk:google-adk:1.2.0`
- `com.google.adk:google-adk-dev:1.2.0`
- All `com.neullabs:*` modules at the current Regulus version
- Spring Boot 3.3
- Resilience4j 2.2
- Spring Kafka 3.2
- OpenTelemetry 1.38
- Jackson 2.17

## Versioning policy

- `0.x` — public preview. Breaking changes allowed at minor bumps.
- `1.x` — stable. SemVer.
- ADK pin: tested compatibility, but Regulus runs nightly CI against
  `1.+` to surface drift early.

## Snapshots

`-SNAPSHOT` versions are published to Sonatype OSSRH snapshots:

```kotlin
repositories {
    mavenCentral()
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}
```
