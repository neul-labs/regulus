rootProject.name = "regulus"

include(
    // Bill of Materials
    "platform:regulus-ai-bom",

    // Core Libraries — existing
    "platform:core:regulus-ai-policy",
    "platform:core:regulus-ai-privacy",
    "platform:core:regulus-ai-kill-switch",
    "platform:core:regulus-ai-observability",
    "platform:core:regulus-ai-llm",
    "platform:core:regulus-ai-persistence",

    // Core Libraries — ADK extension surface
    "platform:core:regulus-ai-adk-plugins",
    "platform:core:regulus-ai-adk-services",
    "platform:core:regulus-ai-adk-a2a",
    "platform:core:regulus-ai-compliance",

    // Core Libraries — Governance + GRC
    "platform:core:regulus-ai-governance",
    "platform:core:regulus-ai-grc-adapters",

    // Spring Boot Starters
    "platform:starters:regulus-ai-agents-spring-boot-starter",
    "platform:starters:regulus-ai-governance-starter",
    "platform:starters:regulus-ai-safety-starter",
    "platform:starters:regulus-ai-adk-spring-boot-starter",

    // DSL Parsers
    "platform:dsl:regulus-ai-dsl-kotlin",
    "platform:dsl:regulus-ai-dsl-yaml",

    // Gradle Plugin
    "platform:gradle-plugin:regulus-compliance-gradle-plugin",

    // Examples — ADK-first
    "examples:adk-quickstart",
    "examples:adk-multi-agent-a2a",
    "examples:adk-vertex-agent-engine-deploy",

    // Examples — legacy (LangChain4j path, retained as alternative runtime)
    "examples:quickstart",
    "examples:agent-demo"
)
