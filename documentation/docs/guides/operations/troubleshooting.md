# Troubleshooting

Common issues and solutions for Regulus agents.

## LLM Connection Issues

### Authentication Failures

**Symptom**: `UNAUTHENTICATED` or `401 Unauthorized` errors

**Google Vertex AI**:
```bash
# Verify credentials
gcloud auth application-default print-access-token

# Check environment variable
echo $GOOGLE_APPLICATION_CREDENTIALS

# Verify service account permissions
gcloud projects get-iam-policy $PROJECT_ID \
  --filter="bindings.members:serviceAccount:your-sa@..." \
  --format="table(bindings.role)"
```

**Solution**:
1. Ensure `GOOGLE_APPLICATION_CREDENTIALS` points to valid key file
2. Verify service account has `roles/aiplatform.user` role
3. Check key file hasn't expired

**OpenAI**:
```bash
# Test API key
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"
```

**Solution**:
1. Verify API key is correct
2. Check key hasn't been revoked
3. Ensure billing is active on OpenAI account

### Rate Limiting

**Symptom**: `429 Too Many Requests` or `RESOURCE_EXHAUSTED`

**Diagnosis**:
```bash
# Check current rate limit metrics
curl http://localhost:8080/actuator/metrics/regulus.llm.requests.total
```

**Solution**:
```yaml
regulus:
  llm:
    retry:
      max-attempts: 3
      on-rate-limit:
        enabled: true
        initial-delay: 1s
        max-delay: 60s
        respect-retry-after: true
```

### Timeout Errors

**Symptom**: `DEADLINE_EXCEEDED` or connection timeout

**Solution**:
```yaml
regulus:
  llm:
    timeout: 60s  # Increase timeout
    connect-timeout: 10s
```

## Kill Switch Issues

### Kill Switch Not Activating

**Diagnosis**:
```bash
# Check kill switch status
curl http://localhost:8080/actuator/health

# Check backend connectivity
redis-cli -h $REDIS_HOST ping
```

**Common Causes**:
1. Backend (Redis/Consul) unreachable
2. Incorrect key prefix configuration
3. Dual-control approval not completed

**Solution**:
```yaml
regulus:
  kill-switch:
    backend: redis
    redis:
      host: ${REDIS_HOST}
      port: 6379
      connection-timeout: 5s
```

### Kill Switch Stuck Active

**Diagnosis**:
```bash
# Check activation details
redis-cli get "regulus:killswitch:global:active"
redis-cli get "regulus:killswitch:global:reason"
```

**Solution**:
1. Use admin endpoint to deactivate (requires dual-control)
2. Or use emergency bypass (must be enabled and audited)

```bash
curl -X POST http://localhost:8080/api/admin/kill-switch/deactivate \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"scope": "global", "reason": "Issue resolved"}'
```

## Policy Guard Issues

### Unexpected Policy Violations

**Diagnosis**:
```bash
# Check policy configuration
curl http://localhost:8080/actuator/configprops | jq '.regulus.policy'

# Check violation logs
grep "PolicyViolation" /var/log/app.log | tail -20
```

**Common Causes**:
1. Missing purpose code in request
2. Consent not properly passed
3. Purpose code not in allowed list

**Solution**:
```java
// Ensure context is properly built
PolicyContext context = PolicyContext.builder()
    .purposeCode("CUSTOMER_SUPPORT")  // Must match config
    .hasConsent(true)                  // Must be true if required
    .userId(request.userId())
    .build();
```

### Policy Evaluation Slow

**Diagnosis**:
```bash
# Check policy latency metrics
curl http://localhost:8080/actuator/metrics/regulus.policy.latency
```

**Solution**:
1. Order guards by speed (fast guards first)
2. Cache expensive lookups (LEI validation)
3. Use async validation where possible

## PII Redaction Issues

### PII Not Being Redacted

**Diagnosis**:
```bash
# Check privacy configuration
curl http://localhost:8080/actuator/configprops | jq '.regulus.privacy'
```

**Common Causes**:
1. Pattern not enabled in configuration
2. Input format doesn't match pattern
3. Privacy filter not injected

**Solution**:
```yaml
regulus:
  privacy:
    redaction:
      enabled: true
      patterns:
        nino: true
        sort-code: true
        account-number: true
```

### Over-Redaction

**Symptom**: Non-PII text being redacted

**Diagnosis**:
```java
RedactionResult result = privacyFilter.redactWithDetails(input);
result.detections().forEach(d ->
    log.info("Detected: {} at {}-{}", d.type(), d.start(), d.end())
);
```

**Solution**:
1. Review pattern regex for false positives
2. Add allowlist for known safe patterns
3. Adjust pattern sensitivity

## Memory Issues

### OutOfMemoryError

**Diagnosis**:
```bash
# Check heap usage
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Generate heap dump
jcmd $PID GC.heap_dump /tmp/heapdump.hprof
```

**Solution**:
```dockerfile
# Increase heap size
ENTRYPOINT ["java", "-Xmx1g", "-Xms512m", "-jar", "app.jar"]
```

```yaml
# Kubernetes resources
resources:
  limits:
    memory: "1.5Gi"
  requests:
    memory: "1Gi"
```

### High GC Overhead

**Diagnosis**:
```bash
# Enable GC logging
java -Xlog:gc*:file=/var/log/gc.log -jar app.jar
```

**Solution**:
1. Tune GC parameters
2. Review for memory leaks
3. Use streaming for large responses

## Connection Pool Issues

### Database Connection Exhaustion

**Symptom**: `HikariPool-1 - Connection is not available`

**Diagnosis**:
```bash
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

**Solution**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      leak-detection-threshold: 60000  # Enable leak detection
```

### Redis Connection Issues

**Symptom**: `Unable to connect to Redis`

**Diagnosis**:
```bash
redis-cli -h $REDIS_HOST ping
redis-cli -h $REDIS_HOST info clients
```

**Solution**:
```yaml
spring:
  data:
    redis:
      timeout: 5s
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
```

## Kubernetes Issues

### Pod CrashLoopBackOff

**Diagnosis**:
```bash
kubectl logs -f deployment/regulus-agent --previous
kubectl describe pod $POD_NAME
```

**Common Causes**:
1. Failed health checks
2. Missing environment variables
3. Secret not mounted

**Solution**:
1. Check probe configuration
2. Verify all required env vars are set
3. Check secret exists and is mounted

### Readiness Probe Failing

**Diagnosis**:
```bash
kubectl describe pod $POD_NAME | grep -A 10 "Readiness"
curl http://localhost:8080/actuator/health/readiness
```

**Solution**:
```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30  # Increase if app needs more startup time
  periodSeconds: 10
  failureThreshold: 3
```

## Debugging Commands

### Check Application Health
```bash
curl http://localhost:8080/actuator/health | jq
```

### View Configuration
```bash
curl http://localhost:8080/actuator/configprops | jq '.regulus'
```

### Check Metrics
```bash
curl http://localhost:8080/actuator/metrics
```

### Thread Dump
```bash
curl http://localhost:8080/actuator/threaddump | jq
```

### Heap Summary
```bash
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

## Getting Help

If issues persist:

1. **Check logs** - Review application and infrastructure logs
2. **Enable debug logging** - `logging.level.com.regulus=DEBUG`
3. **Review metrics** - Check for anomalies in dashboards
4. **Open issue** - Report on GitHub with reproduction steps
