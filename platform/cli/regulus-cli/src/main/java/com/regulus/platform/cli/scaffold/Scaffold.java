package com.regulus.platform.cli.scaffold;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Produces a working ADK + Regulus project from a {@link ScaffoldRequest}.
 *
 * <p>Templates live under {@code resources/templates/*.tmpl} and use a
 * deliberately minimal {@code {{name}}} substitution scheme — no Mustache
 * dependency, no Velocity, just {@link String#replace}. The full template
 * keys are documented on {@link #render}.
 */
public final class Scaffold {

    public static final String DEFAULT_REGULUS_VERSION = "0.1.0";
    public static final String DEFAULT_ADK_VERSION     = "1.2.0";

    public int create(ScaffoldRequest req) throws IOException {
        Path target = req.parentDir().toAbsolutePath().resolve(req.name()).normalize();
        if (Files.exists(target) && !req.force()) {
            System.err.println("regulus init: directory exists: " + target);
            System.err.println("  pass --force to overwrite, or pick a different name.");
            return 2;
        }
        Files.createDirectories(target);

        Map<String, String> vars = vars(req);

        write(target, "build.gradle.kts",                                vars, "build.gradle.kts.tmpl");
        write(target, "settings.gradle.kts",                             vars, "settings.gradle.kts.tmpl");
        write(target, "gradle.properties",                               vars, "gradle.properties.tmpl");
        write(target, ".gitignore",                                      vars, "gitignore.tmpl");
        write(target, "README.md",                                       vars, "README.md.tmpl");
        write(target, "src/main/resources/application.yaml",             vars, "application.yaml.tmpl");
        write(target, "src/main/resources/logback.xml",                  vars, "logback.xml.tmpl");

        String pkgPath = req.javaPackage().replace('.', '/');
        write(target, "src/main/java/" + pkgPath + "/AgentApplication.java",
                vars, "AgentApplication.java.tmpl");
        write(target, "src/main/java/" + pkgPath + "/ChatController.java",
                vars, "ChatController.java.tmpl");

        // Gradle wrapper — minimal viable wrapper that defers to the user's
        // gradle. We include the script wrappers; the user runs `gradle wrapper`
        // once to materialise the jar + properties.
        write(target, "gradlew",     vars, "gradlew.tmpl");
        write(target, "gradlew.bat", vars, "gradlew.bat.tmpl");
        // Make gradlew executable on POSIX
        try { target.resolve("gradlew").toFile().setExecutable(true); } catch (Exception ignored) {}

        printSummary(target, req);
        return 0;
    }

    private static Map<String, String> vars(ScaffoldRequest req) {
        Map<String, String> v = new HashMap<>();
        v.put("name", req.name());
        v.put("package", req.javaPackage());
        v.put("region", req.region());
        v.put("regulusVersion", req.regulusVersion());
        v.put("adkVersion", req.adkVersion());
        v.put("grcAdapter", req.grcAdapter());
        v.put("profilesYaml", yamlList(req.profiles()));
        v.put("frameworksYaml", yamlList(req.frameworks()));
        v.put("profilesCsv", String.join(",", req.profiles()));
        v.put("frameworksCsv", String.join(",", req.frameworks()));
        v.put("grcYamlBlock", grcYamlBlock(req.grcAdapter()));
        return v;
    }

    private static String yamlList(java.util.List<String> items) {
        if (items == null || items.isEmpty()) return "[]";
        return "[" + String.join(", ", items) + "]";
    }

    /** Renders the grc.* YAML block based on the chosen adapter. */
    private static String grcYamlBlock(String adapter) {
        return switch (adapter) {
            case "stdout"        -> "    stdout: true";
            case "kafka"         -> """
                    stdout: false
                    # See Operations → Audit retention runbook for Kafka wiring.""";
            case "servicenow-irm" -> """
                    servicenow-irm:
                      enabled: true
                      base-uri: ${SERVICENOW_URI}
                      bearer-token: ${SERVICENOW_TOKEN}""";
            case "onetrust-ai-gov" -> """
                    onetrust-ai-gov:
                      enabled: true
                      base-uri: ${ONETRUST_URI}
                      api-key: ${ONETRUST_API_KEY}""";
            case "metricstream"   -> """
                    metricstream:
                      enabled: true
                      base-uri: ${METRICSTREAM_URI}
                      auth-token: ${METRICSTREAM_TOKEN}""";
            case "webhook"        -> """
                    webhook:
                      enabled: true
                      endpoint: ${REGULUS_WEBHOOK_ENDPOINT}
                      hmac-key-hex: ${REGULUS_WEBHOOK_HMAC_KEY}""";
            default               -> "    stdout: true";
        };
    }

    private static void write(Path target, String relPath, Map<String, String> vars, String template) throws IOException {
        Path out = target.resolve(relPath);
        Files.createDirectories(out.getParent() != null ? out.getParent() : target);
        String body = render(loadTemplate(template), vars);
        Files.writeString(out, body, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Substitutes {@code {{key}}} occurrences from the vars map.
     * Templates use this exact form — no spaces inside the braces.
     */
    static String render(String template, Map<String, String> vars) {
        String body = template;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            body = body.replace("{{" + e.getKey() + "}}", e.getValue() != null ? e.getValue() : "");
        }
        return body;
    }

    private static String loadTemplate(String resource) throws IOException {
        try (InputStream in = Scaffold.class.getResourceAsStream("/templates/" + resource)) {
            if (in == null) throw new IOException("Template not found on classpath: " + resource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void printSummary(Path target, ScaffoldRequest req) {
        System.out.println();
        System.out.println("✓ created " + target);
        System.out.println("  profiles:   " + String.join(", ", req.profiles()));
        if (!req.frameworks().isEmpty()) {
            System.out.println("  frameworks: " + String.join(", ", req.frameworks()));
        }
        System.out.println("  region:     " + req.region());
        System.out.println("  grc:        " + req.grcAdapter());
        System.out.println();
        System.out.println("Next steps:");
        System.out.println("  cd " + target.getFileName() + " && gradle wrapper && ./gradlew bootRun");
        System.out.println();
        System.out.println("Docs:   https://docs.neullabs.com");
        System.out.println("Issues: https://github.com/neul-labs/regulus/issues");
    }
}
