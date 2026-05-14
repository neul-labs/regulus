package com.neullabs.regulus.cli.cmd;

import com.neullabs.regulus.cli.scaffold.Scaffold;
import com.neullabs.regulus.cli.scaffold.ScaffoldRequest;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Scaffolds a new ADK + Regulus project in a target directory.
 *
 * <pre>{@code
 * regulus init my-agent \
 *     --profiles=eu-ai-act,uk-gdpr,fca-sysc \
 *     --frameworks=nist-ai-rmf,iso-42001 \
 *     --grc-adapter=stdout \
 *     --region=europe-west2
 * }</pre>
 */
@Command(
        name = "init",
        description = "Create a new ADK + Regulus agent project.",
        mixinStandardHelpOptions = true)
public final class InitCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Name of the agent directory to create.")
    private String name;

    @Option(names = {"-p", "--profiles"}, required = true, split = ",",
            description = "Compliance profile ids (comma-separated). Required. " +
                    "E.g. eu-ai-act,uk-gdpr,fca-sysc")
    private List<String> profiles;

    @Option(names = {"-f", "--frameworks"}, split = ",",
            description = "Governance framework ids (comma-separated). Optional. " +
                    "E.g. nist-ai-rmf,nist-ai-rmf-600-1,iso-42001")
    private List<String> frameworks = List.of();

    @Option(names = "--grc-adapter",
            description = "GRC adapter to wire (default: stdout). " +
                    "Options: stdout, kafka, servicenow-irm, onetrust-ai-gov, metricstream, webhook")
    private String grcAdapter = "stdout";

    @Option(names = "--region",
            description = "Residency region to pin the scaffold to (default: europe-west2).")
    private String region = "europe-west2";

    @Option(names = "--package",
            description = "Java package for the generated AgentApplication (default: com.example.agent).")
    private String javaPackage = "com.example.agent";

    @Option(names = "--regulus-version",
            description = "Override the Regulus version pinned in the scaffold (default: " +
                    Scaffold.DEFAULT_REGULUS_VERSION + ").")
    private String regulusVersion = Scaffold.DEFAULT_REGULUS_VERSION;

    @Option(names = "--adk-version",
            description = "Override the ADK version pinned in the scaffold (default: " +
                    Scaffold.DEFAULT_ADK_VERSION + ").")
    private String adkVersion = Scaffold.DEFAULT_ADK_VERSION;

    @Option(names = "--dir", description = "Parent directory to create the project in (default: cwd).")
    private Path parentDir = Path.of(".");

    @Option(names = "--force", description = "Overwrite the target directory if it exists.")
    private boolean force = false;

    @Override
    public Integer call() throws Exception {
        ScaffoldRequest request = new ScaffoldRequest(
                name,
                parentDir,
                profiles,
                frameworks,
                grcAdapter,
                region,
                javaPackage,
                regulusVersion,
                adkVersion,
                force);
        return new Scaffold().create(request);
    }

    // For Gradle task delegation — bypasses Picocli, callers populate fields directly.
    public static InitCommand of(String name, List<String> profiles, List<String> frameworks,
                                  String grcAdapter, String region, String javaPackage,
                                  String regulusVersion, Path parentDir) {
        InitCommand cmd = new InitCommand();
        cmd.name = name;
        cmd.profiles = profiles;
        cmd.frameworks = frameworks != null ? frameworks : List.of();
        cmd.grcAdapter = grcAdapter != null ? grcAdapter : "stdout";
        cmd.region = region != null ? region : "europe-west2";
        cmd.javaPackage = javaPackage != null ? javaPackage : "com.example.agent";
        cmd.regulusVersion = regulusVersion != null ? regulusVersion : Scaffold.DEFAULT_REGULUS_VERSION;
        cmd.parentDir = parentDir != null ? parentDir : Path.of(".");
        return cmd;
    }

    public static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
