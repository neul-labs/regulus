package com.neullabs.regulus.cli.cmd.audit;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * {@code regulus audit ...} subcommand group. Only {@code verify} is
 * implemented today; offline integrity checking is the first auditor-facing
 * tool to ship from the CLI.
 */
@Command(
        name = "audit",
        description = "Tools for inspecting Regulus audit chains.",
        mixinStandardHelpOptions = true,
        subcommands = {
                AuditVerifyCommand.class
        })
public final class AuditCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        new CommandLine(this).usage(System.out);
        return 0;
    }
}
