package com.neullabs.regulus.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public abstract class RegulusAdkDoctorTask extends DefaultTask {

    @TaskAction
    public void check() {
        RegulusComplianceExtension ext = getProject().getExtensions()
                .getByType(RegulusComplianceExtension.class);

        getLogger().lifecycle("regulusAdkDoctor: ADK target version {}", ext.getAdkVersion());
        getLogger().lifecycle("regulusAdkDoctor: {} profile(s) configured", ext.getProfiles().size());

        boolean adkOnClasspath = getProject().getConfigurations()
                .stream()
                .flatMap(c -> c.getAllDependencies().stream())
                .anyMatch(d -> "com.google.adk".equals(d.getGroup()) && "google-adk".equals(d.getName()));

        if (!adkOnClasspath) {
            getLogger().warn("regulusAdkDoctor: com.google.adk:google-adk is NOT on the classpath. "
                    + "Add `implementation(\"com.google.adk:google-adk:" + ext.getAdkVersion() + "\")`.");
        }

        if (ext.getProfiles().isEmpty()) {
            getLogger().warn("regulusAdkDoctor: no Regulus profiles declared. "
                    + "Add e.g. `regulusCompliance { profiles = listOf(\"eu-ai-act\", \"uk-gdpr\") }`.");
        }
    }
}
