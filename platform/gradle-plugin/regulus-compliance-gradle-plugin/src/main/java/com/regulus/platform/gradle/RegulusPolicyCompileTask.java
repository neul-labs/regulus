package com.regulus.platform.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public abstract class RegulusPolicyCompileTask extends DefaultTask {

    @TaskAction
    public void compile() {
        RegulusComplianceExtension ext = getProject().getExtensions()
                .getByType(RegulusComplianceExtension.class);
        getLogger().lifecycle("regulusPolicyCompile: compiling policies from {}", ext.getPolicySources());
        // The DSL parser modules (regulus-ai-dsl-yaml, regulus-ai-dsl-kotlin) do the actual
        // work; this task is the build-time front door for them.
    }
}
