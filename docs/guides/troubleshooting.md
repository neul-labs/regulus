# Troubleshooting Guide

Common issues and solutions when developing and operating Regulus-based AI agents.

---

## Quick Diagnosis

```bash
# Check application health
curl http://localhost:8080/actuator/health | jq

# Check specific components
curl http://localhost:8080/actuator/health/killSwitch | jq
curl http://localhost:8080/actuator/health/dataResidency | jq
curl http://localhost:8080/actuator/health/llm | jq

# View configuration
curl http://localhost:8080/actuator/configprops | jq '.contexts.application.beans | keys'

# Check metrics
curl http://localhost:8080/actuator/prometheus | grep regulus
```

---

## Authentication & Credentials

### GCP Credentials Not Found

**Error:**
```
com.google.auth.oauth2.GoogleAuthException: Failed to get credentials
```

**Solutions:**

1. **Local Development** - Use application default credentials:
   ```bash
   gcloud auth application-default login
   ```

2. **Service Account** - Set the key path:
   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
   ```

3. **Configuration** - Check your `application.yaml`:
   ```yaml
   regulus:
     ai:
       gcp:
         authentication:
           mode: APPLICATION_DEFAULT  # or SERVICE_ACCOUNT_FILE
           service-account-file: ${GOOGLE_APPLICATION_CREDENTIALS:}
   ```

4. **GKE/Cloud Run** - Ensure workload identity is configured:
   ```bash
   kubectl describe serviceaccount your-sa -n your-namespace
   ```

### Invalid API Key

**Error:**
```
dev.langchain4j.exception.AuthenticationException: Invalid API key
```

**Solutions:**

1. Verify key is set:
   ```bash
   echo $OPENAI_API_KEY | head -c 10  # Should show "sk-..."
   ```

2. Check configuration binding:
   ```yaml
   regulus:
     ai:
       llm:
         openai:
           api-key: ${OPENAI_API_KEY}  # Not ${openai.api.key}
   ```

3. Ensure no whitespace in environment variable:
   ```bash
   export OPENAI_API_KEY="$(echo $OPENAI_API_KEY | tr -d '[:space:]')"
   ```

---

## Data Residency

### Data Residency Violation

**Error:**
```
DataResidencyViolationException: Data residency violation: region us-central1
not allowed for UK_REGULATED data
```

**Diagnosis:**
```java
// Check current configuration
@Autowired
private DataResidencyEnforcer enforcer;

Set<String> allowed = enforcer.getAllowedRegionsForModel("your-model");
log.info("Allowed regions: {}", allowed);

// Check specific endpoint
boolean allowed = enforcer.isEndpointAllowed(
    "https://us-central1-aiplatform.googleapis.com/..."
);
log.info("Endpoint allowed: {}", allowed);
```

**Solutions:**

1. **Use UK region endpoint:**
   ```yaml
   regulus:
     ai:
       llm:
         gemini:
           location: europe-west2  # GCP London
   ```

2. **For non-regulated data, adjust classification:**
   ```java
   ResidencyCheckRequest request = new ResidencyCheckRequest(
       requestId,
       "internal-data",  // Not "customer-pii"
       sourceRegion,
       targetRegion,
       requestedBy,
       Map.of()
   );
   ```

3. **Add region to allowed list (if approved):**
   ```yaml
   regulus:
     ai:
       safety:
         data-residency:
           allowed-regions:
             - europe-west2
             - eu-west-2
             - uksouth
             - europe-west1  # Add if needed
   ```

### Cannot Determine Region from Endpoint

**Error:**
```
WARN DataResidencyEnforcer - Cannot determine region from endpoint: https://custom-llm.example.com
```

**Solutions:**

1. **Allow unknown regions for non-sensitive data:**
   ```yaml
   regulus:
     ai:
       safety:
         data-residency:
           allow-unknown-regions: true  # Only for non-PII data
   ```

2. **Add custom endpoint pattern:**
   ```java
   // Extend DataResidencyEnforcer to recognize custom patterns
   @Override
   protected String extractRegionFromEndpoint(String endpoint) {
       if (endpoint.contains("custom-llm.example.com")) {
           return "europe-west2"; // Known to be in UK
       }
       return super.extractRegionFromEndpoint(endpoint);
   }
   ```

---

## Kill Switch

### Kill Switch Blocking All Requests

**Symptoms:**
- All agent requests return error
- Logs show `KillSwitchInterceptor` blocking

**Diagnosis:**
```bash
# Check kill switch state
curl http://localhost:8080/actuator/health/killSwitch | jq

