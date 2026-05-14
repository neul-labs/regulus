package com.neullabs.regulus.cli.cmd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DoctorCommandTest {

    @Test
    void emptyDirFailsWithMissingBuildFiles(@TempDir Path tempDir) {
        int exit = new CommandLine(new DoctorCommand())
                .execute("--dir=" + tempDir);
        assertThat(exit).isEqualTo(1);
    }

    @Test
    void wellFormedDirPasses(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("build.gradle.kts"),
                "dependencies {\n" +
                "    implementation(\"com.google.adk:google-adk:1.2.0\")\n" +
                "    implementation(\"com.neullabs:regulus-ai-adk-plugins\")\n" +
                "}\n");
        Path appYaml = tempDir.resolve("src/main/resources/application.yaml");
        Files.createDirectories(appYaml.getParent());
        Files.writeString(appYaml,
                "regulus:\n" +
                "  compliance:\n" +
                "    profiles: [eu-ai-act]\n");

        int exit = new CommandLine(new DoctorCommand())
                .execute("--dir=" + tempDir);
        assertThat(exit).isZero();
    }
}
