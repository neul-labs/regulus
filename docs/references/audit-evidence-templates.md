# Audit Evidence Templates

Pre-built templates for generating regulatory audit evidence from Regulus platform. Use these templates for Internal Audit walkthroughs, regulatory examinations, and compliance reporting.

---

## Template Index

| Template | Regulation | Frequency | Owner |
|----------|------------|-----------|-------|
| [Model Inventory Extract](#1-model-inventory-extract) | SS1/23 | Quarterly | Model Risk |
| [Kill Switch Audit Log](#2-kill-switch-audit-log) | PS21/3 | On-demand | Platform Ops |
| [Data Residency Compliance](#3-data-residency-compliance-report) | UK GDPR, SYSC 13.9 | Monthly | Data Privacy |
| [Consumer Duty Outcomes](#4-consumer-duty-outcomes-report) | Consumer Duty | Quarterly | Compliance |
| [Vendor Register Extract](#5-vendor-register-extract) | SS2/21 | Quarterly | Third Party Risk |
| [Validation Evidence Pack](#6-validation-evidence-pack) | SS1/23 | Per validation | Model Validation |

---

## 1. Model Inventory Extract

**Regulation**: PRA SS1/23 MRM1, MRM2
**Purpose**: Demonstrate comprehensive model inventory with risk classification

### Template

```json
{
  "reportMetadata": {
    "reportType": "MODEL_INVENTORY_EXTRACT",
    "generatedAt": "2025-01-25T10:00:00Z",
    "generatedBy": "regulus-platform",
    "reportingPeriod": {
      "from": "2024-10-01",
      "to": "2024-12-31"
    },
    "version": "1.0"
  },
  "summary": {
    "totalModels": 12,
    "byRiskTier": {
      "TIER_1": 2,
      "TIER_2": 5,
      "TIER_3": 4,
      "TIER_4": 1
    },
    "byStatus": {
      "PRODUCTION": 8,
      "STAGING": 2,
      "DEVELOPMENT": 2
    },
    "modelsRequiringRevalidation": 1
  },
  "models": [
    {
      "modelId": "mortgage-adviser-v1.2.0",
      "name": "Mortgage Affordability Adviser",
      "type": "AI_AGENT",
      "riskTier": "TIER_2",
      "owner": "Lending Team",
      "ownerEmail": "lending-ai@bank.com",
      "intendedUse": "Customer-facing mortgage affordability assessments",
      "dataClassification": "PII",
      "regulatoryScope": "FCA_MCOB",
      "deploymentStatus": "PRODUCTION",
      "deployedAt": "2024-11-15T09:00:00Z",
      "lastValidation": {
        "date": "2024-11-10T14:30:00Z",
        "outcome": "APPROVED",
        "validatedBy": "Model Risk Team",
        "nextReviewDate": "2025-05-10"
      },
      "performanceMetrics": {
        "accuracy": 0.94,
        "latencyP99Ms": 245,
        "errorRate": 0.002,
        "driftScore": 0.05
      }
    }
  ],
  "attestation": {
    "attestedBy": "Model Risk Officer",
    "attestedAt": "2025-01-25T10:00:00Z",
    "statement": "I confirm this inventory is complete and accurate as of the reporting date."
  }
}
```

### Java Generation

```java
@Service
public class AuditEvidenceGenerator {

    @Autowired
    private ModelRegistry modelRegistry;

    public ModelInventoryReport generateModelInventoryReport(
        LocalDate from,
        LocalDate to
    ) {
        List<ModelRegistryEntry> models = modelRegistry.findAll();

        return ModelInventoryReport.builder()
            .reportType("MODEL_INVENTORY_EXTRACT")
            .generatedAt(Instant.now())
            .reportingPeriod(new ReportingPeriod(from, to))
            .summary(calculateSummary(models))
            .models(models.stream()
                .map(this::toReportEntry)
                .toList())
            .build();
    }
}
```

---

## 2. Kill Switch Audit Log

**Regulation**: PRA PS21/3
**Purpose**: Demonstrate operational resilience controls and dual-control governance

### Template

```json
{
  "reportMetadata": {
    "reportType": "KILL_SWITCH_AUDIT_LOG",
    "generatedAt": "2025-01-25T10:00:00Z",
    "reportingPeriod": {
      "from": "2024-10-01",
      "to": "2024-12-31"
    }
  },
  "summary": {
    "totalActivations": 3,
    "emergencyActivations": 1,
    "dualControlActivations": 2,
    "averageResolutionTimeMinutes": 45,
    "drillsCompleted": 2
  },
  "activations": [
    {
      "requestId": "KS-A1B2C3D4",
      "type": "DUAL_CONTROL",
      "scope": "AGENT",
      "targetId": "mortgage-adviser-v1.2.0",
      "reason": "Model drift detected - exceeds threshold",
      "requestedBy": "risk-team@bank.com",
      "requestedAt": "2024-11-20T14:30:00Z",
      "approvals": [
        {
          "approver": "risk-team@bank.com",
          "approvedAt": "2024-11-20T14:30:00Z",
          "type": "REQUESTER"
        },
        {
          "approver": "ai-ops@bank.com",
          "approvedAt": "2024-11-20T14:45:00Z",
          "type": "SECOND_APPROVER"
        }
      ],
      "executedAt": "2024-11-20T14:45:00Z",
      "deactivatedAt": "2024-11-20T16:30:00Z",
      "deactivatedBy": "ai-ops@bank.com",
      "resolutionTimeMinutes": 120,
      "incidentTicket": "INC0012345",
      "rootCause": "Training data drift in credit bureau data",
      "remediation": "Model retrained with updated data"
    },
    {
      "requestId": null,
      "type": "EMERGENCY",
      "scope": "GLOBAL",
      "targetId": null,
      "reason": "EMERGENCY: Security incident detected",
      "requestedBy": "security-ops@bank.com",
      "requestedAt": "2024-12-05T03:15:00Z",
      "approvals": [],
      "executedAt": "2024-12-05T03:15:00Z",
      "deactivatedAt": "2024-12-05T05:30:00Z",
      "deactivatedBy": "security-ops@bank.com",
      "resolutionTimeMinutes": 135,
      "incidentTicket": "SEC0000789",
      "rootCause": "False positive from SIEM",
      "remediation": "SIEM rules tuned"
    }
  ],
  "drills": [
    {
      "drillId": "DRILL-2024-Q4-01",
      "date": "2024-10-15",
      "type": "PLANNED",
      "scope": "AGENT",
      "targetId": "test-agent",
      "participants": ["ai-ops@bank.com", "risk-team@bank.com"],
      "outcome": "SUCCESSFUL",
      "findingsCount": 0,
      "documentation": "https://confluence/kill-switch-drill-q4"
    }
  ],
  "configuration": {
    "dualControlEnabled": true,
    "requiredApprovers": 2,
    "emergencyBypassEnabled": true,
    "authorizedApprovers": [
      "risk-team@bank.com",
      "ai-ops@bank.com",
      "security-ops@bank.com"
    ]
  }
}
```

### Java Generation

```java
public KillSwitchAuditReport generateKillSwitchReport(
    LocalDate from,
    LocalDate to
) {
    List<DualControlAuditEntry> auditLog = dualControlKillSwitch.getAuditLog()
        .stream()
        .filter(e -> e.timestamp().isAfter(from.atStartOfDay().toInstant(ZoneOffset.UTC)))
        .filter(e -> e.timestamp().isBefore(to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)))
        .toList();

    return KillSwitchAuditReport.builder()
        .reportType("KILL_SWITCH_AUDIT_LOG")
        .generatedAt(Instant.now())
        .reportingPeriod(new ReportingPeriod(from, to))
        .summary(calculateKillSwitchSummary(auditLog))
        .activations(extractActivations(auditLog))
        .drills(getDrillRecords(from, to))
        .configuration(getCurrentConfiguration())
        .build();
}
```

---

## 3. Data Residency Compliance Report

**Regulation**: UK GDPR Articles 44-49, FCA SYSC 13.9
**Purpose**: Demonstrate data residency enforcement and violation tracking

### Template

```json
{
  "reportMetadata": {
    "reportType": "DATA_RESIDENCY_COMPLIANCE",
    "generatedAt": "2025-01-25T10:00:00Z",
    "reportingPeriod": {
      "from": "2024-10-01",
      "to": "2024-12-31"
    }
  },
  "summary": {
    "totalChecks": 125000,
    "allowedRequests": 124950,
    "blockedRequests": 45,
    "approvalRequiredRequests": 5,
    "violationRate": "0.04%"
  },
  "configuration": {
    "allowedRegions": [
      "europe-west2",
      "eu-west-2",
      "uksouth"
    ],
    "enforceUkResidency": true,
    "blockViolations": true
  },
  "violations": [
    {
      "violationId": "DRV-001",
      "timestamp": "2024-11-15T09:30:00Z",
      "dataType": "customer-pii",
      "classification": "PII",
      "attemptedRegion": "us-east-1",
      "allowedRegions": ["europe-west2", "eu-west-2", "uksouth"],
      "requestedBy": "batch-processor@system",
      "action": "BLOCKED",
      "rootCause": "Misconfigured batch job endpoint",
      "remediation": "Endpoint corrected to eu-west-2"
    }
  ],
  "regionUsage": {
    "europe-west2": {
      "requests": 85000,
      "percentage": "68%",
      "dataTypes": ["PII", "UK_REGULATED", "STANDARD"]
    },
    "eu-west-2": {
      "requests": 30000,
      "percentage": "24%",
      "dataTypes": ["STANDARD", "INTERNAL"]
    },
    "uksouth": {
      "requests": 9950,
      "percentage": "8%",
      "dataTypes": ["STANDARD"]
    }
  },
  "attestation": {
    "attestedBy": "Data Protection Officer",
    "attestedAt": "2025-01-25T10:00:00Z",
    "statement": "All data processing occurred in approved UK/EEA regions during the reporting period."
  }
}
```

---

## 4. Consumer Duty Outcomes Report

**Regulation**: FCA Consumer Duty (FG22/5)
**Purpose**: Demonstrate delivery of good customer outcomes

### Template

```json
{
  "reportMetadata": {
    "reportType": "CONSUMER_DUTY_OUTCOMES",
    "generatedAt": "2025-01-25T10:00:00Z",
    "reportingPeriod": {
      "from": "2024-10-01",
      "to": "2024-12-31"
    }
  },
  "productsAndServices": {
    "suitabilityChecks": {
      "total": 15576,
      "suitable": 15234,
      "unsuitable": 342,
      "unsuitableRate": "2.2%"
    },
    "targetMarketValidations": {
      "total": 15576,
      "passed": 15400,
      "failed": 176
    },
    "topUnsuitableReasons": [
      {"reason": "Income below threshold", "count": 150},
      {"reason": "Risk profile mismatch", "count": 120},
      {"reason": "Age restriction", "count": 72}
    ]
  },
  "priceAndValue": {
    "valueAssessments": 8234,
    "feeDisclosuresProvided": 8234,
    "feeDisclosureRate": "100%",
    "averageValueScore": 7.8
  },
  "consumerUnderstanding": {
    "communications": 45000,
    "averageReadabilityScore": 72.5,
    "targetReadabilityScore": 60,
    "jargonTermsReplaced": 1234,
    "customerComprehensionFeedback": 4.2
  },
  "consumerSupport": {
    "vulnerableCustomersIdentified": 89,
    "vulnerableCustomerActions": {
      "enhancedSupport": 89,
      "escalatedToSpecialist": 45,
      "referredToSocialServices": 3
    },
    "escalations": {
      "total": 245,
      "byReason": {
        "COMPLEXITY": 110,
        "VULNERABLE_CUSTOMER": 89,
        "COMPLAINT": 23,
        "HIGH_VALUE_DECISION": 23
      }
    },
    "complaints": {
      "received": 23,
      "resolvedWithinSla": 22,
      "slaComplianceRate": "95.7%",
      "averageResolutionHours": 18.5
    }
  },
  "crossCuttingRules": {
    "goodFaithViolations": 0,
    "foreseeableHarmPrevented": 15,
    "conflictsOfInterestDisclosed": 3
  },
  "attestation": {
    "attestedBy": "Consumer Duty Champion",
    "attestedAt": "2025-01-25T10:00:00Z",
    "statement": "The firm has acted to deliver good outcomes for retail customers during the reporting period."
  }
}
```

---

## 5. Vendor Register Extract

**Regulation**: PRA SS2/21
**Purpose**: Demonstrate third-party risk management for AI/ML providers

### Template

```json
{
  "reportMetadata": {
    "reportType": "VENDOR_REGISTER_EXTRACT",
    "generatedAt": "2025-01-25T10:00:00Z"
  },
  "summary": {
    "totalVendors": 5,
    "criticalVendors": 2,
    "vendorsDueReassessment": 1,
    "concentrationRiskScore": "LOW"
  },
  "vendors": [
    {
      "vendorId": "VENDOR-001",
      "name": "Google Cloud (Vertex AI)",
      "serviceType": "LLM_PROVIDER",
      "criticality": "CRITICAL",
      "regions": ["europe-west2"],
      "dataProcessingLocation": "UK",
      "contract": {
        "startDate": "2024-01-01",
        "endDate": "2026-12-31",
        "value": "£500,000/year",
        "noticePeriod": "90 days"
      },
      "dueDiligence": {
        "lastAssessment": "2024-06-15",
        "nextAssessment": "2025-06-15",
        "riskRating": "LOW",
        "socReport": "SOC 2 Type II",
        "isoCompliance": ["ISO 27001", "ISO 27017", "ISO 27018"]
      },
      "exitStrategy": {
        "documented": true,
        "lastTested": "2024-09-01",
        "alternativeProvider": "Azure OpenAI",
        "migrationTimeEstimate": "4 weeks"
      },
      "subprocessors": [
        {"name": "Google LLC", "location": "USA", "purpose": "Infrastructure"}
      ]
    }
  ]
}
```

---

## 6. Validation Evidence Pack

**Regulation**: PRA SS1/23 MRM3
**Purpose**: Provide complete validation evidence for model approval

### Template

```json
{
  "reportMetadata": {
    "reportType": "VALIDATION_EVIDENCE_PACK",
    "generatedAt": "2025-01-25T10:00:00Z",
    "modelId": "mortgage-adviser-v1.2.0"
  },
  "modelDetails": {
    "name": "Mortgage Affordability Adviser",
    "version": "1.2.0",
    "type": "AI_AGENT",
    "riskTier": "TIER_2",
    "owner": "Lending Team"
  },
  "validationScope": {
    "validationType": "INDEPENDENT",
    "validatedBy": "Model Risk Team",
    "validationDate": "2024-11-10",
    "areasAssessed": [
      "Conceptual soundness",
      "Data quality",
      "Performance testing",
      "Sensitivity analysis",
      "Governance controls"
    ]
  },
  "conceptualSoundness": {
    "assessment": "SATISFACTORY",
    "findings": [
      "Affordability calculation follows FCA MCOB 11 requirements",
      "Stress testing includes appropriate interest rate scenarios"
    ],
    "limitations": [
      "Does not account for future income changes",
      "Assumes stable employment"
    ]
  },
  "dataQuality": {
    "assessment": "SATISFACTORY",
    "dataSourcesReviewed": ["Credit bureau", "Customer inputs", "Property valuation"],
    "completenessScore": 0.98,
    "accuracyScore": 0.96,
    "findings": []
  },
  "performanceTesting": {
    "assessment": "SATISFACTORY",
    "metrics": {
      "accuracy": 0.94,
      "precision": 0.92,
      "recall": 0.95,
      "f1Score": 0.93
    },
    "backtestingPeriod": "2023-01-01 to 2024-06-30",
    "outOfTimeTesting": "PASSED"
  },
  "sensitivityAnalysis": {
    "assessment": "SATISFACTORY",
    "scenariosTested": [
      {"scenario": "Interest rate +2%", "impact": "Max mortgage -15%"},
      {"scenario": "Income -20%", "impact": "Max mortgage -25%"},
      {"scenario": "Expenses +30%", "impact": "Max mortgage -20%"}
    ]
  },
  "governanceControls": {
    "assessment": "SATISFACTORY",
    "controlsReviewed": [
      "Kill switch operational",
      "Dual-control enabled",
      "Data residency enforced",
      "Audit logging active"
    ]
  },
  "overallOutcome": {
    "decision": "APPROVED",
    "conditions": [
      "Quarterly performance monitoring required",
      "Annual revalidation"
    ],
    "nextReviewDate": "2025-11-10"
  },
  "approvals": [
    {
      "role": "Lead Validator",
      "name": "Jane Smith",
      "date": "2024-11-10",
      "signature": "[Digital signature]"
    },
    {
      "role": "Model Risk Head",
      "name": "John Doe",
      "date": "2024-11-12",
      "signature": "[Digital signature]"
    }
  ]
}
```

---

## Generating Evidence

### Gradle Task

```kotlin
// build.gradle.kts
tasks.register("generateAuditEvidence") {
    group = "compliance"
    description = "Generate audit evidence pack for regulatory reporting"

    doLast {
        exec {
            commandLine("java", "-jar", "regulus-audit-cli.jar",
                "--report-type", "ALL",
                "--from", project.findProperty("from") ?: "2024-10-01",
                "--to", project.findProperty("to") ?: "2024-12-31",
                "--output", "build/audit-evidence"
            )
        }
    }
}
```

### CLI Usage

```bash
# Generate all evidence reports
./gradlew generateAuditEvidence -Pfrom=2024-10-01 -Pto=2024-12-31

# Generate specific report
java -jar regulus-audit-cli.jar \
  --report-type MODEL_INVENTORY \
  --from 2024-10-01 \
  --to 2024-12-31 \
  --output ./evidence
```

---

## Evidence Retention

| Evidence Type | Retention Period | Storage Location |
|---------------|------------------|------------------|
| Model inventory | 7 years | GRC repository |
| Kill switch logs | 7 years | Splunk + backup |
| Data residency | 7 years | Compliance database |
| Consumer Duty | 7 years | Audit database |
| Vendor register | 7 years | VRM system |
| Validation packs | 7 years | GRC repository |

---

## Related Documentation

- [Regulatory Reference](./regulatory-reference.md)
- [Risk Control Matrix](../governance/risk-control-matrix.md)
- [Model Registry](../governance/model-registry.md)
- [Kill Switch Design](../governance/kill-switch.md)
