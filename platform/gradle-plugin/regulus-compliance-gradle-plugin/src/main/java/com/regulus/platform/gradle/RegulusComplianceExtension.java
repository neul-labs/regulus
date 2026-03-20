package com.regulus.platform.gradle;

import java.util.ArrayList;
import java.util.List;

/**
 * Gradle DSL block:
 *
 * <pre>{@code
 * regulusCompliance {
 *     profiles = listOf("eu-ai-act", "uk-gdpr", "fca-sysc")
 *     policySources = listOf("src/main/resources/policies")
 *     matrixOutput = layout.buildDirectory.file("regulus/coverage-matrix.md").get().asFile
 * }
 * }</pre>
 */
public class RegulusComplianceExtension {

    private List<String> profiles = new ArrayList<>();
    private List<String> policySources = new ArrayList<>();
    private String matrixOutput = "build/regulus/coverage-matrix.md";
    private String adkVersion = "1.2.0";

    public List<String> getProfiles() { return profiles; }
    public void setProfiles(List<String> profiles) { this.profiles = profiles; }

    public List<String> getPolicySources() { return policySources; }
    public void setPolicySources(List<String> policySources) { this.policySources = policySources; }

    public String getMatrixOutput() { return matrixOutput; }
    public void setMatrixOutput(String matrixOutput) { this.matrixOutput = matrixOutput; }

    public String getAdkVersion() { return adkVersion; }
    public void setAdkVersion(String adkVersion) { this.adkVersion = adkVersion; }
}
