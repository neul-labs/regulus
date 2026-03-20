package com.regulus.quickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Regulus AI Platform Quick Start Application.
 *
 * Run with: ./gradlew :examples:quickstart:bootRun
 *
 * Endpoints:
 *   - POST /mcp           - MCP server (tool discovery & invocation)
 *   - GET  /api/validate  - ISO 20022 validation
 *   - GET  /api/risk      - Risk scoring
 *   - GET  /api/health    - Health check
 */
@SpringBootApplication
public class QuickstartApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickstartApplication.class, args);
    }
}
