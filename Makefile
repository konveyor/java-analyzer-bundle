# Makefile for Java Analyzer Bundle
# Replicates GitHub Actions CI/CD pipeline for local verification

.PHONY: help all ci clean clean-containers clean-go phase1 phase2 unit-tests build-container run-integration-tests set-version

# Detect container runtime (prefer Podman, fallback to Docker)
CONTAINER_RUNTIME := $(shell command -v podman 2>/dev/null || command -v docker 2>/dev/null)
ifeq ($(CONTAINER_RUNTIME),)
$(error Neither podman nor docker is installed)
endif

# Set volume flags based on container runtime
ifeq ($(findstring podman,$(CONTAINER_RUNTIME)),podman)
VOLUME_FLAGS := :Z
else
VOLUME_FLAGS :=
endif

# Variables
IMAGE_NAME := jdtls-analyzer:test
REPO_ROOT := $(shell pwd)
GO_MODULE := java-analyzer-bundle.test/integration
BUNDLE_VERSION ?= 1.1.0-SNAPSHOT
TYCHO_VERSION := 4.0.7

# Default target
help:
	@echo "======================================================================"
	@echo "Java Analyzer Bundle - CI/CD Verification Makefile"
	@echo "======================================================================"
	@echo ""
	@echo "Available targets:"
	@echo ""
	@echo "  make ci                  - Run complete CI/CD pipeline (Phase 1 + 2)"
	@echo "  make phase1              - Run Phase 1: Unit tests only"
	@echo "  make phase2              - Run Phase 2: Integration tests only"
	@echo ""
	@echo "Phase 1 targets:"
	@echo "  make unit-tests          - Run Maven unit tests"
	@echo ""
	@echo "Phase 2 targets:"
	@echo "  make build-container     - Build JDT.LS container image"
	@echo "  make run-integration-tests - Run integration tests in container"
	@echo ""
	@echo "Version targets:"
	@echo "  make set-version         - Set bundle version (uses BUNDLE_VERSION)"
	@echo ""
	@echo "Utility targets:"
	@echo "  make clean               - Clean all build artifacts"
	@echo "  make clean-containers    - Remove container images"
	@echo "  make clean-go            - Clean Go build artifacts"
	@echo ""
	@echo "Variables:"
	@echo "  BUNDLE_VERSION           - Bundle version (default: 1.1.0-SNAPSHOT)"
	@echo "                             Set to release version for non-SNAPSHOT builds"
	@echo "                             Example: make ci BUNDLE_VERSION=1.2.0"
	@echo ""
	@echo "Container runtime: $(CONTAINER_RUNTIME)"
	@echo "======================================================================"

# Run complete CI/CD pipeline
ci: phase1 phase2
	@echo ""
	@echo "======================================================================"
	@echo "✓ Complete CI/CD Pipeline Succeeded!"
	@echo "======================================================================"

# Alias for consistency
all: ci

# Phase 1: Unit Tests
phase1: unit-tests
	@echo ""
	@echo "======================================================================"
	@echo "✓ Phase 1 Complete: Unit tests passed"
	@echo "======================================================================"

# Phase 2: Integration Tests
phase2: build-container run-integration-tests
	@echo ""
	@echo "======================================================================"
	@echo "✓ Phase 2 Complete: Integration tests passed"
	@echo "======================================================================"

# Version management
set-version:
ifeq ($(findstring -SNAPSHOT,$(BUNDLE_VERSION)),)
	@echo "======================================================================"
	@echo "Setting release version: $(BUNDLE_VERSION)"
	@echo "======================================================================"
	mvn -B org.eclipse.tycho:tycho-versions-plugin:$(TYCHO_VERSION):set-version -DnewVersion=$(BUNDLE_VERSION)
else
	@echo "Using SNAPSHOT version: $(BUNDLE_VERSION)"
endif

# Phase 1 Targets
unit-tests: set-version
	@echo "======================================================================"
	@echo "Phase 1: Running Unit Tests (version: $(BUNDLE_VERSION))"
	@echo "======================================================================"
	@echo ""
	mvn clean integration-test

# Phase 2 Targets
build-container:
	@echo ""
	@echo "======================================================================"
	@echo "Phase 2: Building JDT.LS Container Image (version: $(BUNDLE_VERSION))"
	@echo "======================================================================"
	@echo ""
	@echo "Using container runtime: $(CONTAINER_RUNTIME)"
	@echo ""
	$(CONTAINER_RUNTIME) build -t $(IMAGE_NAME) -f Dockerfile.test --build-arg BUNDLE_VERSION=$(BUNDLE_VERSION) .
	@echo ""
	@echo "✓ Container image built: $(IMAGE_NAME)"

run-integration-tests:
	@echo ""
	@echo "======================================================================"
	@echo "Phase 2: Running Integration Tests in Container"
	@echo "======================================================================"
	@echo ""
	@echo "Installing Go and running tests inside container..."
	$(CONTAINER_RUNTIME) run --rm \
		-v $(REPO_ROOT)/java-analyzer-bundle.test:/tests$(VOLUME_FLAGS) \
		-e WORKSPACE_DIR=/tests/projects \
		-e JDTLS_PATH=/jdtls \
		--workdir /tests/integration \
		--entrypoint /bin/sh \
		$(IMAGE_NAME) \
		-c "microdnf install -y golang && go mod download && go test -v"
	@echo ""
	@echo "✓ Integration tests passed"

# Clean targets
clean: clean-go
	@echo "======================================================================"
	@echo "Cleaning Build Artifacts"
	@echo "======================================================================"
	mvn clean
	@echo ""
	@echo "✓ Maven artifacts cleaned"

clean-containers:
	@echo "======================================================================"
	@echo "Removing Container Images"
	@echo "======================================================================"
	-$(CONTAINER_RUNTIME) rmi $(IMAGE_NAME) 2>/dev/null || true
	@echo ""
	@echo "✓ Container images removed"

clean-go:
	@echo "Cleaning Go build artifacts..."
	cd $(GO_MODULE) && go clean -testcache
	@echo "✓ Go artifacts cleaned"

# Development targets
.PHONY: test test-phase1 test-phase2 verify
test: ci
test-phase1: phase1
test-phase2: phase2
verify: ci
