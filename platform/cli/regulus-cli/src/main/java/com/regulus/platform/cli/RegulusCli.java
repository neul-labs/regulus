package com.regulus.platform.cli;

import com.regulus.platform.cli.cmd.DoctorCommand;
import com.regulus.platform.cli.cmd.InitCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Regulus CLI — scaffolds compliant ADK agents, sanity-checks projects.
 *
 * <p>Install:
 * <pre>{@code
 * curl -fsSL https://regulus.neullabs.com/install.sh | sh
 * }</pre>
 *
 * <p>Use:
 * <pre>{@code
 * regulus init my-agent --profiles=eu-ai-act,uk-gdpr,fca-sysc \
 *                       --frameworks=nist-ai-rmf,iso-42001
 * regulus doctor
 * regulus version
 * }</pre>
 */
@Command(
        name = "regulus",
        mixinStandardHelpOptions = true,
        version = "Regulus CLI " + RegulusCli.VERSION,
        description = "Scaffold compliant ADK agents and check project health.",
        subcommands = {
                InitCommand.class,
                DoctorCommand.class
        })
public final class RegulusCli implements Callable<Integer> {

    public static final String VERSION = "0.1.0";

    @Override
    public Integer call() {
        // No subcommand given — print usage.
        new CommandLine(this).usage(System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new RegulusCli()).execute(args);
        System.exit(exitCode);
    }
}
