# Regulus Platform Makefile
# Common development tasks and shortcuts

.PHONY: help build test clean run lint check quickstart

# Default target
help:
	@echo "Regulus Platform - Available targets:"
	@echo ""
	@echo "  quickstart  - Start the quickstart example app (recommended!)"
	@echo "  build       - Build all modules"
	@echo "  test        - Run all tests"
	@echo "  clean       - Clean build outputs"
	@echo "  lint        - Run code formatting checks"
	@echo "  check       - Run all checks (lint + test)"
	@echo "  deps        - Show dependency tree"
	@echo ""
	@echo "Quick Start:"
	@echo "  quickstart       - Run the quickstart example app"
	@echo "  quickstart-build - Build quickstart only"
	@echo "  curl-examples    - Show curl command examples"
	@echo ""
	@echo "Module-specific:"
	@echo "  build-policy     - Build policy module"
	@echo "  build-privacy    - Build privacy module"
	@echo "  build-killswitch - Build kill-switch module"
	@echo "  build-starters   - Build all starters"
	@echo ""
	@echo "Development:"
	@echo "  dev-setup   - Set up local development environment"
	@echo "  refresh     - Refresh Gradle dependencies"

# Build targets
build:
	./gradlew build -x test

build-policy:
	./gradlew :platform:core:regulus-ai-policy:build

build-privacy:
	./gradlew :platform:core:regulus-ai-privacy:build

build-killswitch:
	./gradlew :platform:core:regulus-ai-kill-switch:build

build-starters:
	./gradlew :platform:starters:regulus-ai-agents-spring-boot-starter:build \
	          :platform:starters:regulus-ai-governance-starter:build \
	          :platform:starters:regulus-ai-safety-starter:build

# Test targets
test:
	./gradlew test

test-policy:
	./gradlew :platform:core:regulus-ai-policy:test

test-privacy:
	./gradlew :platform:core:regulus-ai-privacy:test

# Clean targets
clean:
	./gradlew clean

# Lint and formatting
lint:
	./gradlew spotlessCheck || echo "Spotless not configured yet"

format:
	./gradlew spotlessApply || echo "Spotless not configured yet"

# Combined checks
check: lint test

# Dependency management
deps:
	./gradlew dependencies --configuration compileClasspath

refresh:
	./gradlew --refresh-dependencies build -x test

# Development setup
dev-setup:
	@echo "Setting up local development environment..."
	@mkdir -p .gradle-cache
	./gradlew wrapper --gradle-version 8.5
	./gradlew build -x test
	@echo "Development environment ready!"

# Quick Start targets
quickstart:
	@echo "Starting Regulus Quickstart Application..."
	@echo "Once started, try: make curl-examples"
	@echo ""
	./gradlew :examples:quickstart:bootRun

quickstart-build:
	./gradlew :examples:quickstart:build

curl-examples:
	@echo "=== Regulus Platform API Examples ==="
	@echo ""
	@echo "1. Health Check:"
	@echo '   curl http://localhost:8080/api/health'
	@echo ""
	@echo "2. Validate ISO 20022 Payment:"
	@echo '   curl -X POST http://localhost:8080/api/validate \'
	@echo '     -H "Content-Type: application/json" \'
	@echo '     -d '\''{"message": "<CstmrCdtTrfInitn><GrpHdr><MsgId>MSG001</MsgId></GrpHdr><PmtInf><CdtTrfTxInf/></PmtInf></CstmrCdtTrfInitn>", "messageType": "pain.001"}'\'
	@echo ""
	@echo "3. Calculate Risk Score:"
	@echo '   curl -X POST http://localhost:8080/api/risk \'
	@echo '     -H "Content-Type: application/json" \'
	@echo '     -d '\''{"transactionId": "TX123", "amount": 15000, "currency": "GBP", "receiverCountry": "RU"}'\'
	@echo ""
	@echo "4. Redact PII:"
	@echo '   curl -X POST http://localhost:8080/api/redact \'
	@echo '     -H "Content-Type: application/json" \'
	@echo '     -d '\''{"content": "Customer sort code 12-34-56, card 4111-1111-1111-1111"}'\'
	@echo ""
	@echo "5. Check Policy:"
	@echo '   curl -X POST http://localhost:8080/api/policy/check \'
	@echo '     -H "Content-Type: application/json" \'
	@echo '     -d '\''{"lei": "529900T8BM49AURSDO55", "purposeCode": "SERVICE_DELIVERY", "consentId": "CONSENT123"}'\'
	@echo ""
	@echo "6. Toggle Kill Switch (Admin):"
	@echo '   curl -X POST http://localhost:8080/api/admin/killswitch \'
	@echo '     -H "Content-Type: application/json" \'
	@echo '     -d '\''{"scope": "global", "enabled": true, "reason": "Testing"}'\'

# Docker targets (for future use)
docker-build:
	@echo "Docker build not yet configured"

docker-run:
	@echo "Docker run not yet configured"
