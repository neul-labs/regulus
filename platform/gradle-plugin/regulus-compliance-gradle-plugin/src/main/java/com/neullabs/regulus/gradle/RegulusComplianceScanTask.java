package com.neullabs.regulus.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

public abstract class RegulusComplianceScanTask extends DefaultTask {

    @TaskAction
    public void scan() {
        RegulusComplianceExtension ext = getProject().getExtensions()
                .getByType(RegulusComplianceExtension.class);
        if (ext.getProfiles().isEmpty()) {
            throw new GradleException(
                    "regulusCompliance.profiles is empty. Declare at least one Regulus profile, "
                    + "e.g. \"eu-ai-act\", \"uk-gdpr\", \"fca-sysc\".");
        }
        getLogger().lifecycle("regulusComplianceScan: {} profile(s) declared: {}",
                ext.getProfiles().size(), ext.getProfiles());
    }
}
