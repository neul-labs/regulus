package com.neullabs.regulus.persistence.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuration for Regulus persistence layer.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "regulus.ai.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PersistenceProperties.class)
@EnableJpaRepositories(basePackages = "com.neullabs.regulus.persistence.repository")
@EntityScan(basePackages = "com.neullabs.regulus.persistence.entity")
public class PersistenceAutoConfiguration {

}
