package com.regulus.platform.cli.cmd;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Sanity-checks a Regulus + ADK project. Useful for users not yet running
 * via Gradle — they can run {@code regulus doctor} against any directory
 * to get a quick diagnosis.
 *
 * <p>The Gradle equivalent is {@code ./gradlew regulusAdkDoctor}; this
 * command is the lighter standalone variant.
 */
@Command(
        name = "doctor",
        description = "Check a project for common Regulus / ADK configuration issues.",
        mixinStandardHelpOptions = true)
public final class DoctorCommand implements Callable<Integer> {

    @Option(names = "--dir", description = "Project directory to inspect (default: cwd).")
    private Path projectDir = Path.of(".");

    @Override
    public Integer call() throws Exception {
        boolean ok = true;

        Path buildFile = projectDir.resolve("build.gradle.kts");
        Path appYaml = projectDir.resolve("src/main/resources/application.yaml");

        ok &= check(buildFile, "build.gradle.kts");
        ok &= check(appYaml, "application.yaml");

        if (Files.exists(buildFile)) {
            String build = Files.readString(buildFile);
            ok &= contains(build, "com.google.adk:google-adk", "ADK dependency declared");
            ok &= contains(build, "com.regulus.platform", "Regulus BOM / module declared");
        }

        if (Files.exists(appYaml)) {
            String yaml = Files.readString(appYaml);
            ok &= contains(yaml, "regulus:", "Regulus YAML root present");
            ok &= contains(yaml, "compliance:", "compliance section present");
            ok &= contains(yaml, "profiles:", "profiles declared");
        }

        if (ok) {
            System.out.println("regulus doctor: OK");
            return 0;
        }
        System.out.println();
        System.out.println("Some checks failed. See above. For details: https://regulus.neullabs.com/reference/cli/");
        return 1;
    }

    private boolean check(Path file, String label) {
        if (Files.exists(file)) {
            System.out.println("✓ " + label + " present");
            return true;
        }
        System.out.println("✗ " + label + " missing");
        return false;
    }

    private boolean contains(String body, String needle, String label) {
        if (body.contains(needle)) {
            System.out.println("✓ " + label);
            return true;
        }
        System.out.println("✗ " + label + " (missing '" + needle + "')");
        return false;
    }
}
