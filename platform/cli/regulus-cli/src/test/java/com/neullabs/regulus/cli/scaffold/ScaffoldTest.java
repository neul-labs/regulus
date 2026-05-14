package com.neullabs.regulus.cli.scaffold;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScaffoldTest {

    @Test
    void producesExpectedFileTree(@TempDir Path tempDir) throws IOException {
        ScaffoldRequest req = new ScaffoldRequest(
                "my-agent",
                tempDir,
                List.of("eu-ai-act", "uk-gdpr"),
                List.of("nist-ai-rmf"),
                "stdout",
                "europe-west2",
                "com.example.agent",
                "0.1.0",
                "1.2.0",
                /* force */ false);

        int rc = new Scaffold().create(req);
        assertThat(rc).isZero();

        Path root = tempDir.resolve("my-agent");
        assertThat(root.resolve("build.gradle.kts")).exists();
        assertThat(root.resolve("settings.gradle.kts")).exists();
        assertThat(root.resolve("gradle.properties")).exists();
        assertThat(root.resolve(".gitignore")).exists();
        assertThat(root.resolve("README.md")).exists();
        assertThat(root.resolve("gradlew")).exists();
        assertThat(root.resolve("gradlew.bat")).exists();
        assertThat(root.resolve("src/main/java/com/example/agent/AgentApplication.java")).exists();
        assertThat(root.resolve("src/main/java/com/example/agent/ChatController.java")).exists();
        assertThat(root.resolve("src/main/resources/application.yaml")).exists();
        assertThat(root.resolve("src/main/resources/logback.xml")).exists();
    }

    @Test
    void buildGradleReferencesResolvedCoords(@TempDir Path tempDir) throws IOException {
        new Scaffold().create(new ScaffoldRequest(
                "my-agent", tempDir,
                List.of("eu-ai-act"), List.of(), "stdout",
                "europe-west2", "com.example.agent",
                "0.1.0", "1.2.0", false));

        String build = Files.readString(tempDir.resolve("my-agent/build.gradle.kts"));
        assertThat(build).contains("com.neullabs:regulus-ai-bom:0.1.0");
        assertThat(build).contains("com.google.adk:google-adk:1.2.0");
        assertThat(build).contains("com.neullabs:regulus-ai-adk-spring-boot-starter");
    }

    @Test
    void applicationYamlContainsProfilesAndFrameworks(@TempDir Path tempDir) throws IOException {
        new Scaffold().create(new ScaffoldRequest(
                "my-agent", tempDir,
                List.of("eu-ai-act", "uk-gdpr"),
                List.of("nist-ai-rmf", "iso-42001"),
                "stdout", "europe-west2",
                "com.example.agent",
                "0.1.0", "1.2.0", false));

        String yaml = Files.readString(tempDir.resolve("my-agent/src/main/resources/application.yaml"));
        assertThat(yaml).contains("[eu-ai-act, uk-gdpr]");
        assertThat(yaml).contains("[nist-ai-rmf, iso-42001]");
        assertThat(yaml).contains("europe-west2");
    }

    @Test
    void agentApplicationHasCorrectPackage(@TempDir Path tempDir) throws IOException {
        new Scaffold().create(new ScaffoldRequest(
                "my-agent", tempDir,
                List.of("eu-ai-act"), List.of(),
                "stdout", "europe-west2",
                "com.tenant.bank.agent",
                "0.1.0", "1.2.0", false));

        Path appFile = tempDir.resolve("my-agent/src/main/java/com/tenant/bank/agent/AgentApplication.java");
        assertThat(appFile).exists();
        String content = Files.readString(appFile);
        assertThat(content).startsWith("package com.tenant.bank.agent;");
    }

    @Test
    void existingDirectoryWithoutForceReturnsTwo(@TempDir Path tempDir) throws IOException {
        new Scaffold().create(new ScaffoldRequest(
                "my-agent", tempDir,
                List.of("eu-ai-act"), List.of(),
                "stdout", "europe-west2", "com.example.agent",
                "0.1.0", "1.2.0", false));

        int rc = new Scaffold().create(new ScaffoldRequest(
                "my-agent", tempDir,
                List.of("eu-ai-act"), List.of(),
                "stdout", "europe-west2", "com.example.agent",
                "0.1.0", "1.2.0", /* force */ false));

        assertThat(rc).isEqualTo(2);
    }

    @Test
    void grcAdapterStdoutEnabledByDefault(@TempDir Path tempDir) throws IOException {
        new Scaffold().create(new ScaffoldRequest(
                "my-agent", tempDir,
                List.of("eu-ai-act"), List.of(),
                "stdout", "europe-west2", "com.example.agent",
                "0.1.0", "1.2.0", false));
        String yaml = Files.readString(tempDir.resolve("my-agent/src/main/resources/application.yaml"));
        assertThat(yaml).contains("stdout: true");
    }

    @Test
    void grcAdapterServiceNowYieldsBlock(@TempDir Path tempDir) throws IOException {
        new Scaffold().create(new ScaffoldRequest(
                "my-agent", tempDir,
                List.of("eu-ai-act"), List.of(),
                "servicenow-irm",
                "europe-west2", "com.example.agent",
                "0.1.0", "1.2.0", false));
        String yaml = Files.readString(tempDir.resolve("my-agent/src/main/resources/application.yaml"));
        assertThat(yaml).contains("servicenow-irm:");
        assertThat(yaml).contains("enabled: true");
    }
}
