package com.regulus.examples.adk.quickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ADK + Regulus quickstart: a Spring Boot application with the full Regulus
 * plugin suite wired around a Gemini-backed ADK agent.
 *
 * <p>See {@code application.yaml} for the declarative configuration that wires
 * the EU AI Act + UK GDPR + FCA SYSC profiles.
 */
@SpringBootApplication
public class RegulusAdkQuickstartApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegulusAdkQuickstartApplication.class, args);
    }
}
