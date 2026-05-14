package com.neullabs.regulus.gradle;

import com.neullabs.regulus.cli.scaffold.Scaffold;
import com.neullabs.regulus.cli.scaffold.ScaffoldRequest;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Gradle wrapper around the CLI's {@link Scaffold}.
 *
 * <pre>{@code
 * ./gradlew initRegulusAgent \
 *     -PagentName=my-agent \
 *     -Pprofiles=eu-ai-act,uk-gdpr,fca-sysc \
 *     -Pframeworks=nist-ai-rmf,iso-42001
 * }</pre>
 */
public abstract class InitRegulusAgentTask extends DefaultTask {

    @TaskAction
    public void scaffold() throws Exception {
        String name = projectProperty("agentName", null);
        if (name == null || name.isBlank()) {
            throw new GradleException("Missing required project property: -PagentName=<name>");
        }
        String profilesCsv = projectProperty("profiles", null);
        if (profilesCsv == null || profilesCsv.isBlank()) {
            throw new GradleException("Missing required project property: -Pprofiles=eu-ai-act,uk-gdpr,...");
        }
        List<String> profiles = splitCsv(profilesCsv);
        List<String> frameworks = splitCsv(projectProperty("frameworks", ""));
        String grcAdapter = projectProperty("grcAdapter", "stdout");
        String region = projectProperty("region", "europe-west2");
        String javaPackage = projectProperty("javaPackage", "com.example.agent");
        String regulusVersion = projectProperty("regulusVersion", Scaffold.DEFAULT_REGULUS_VERSION);
        String adkVersion = projectProperty("adkVersion", Scaffold.DEFAULT_ADK_VERSION);
        Path parentDir = Path.of(projectProperty("dir", getProject().getProjectDir().toString()));

        ScaffoldRequest req = new ScaffoldRequest(
                name, parentDir, profiles, frameworks, grcAdapter, region,
                javaPackage, regulusVersion, adkVersion, /* force */ false);
        int rc = new Scaffold().create(req);
        if (rc != 0) {
            throw new GradleException("regulus init failed with code " + rc);
        }
    }

    private String projectProperty(String key, String fallback) {
        Object value = getProject().findProperty(key);
        return value != null ? value.toString() : fallback;
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