# Check audit log
curl http://localhost:8080/api/admin/kill-switch/audit | jq
```

**Solutions:**

1. **Check if global kill switch is active:**
   ```java
   @Autowired
   private KillSwitchManager killSwitchManager;

   boolean isActive = killSwitchManager.isGlobalKillSwitchActive();
   log.info("Global kill switch active: {}", isActive);
   ```

2. **Deactivate (with proper authorization):**
   ```java
   // If dual-control is enabled
   String requestId = dualControlKillSwitch.requestDeactivation(
       KillSwitchState.Scope.GLOBAL,
       null,
       "operator@bank.com"
   );
   // Then approve with second approver
   ```

3. **For development, disable dual-control:**
   ```yaml
   # application-local.yaml
   regulus:
     ai:
       safety:
         kill-switch:
           dual-control:
             enabled: false
   ```

### Dual-Control Approval Not Working

**Error:**
```
Approval attempted for unknown request: KS-XXXXXXXX
```

**Causes:**
- Request expired (default: 4 hours)
- Request already processed
- Wrong request ID

**Solutions:**

1. **Check pending requests:**
   ```java
   List<PendingRequest> pending = dualControlKillSwitch.getPendingRequests();
   pending.forEach(r -> log.info(
       "Request {}: {} (expires: {})",
       r.getRequestId(),
       r.getReason(),
       r.getRequestedAt().plus(Duration.ofHours(4))
   ));
   ```

2. **Extend expiry if needed:**
   ```yaml
   regulus:
     ai:
       safety:
         kill-switch:
           dual-control:
             request-expiry-hours: 8  # Extend from 4 hours
   ```

---

## MCP Protocol

### MCP Tools Not Discovered

**Symptoms:**
- `tools/list` returns empty array
- Tools not available to agents

**Diagnosis:**
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

**Solutions:**

1. **Ensure tools are in scanned packages:**
   ```java
   @SpringBootApplication
   @ComponentScan(basePackages = {
       "com.yourcompany.agent",
       "com.yourcompany.tools"  // Add tool package
   })
   public class Application { }
   ```

2. **Verify `@McpTool` annotation:**
   ```java
   @Component  // Must be a Spring bean
   @McpTool(
       name = "your_tool",
       description = "Tool description"
   )
   public class YourTool implements McpToolHandler { }
   ```

3. **Check MCP server is enabled:**
   ```yaml
   regulus:
     ai:
       mcp:
         server:
           enabled: true
   ```

### MCP Tool Execution Fails

**Error:**
```json
{"jsonrpc":"2.0","id":1,"error":{"code":-32603,"message":"Tool execution failed"}}
```

**Diagnosis:**
```bash
# Enable debug logging
logging:
  level:
    com.regulus.platform.agents.mcp: DEBUG
