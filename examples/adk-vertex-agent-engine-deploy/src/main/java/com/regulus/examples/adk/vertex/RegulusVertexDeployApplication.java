package com.regulus.examples.adk.vertex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Same shape as the quickstart, but configured for {@code adk deploy} to
 * Vertex AI Agent Engine and packaged into a container via Jib.
 *
 * <p>What an auditor sees in Agent Engine logs after deployment is documented
 * in the README.
 */
@SpringBootApplication
public class RegulusVertexDeployApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegulusVertexDeployApplication.class, args);
    }
}
