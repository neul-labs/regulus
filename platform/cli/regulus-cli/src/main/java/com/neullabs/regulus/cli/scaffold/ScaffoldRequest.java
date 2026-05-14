package com.neullabs.regulus.cli.scaffold;

import java.nio.file.Path;
import java.util.List;

/**
 * Input to {@link Scaffold#create(ScaffoldRequest)}. Immutable.
 */
public record ScaffoldRequest(
        String name,
        Path parentDir,
        List<String> profiles,
        List<String> frameworks,
        String grcAdapter,
        String region,
        String javaPackage,
        String regulusVersion,
        String adkVersion,
        boolean force) {
}
