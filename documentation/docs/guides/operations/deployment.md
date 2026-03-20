# Deployment

Production deployment guide for Regulus agents.

## Docker

### Building the Image

```dockerfile title="Dockerfile"
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Add non-root user
RUN addgroup -S regulus && adduser -S regulus -G regulus
USER regulus

COPY build/libs/app.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose

```yaml title="docker-compose.yml"
version: '3.8'

services:
  agent:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - GOOGLE_CLOUD_PROJECT=${GOOGLE_CLOUD_PROJECT}
      - GOOGLE_APPLICATION_CREDENTIALS=/secrets/gcp-key.json
    volumes:
      - ./secrets:/secrets:ro
    healthcheck:
      test: ["CMD", "wget", "-q", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    deploy:
      resources:
        limits:
          memory: 1G
          cpus: '1'

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data

volumes:
  redis-data:
```

## Kubernetes

### Deployment

```yaml title="k8s/deployment.yaml"
apiVersion: apps/v1
kind: Deployment
metadata:
  name: regulus-agent
  labels:
    app: regulus-agent
spec:
  replicas: 3
  selector:
    matchLabels:
      app: regulus-agent
  template:
    metadata:
      labels:
        app: regulus-agent
    spec:
      serviceAccountName: regulus-agent
      containers:
        - name: agent
          image: regulus-agent:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: GOOGLE_CLOUD_PROJECT
              valueFrom:
                secretKeyRef:
                  name: gcp-credentials
                  key: project-id
          envFrom:
            - configMapRef:
                name: regulus-config
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
          volumeMounts:
            - name: gcp-credentials
              mountPath: /secrets
              readOnly: true
      volumes:
        - name: gcp-credentials
          secret:
            secretName: gcp-credentials
```

### Service

```yaml title="k8s/service.yaml"
apiVersion: v1
kind: Service
metadata:
  name: regulus-agent
spec:
  selector:
    app: regulus-agent
  ports:
    - port: 80
      targetPort: 8080
  type: ClusterIP
```

### Ingress

```yaml title="k8s/ingress.yaml"
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: regulus-agent
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - agent.regulus.internal
      secretName: regulus-tls
  rules:
    - host: agent.regulus.internal
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: regulus-agent
                port:
                  number: 80
```

### ConfigMap

```yaml title="k8s/configmap.yaml"
apiVersion: v1
kind: ConfigMap
metadata:
  name: regulus-config
data:
  REGULUS_LLM_PROVIDER: "gemini"
  REGULUS_LLM_GEMINI_LOCATION: "europe-west2"
  REGULUS_POLICY_REQUIRE_PURPOSE_CODE: "true"
  REGULUS_KILL_SWITCH_ENABLED: "true"
```

### Secrets

```yaml title="k8s/secrets.yaml"
apiVersion: v1
kind: Secret
metadata:
  name: gcp-credentials
type: Opaque
data:
  project-id: <base64-encoded-project-id>
  service-account.json: <base64-encoded-key>
```

### HPA (Horizontal Pod Autoscaler)

```yaml title="k8s/hpa.yaml"
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: regulus-agent
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: regulus-agent
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

## Helm Chart

### values.yaml

```yaml title="helm/values.yaml"
replicaCount: 3

image:
  repository: regulus-agent
  tag: latest
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 80

ingress:
  enabled: true
  className: nginx
  hosts:
    - host: agent.regulus.internal
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: regulus-tls
      hosts:
        - agent.regulus.internal

resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "1"

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70

config:
  llm:
    provider: gemini
    location: europe-west2
  policy:
    requirePurposeCode: true
  killSwitch:
    enabled: true
```

## Environment Configuration

### Production Profile

```yaml title="application-prod.yml"
server:
  port: 8080
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true

regulus:
  llm:
    provider: gemini
    gemini:
      project-id: ${GOOGLE_CLOUD_PROJECT}
      location: europe-west2
      model: gemini-2.0-flash

  kill-switch:
    enabled: true
    backend: redis

  observability:
    metrics:
      enabled: true
    tracing:
      enabled: true
      sampling-rate: 0.1
```

## Health Checks

### Spring Actuator Configuration

```yaml
management:
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState,db,redis

  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
```

### Custom Health Indicator

```java
@Component
public class LlmHealthIndicator implements HealthIndicator {

    private final LlmClient llmClient;

    @Override
    public Health health() {
        try {
            llmClient.ping().block(Duration.ofSeconds(5));
            return Health.up()
                .withDetail("provider", llmClient.getProvider())
                .build();
        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }
}
```

## Security Hardening

### Network Policies

```yaml title="k8s/network-policy.yaml"
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: regulus-agent
spec:
  podSelector:
    matchLabels:
      app: regulus-agent
  policyTypes:
    - Ingress
    - Egress
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              name: ingress-nginx
      ports:
        - protocol: TCP
          port: 8080
  egress:
    - to:
        - ipBlock:
            cidr: 0.0.0.0/0
      ports:
        - protocol: TCP
          port: 443  # LLM APIs
    - to:
        - namespaceSelector:
            matchLabels:
              name: redis
      ports:
        - protocol: TCP
          port: 6379
```

### Pod Security

```yaml
spec:
  securityContext:
    runAsNonRoot: true
    runAsUser: 1000
    fsGroup: 1000
  containers:
    - name: agent
      securityContext:
        allowPrivilegeEscalation: false
        readOnlyRootFilesystem: true
        capabilities:
          drop:
            - ALL
```

## Rollout Strategy

```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
```

## Best Practices

1. **Use health probes** - Configure liveness and readiness probes
2. **Set resource limits** - Prevent resource exhaustion
3. **Enable HPA** - Scale based on load
4. **Use secrets management** - Never store secrets in config
5. **Network isolation** - Use network policies
6. **Graceful shutdown** - Handle in-flight requests
7. **Rolling updates** - Zero-downtime deployments
