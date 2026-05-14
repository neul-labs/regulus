package com.neullabs.regulus.gradle;

import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.api.Project;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegulusCompliancePluginTest {

    @Test
    void applyingPluginRegistersAllFiveTasks() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("com.neullabs.compliance");

        assertThat(project.getTasks().findByName("regulusComplianceScan")).isNotNull();
        assertThat(project.getTasks().findByName("regulusPolicyCompile")).isNotNull();
        assertThat(project.getTasks().findByName("regulusComplianceMatrix")).isNotNull();
        assertThat(project.getTasks().findByName("regulusAdkDoctor")).isNotNull();
        assertThat(project.getTasks().findByName("initRegulusAgent")).isNotNull();
    }

    @Test
    void extensionDefaultsAreSane() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("com.neullabs.compliance");

        RegulusComplianceExtension ext = project.getExtensions()
                .getByType(RegulusComplianceExtension.class);

        assertThat(ext.getProfiles()).isEmpty();
        assertThat(ext.getAdkVersion()).isNotBlank();
        assertThat(ext.getMatrixOutput()).contains("coverage-matrix.md");
    }
}
