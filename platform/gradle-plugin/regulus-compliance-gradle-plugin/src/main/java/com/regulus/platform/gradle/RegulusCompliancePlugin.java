package com.regulus.platform.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Registers Regulus compliance build tasks:
 *
 * <ul>
 *   <li>{@code regulusComplianceScan} — verifies the project's ADK
 *       {@code App} declares at least one Regulus profile; fails the build
 *       otherwise.</li>
 *   <li>{@code regulusPolicyCompile} — compiles YAML / Kotlin DSL policy
 *       sources into a packaged resource.</li>
 *   <li>{@code regulusComplianceMatrix} — renders the regulation × control
 *       matrix from the active profiles into Markdown.</li>
 *   <li>{@code regulusAdkDoctor} — sanity-checks ADK + Regulus version
 *       compatibility, residency wiring, and signing config; run before
 *       {@code adk deploy}.</li>
 * </ul>
 *
 * <p>Plugin ID: {@code com.regulus.compliance}. UX mirrors Google's own
 * {@code adk-java/maven_plugin/} module so the cross-toolchain story is
 * symmetric for ADK developers.
 */
public final class RegulusCompliancePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getExtensions().create("regulusCompliance", RegulusComplianceExtension.class);

        project.getTasks().register("regulusComplianceScan", RegulusComplianceScanTask.class, task -> {
            task.setGroup("regulus");
            task.setDescription("Fails the build if no Regulus compliance profile is declared.");
        });

        project.getTasks().register("regulusPolicyCompile", RegulusPolicyCompileTask.class, task -> {
            task.setGroup("regulus");
            task.setDescription("Compiles Regulus YAML/Kotlin DSL policies into a packaged resource.");
        });

        project.getTasks().register("regulusComplianceMatrix", RegulusComplianceMatrixTask.class, task -> {
            task.setGroup("regulus");
            task.setDescription("Renders the regulation × control coverage matrix as Markdown.");
        });

        project.getTasks().register("regulusAdkDoctor", RegulusAdkDoctorTask.class, task -> {
            task.setGroup("regulus");
            task.setDescription("Sanity-checks ADK + Regulus wiring before deployment.");
        });
    }
}