```

**Common Causes:**

1. **Missing required arguments:**
   ```json
   // Check your inputSchema
   {
     "required": ["arg1", "arg2"]  // Ensure all required args provided
   }
   ```

2. **Type mismatch:**
   ```java
   // Ensure correct type conversion
   BigDecimal amount = new BigDecimal(arguments.get("amount").toString());
   ```

3. **Policy guard blocking:**
   ```
   Check if @RequireLEI or @RequirePurposeCode annotations are satisfied
   ```

---

## LLM Integration

### LLM Timeout

**Error:**
```
java.util.concurrent.TimeoutException: Request timed out after 30000ms
```

**Solutions:**

1. **Increase timeout:**
   ```yaml
   regulus:
     ai:
       llm:
         timeout-ms: 60000  # 60 seconds
   ```

2. **Use streaming for long responses:**
   ```java
   llmClient.streamChat(messages, new TokenStreamHandler() {
       @Override
       public void onToken(String token) {
           // Process tokens as they arrive
       }
   });
   ```

3. **Check endpoint health:**
   ```bash
   curl -v https://europe-west2-aiplatform.googleapis.com/v1/projects/YOUR_PROJECT/locations/europe-west2/publishers/google/models/gemini-1.5-pro:generateContent
   ```

### LLM Rate Limited

**Error:**
```
ResourceExhaustedException: Quota exceeded for quota metric
```

**Solutions:**

1. **Enable retry with backoff:**
   ```yaml
   regulus:
     ai:
       llm:
         retry:
           enabled: true
           max-attempts: 3
           backoff-multiplier: 2
           initial-delay-ms: 1000
   ```

2. **Add circuit breaker:**
   ```yaml
   regulus:
     ai:
       resilience:
         circuit-breaker:
           enabled: true
           failure-rate-threshold: 50
           wait-duration-in-open-state: 30s
   ```

3. **Request quota increase from provider**

---

## Spring Boot Configuration

### Configuration Not Applied

**Symptoms:**
- Settings in `application.yaml` ignored
- Default values used instead

**Diagnosis:**
```bash
curl http://localhost:8080/actuator/env | jq '.propertySources[] | select(.name | contains("application"))'
```

**Solutions:**

1. **Check property binding:**
   ```java
   @ConfigurationProperties(prefix = "regulus.ai.safety")
   public class SafetyProperties {
       // Ensure setters exist for all properties
       private boolean enabled;
       public void setEnabled(boolean enabled) { this.enabled = enabled; }
   }
   ```

2. **Verify YAML indentation:**
   ```yaml
   regulus:
     ai:
       safety:  # Correct
         enabled: true

   # NOT:
   regulus:
   ai:
     safety:  # Wrong - ai should be indented under regulus
   ```

3. **Check profile is active:**
   ```bash
   java -jar app.jar --spring.profiles.active=production
   ```

### Bean Not Found

**Error:**
```
NoSuchBeanDefinitionException: No qualifying bean of type 'DataResidencyEnforcer'
```

**Solutions:**

1. **Check conditional is met:**
   ```yaml
   regulus:
     ai:
       safety:
         data-residency:
           enabled: true  # Must be true for bean creation
   ```

2. **Ensure starter is included:**
   ```kotlin
   dependencies {
       implementation("com.regulus.platform:regulus-ai-safety-starter")
   }
   ```

---

## Performance Issues

### High Latency

**Diagnosis:**
```bash
# Check metrics
curl http://localhost:8080/actuator/prometheus | grep -E "regulus.*latency|regulus.*duration"
```

**Solutions:**

1. **Enable response caching:**
   ```yaml
   regulus:
     ai:
       llm:
         cache:
           enabled: true
           ttl-seconds: 300
   ```

2. **Use streaming for UX:**
   ```java
   // Stream tokens to user as they arrive
   llmClient.streamChat(messages, handler);
   ```

3. **Optimize token count:**
   ```java
   // Use concise prompts
   // Limit max_tokens in response
   ```

### Memory Issues

**Error:**
```
OutOfMemoryError: Java heap space
```

**Solutions:**

1. **Increase heap size:**
   ```bash
   java -Xmx2g -jar app.jar
   ```

2. **Check for memory leaks:**
   ```bash
   curl http://localhost:8080/actuator/heapdump > heap.hprof
   # Analyze with VisualVM or MAT
   ```

3. **Clear violation/audit caches:**
   ```java
   dataResidencyEnforcer.clearViolations();
   ```

---

## Logging & Debugging

### Enable Debug Logging

```yaml
logging:
  level:
    com.regulus: DEBUG
    com.regulus.platform.agents.mcp: DEBUG
    com.regulus.platform.safety: DEBUG
    dev.langchain4j: DEBUG
```

### Useful Log Patterns

```bash
# Kill switch events
grep -E "KillSwitch|KILL" logs/application.log

# Data residency checks
grep -E "DataResidency|residency" logs/application.log

# MCP protocol
grep -E "mcp\.|MCP" logs/application.log

# LLM calls
grep -E "llm\.|LlmClient" logs/application.log
```

### Add Request Tracing

```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0  # 100% in dev, lower in prod
```

---

## Getting Help

### Before Asking for Help

1. **Check the logs** with DEBUG enabled
2. **Verify configuration** with actuator endpoints
3. **Test in isolation** - does the issue occur with minimal config?
4. **Check version compatibility** - are all Regulus starters aligned?

### Information to Provide

```markdown
## Issue Description
[Clear description of the problem]

## Environment
- Regulus version: X.Y.Z
- Java version: 21
- Spring Boot version: 3.3.0
- Cloud provider: GCP/AWS/Azure

## Configuration (sanitized)
```yaml
regulus:
  ai:
    # Relevant config
```

## Error Message
```
[Full stack trace]
```

## Steps to Reproduce
1. ...
2. ...

## Expected Behavior
[What should happen]

## Actual Behavior
[What actually happens]
```

---

## Related Documentation

- [Quickstart Tutorial](./quickstart-tutorial.md)
- [Starters Configuration](./starters.md)
- [Data Residency Guide](./data-residency.md)
- [Kill Switch Design](../governance/kill-switch.md)
