package com.neullabs.regulus.compliance;

import com.neullabs.regulus.compliance.profile.DoraProfile;
import com.neullabs.regulus.compliance.profile.EhdsProfile;
import com.neullabs.regulus.compliance.profile.EuAiActProfile;
import com.neullabs.regulus.compliance.profile.FcaSyscProfile;
import com.neullabs.regulus.compliance.profile.GdprProfile;
import com.neullabs.regulus.compliance.profile.NhsDsptProfile;
import com.neullabs.regulus.compliance.profile.Nis2Profile;
import com.neullabs.regulus.compliance.profile.PraSs123Profile;
import com.neullabs.regulus.compliance.profile.PraSs221Profile;
import com.neullabs.regulus.compliance.profile.UkGdprProfile;

import java.util.List;
import java.util.Map;

/**
 * Static catalogue of the ten shipped profiles. Used by the Spring auto-config
 * to resolve {@code regulus.compliance.profiles: [...]} YAML lists and by the
 * Gradle plugin to render the coverage matrix at build time.
 */
public final class ComplianceProfiles {

    private static final Map<String, ComplianceProfile> CATALOGUE = Map.ofEntries(
            Map.entry("eu-ai-act", new EuAiActProfile()),
            Map.entry("gdpr", new GdprProfile()),
            Map.entry("uk-gdpr", new UkGdprProfile()),
            Map.entry("dora", new DoraProfile()),
            Map.entry("nis2", new Nis2Profile()),
            Map.entry("fca-sysc", new FcaSyscProfile()),
            Map.entry("pra-ss1-23", new PraSs123Profile()),
            Map.entry("pra-ss2-21", new PraSs221Profile()),
            Map.entry("nhs-dspt", new NhsDsptProfile()),
            Map.entry("ehds", new EhdsProfile())
    );

    private ComplianceProfiles() {}

    public static ComplianceProfile byId(String id) {
        ComplianceProfile p = CATALOGUE.get(id);
        if (p == null) {
            throw new IllegalArgumentException(
                    "Unknown compliance profile: " + id + ". Known: " + CATALOGUE.keySet());
        }
        return p;
    }

    public static ComplianceProfile compose(List<String> ids) {
        return new CompositeComplianceProfile(ids.stream().map(ComplianceProfiles::byId).toList());
    }

    public static Map<String, ComplianceProfile> all() {
        return CATALOGUE;
    }
}
