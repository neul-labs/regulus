package com.neullabs.regulus.examples.adk.a2a;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Two ADK agents talking over A2A, with Regulus enforcing policy, privacy,
 * audit, and kill-switch on every hop — including the cross-agent JSON-RPC.
 */
@SpringBootApplication
public class RegulusA2AExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegulusA2AExampleApplication.class, args);
    }
}
