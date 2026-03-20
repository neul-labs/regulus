# Glossary

Every acronym used anywhere in the Regulus docs. One line each; links to the
page where the term is explained in full where applicable.

## A
- **A2A** — Agent-to-Agent. ADK's JSON-RPC protocol for agents to call each
  other. See [Services → A2A](../services/a2a.md).
- **ADK** — [Agent Development Kit](https://github.com/google/adk-java).
  Google's open-source toolkit for building AI agents.
- **AI Act** — [EU AI Act](../compliance/eu/eu-ai-act.md). Regulation EU
  2024/1689 governing AI systems in the EU.
- **Annex III** — Schedule of high-risk AI systems under the EU AI Act.
- **ADC** — Application Default Credentials. How Google client libraries
  authenticate on a developer's machine.

## B
- **BIC** — Business Identifier Code. The 8 or 11-character SWIFT identifier
  of a bank. Built-in Regulus PII pattern.

## C
- **CMEK** — Customer-Managed Encryption Keys. You hold the key, the cloud
  provider uses it via KMS. See [Data residency](data-residency.md).
- **Consumer Duty** — FCA outcomes-based standard for retail financial
  services (FG22/5, PS22/9). See [Compliance → FCA SYSC](../compliance/uk/fca-sysc.md).

## D
- **DORA** — Digital Operational Resilience Act. EU Regulation 2022/2554.
  See [Compliance → DORA](../compliance/eu/dora.md).
- **DPA** — Data Processing Agreement (GDPR Art. 28). Also: UK Data
  Protection Act 2018.
- **DPIA** — Data Protection Impact Assessment (GDPR Art. 35).
- **DPO** — Data Protection Officer (GDPR Arts. 37–39).
- **DSPT** — [NHS Data Security and Protection Toolkit](../compliance/uk/nhs-dspt.md).

## E
- **EDPB** — European Data Protection Board. Coordinates EU national DPAs.
- **EHDS** — [European Health Data Space](../compliance/eu/ehds.md). EU
  Regulation 2025/327.
- **ENISA** — EU Agency for Cybersecurity. Major NIS2 authority.

## F
- **FCA** — Financial Conduct Authority. UK conduct regulator for financial
  services.

## G
- **GCP** — Google Cloud Platform.
- **GDPR** — General Data Protection Regulation. EU 2016/679. See
  [Compliance → GDPR](../compliance/eu/gdpr.md).
- **GHCR** — GitHub Container Registry. Regulus' reference container images
  ship here.

## H
- **HITL** — Human-in-the-Loop. The pattern where a human approves an action
  before the system proceeds. Implemented in ADK via `ToolConfirmation`.

## I
- **IBAN** — International Bank Account Number. Built-in Regulus PII pattern.
- **ICO** — Information Commissioner's Office. UK data-protection regulator.
- **ICT** — Information and Communication Technology. The term DORA and NIS2
  use for what most engineers call "IT systems."
- **IDTA** — International Data Transfer Agreement. UK's analogue to EU SCCs.

## J
- **JSON-RPC** — Transport ADK's A2A protocol uses.

## K
- **KMS** — Key Management Service. The encryption-key broker (GCP KMS, AWS
  KMS).

## L
- **LEI** — Legal Entity Identifier. 20-character ISO 17442 code identifying
  a legal entity in financial markets.

## M
- **MCP** — Model Context Protocol. ADK supports both A2A and MCP for
  inter-agent / agent-to-tool communication.
- **MiFID II** — Markets in Financial Instruments Directive II. EU
  investment-services rulebook.

## N
- **NHS** — National Health Service (UK).
- **NHS Number** — 10-digit UK patient identifier. Built-in Regulus PII pattern.
- **NINO** — National Insurance Number. UK SSN-equivalent. Built-in Regulus
  PII pattern.
- **NIS2** — Network and Information Security Directive 2. EU 2022/2555.
  See [Compliance → NIS2](../compliance/eu/nis2.md).

## P
- **PII** — Personally Identifiable Information.
- **PRA** — Prudential Regulation Authority. UK prudential supervisor for
  banks and insurers.
- **Postcode** — UK postcode. Built-in Regulus PII pattern.

## R
- **Regtech** — Regulatory technology. See [What is regtech?](regtech-intro.md).
- **RPO** — Recovery Point Objective. How much data loss is tolerable in an
  incident (used in DORA audit schema).
- **RTO** — Recovery Time Objective. How quickly service must return.

## S
- **SCC** — Standard Contractual Clauses. EU template contracts for
  international data transfers.
- **SIRI** — Serious Incident Requiring Investigation. NHS DSPT incident
  classification.
- **SMF** — Senior Management Function. FCA / PRA Senior Managers Regime
  responsibility code.
- **SS1/23** — [PRA Supervisory Statement 1/23](../compliance/uk/pra-ss1-23.md).
  Model Risk Management.
- **SS2/21** — [PRA Supervisory Statement 2/21](../compliance/uk/pra-ss2-21.md).
  Outsourcing & Third-Party Risk.
- **SSO** — Single sign-on.
- **SYSC** — Senior Management Arrangements, Systems and Controls. FCA
  Handbook section. See [Compliance → FCA SYSC](../compliance/uk/fca-sysc.md).

## T
- **ToolConfirmation** — ADK primitive for human-in-the-loop confirmation
  of a tool action. Regulus dual control uses this.

## U
- **UK GDPR** — UK version of GDPR, post-Brexit. See [Compliance → UK GDPR](../compliance/uk/uk-gdpr.md).

## V
- **Vertex AI** — Google Cloud's managed ML / LLM platform; ADK's reference
  model backend.
- **Vertex AI Agent Engine** — Vertex's managed runtime for deployed ADK
  agents.
