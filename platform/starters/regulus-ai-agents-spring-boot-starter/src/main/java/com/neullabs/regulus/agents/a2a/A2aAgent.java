package com.neullabs.regulus.agents.a2a;

import java.util.List;
import java.util.Map;

/**
 * Represents an A2A (Agent-to-Agent) agent that can communicate with other agents.
 */
public record A2aAgent(
    String id,
    String name,
    String description,
    String url,
    String provider,
    String version,
    List<A2aSkill> skills,
    Map<String, Object> authentication,
    Map<String, Object> metadata
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String description;
        private String url;
        private String provider;
        private String version = "1.0.0";
        private List<A2aSkill> skills = List.of();
        private Map<String, Object> authentication = Map.of();
        private Map<String, Object> metadata = Map.of();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder skills(List<A2aSkill> skills) {
            this.skills = skills;
            return this;
        }

        public Builder authentication(Map<String, Object> authentication) {
            this.authentication = authentication;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public A2aAgent build() {
            return new A2aAgent(id, name, description, url, provider, version,
                skills, authentication, metadata);
        }
    }
}
