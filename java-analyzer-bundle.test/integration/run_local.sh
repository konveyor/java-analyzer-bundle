#!/bin/bash
# Script to run Phase 2 integration tests locally using Docker or Podman

set -e

echo "=========================================="
echo "Phase 2 Integration Tests - Local Run"
echo "=========================================="

# Detect container runtime (prefer Podman, fall back to Docker)
if command -v podman &> /dev/null; then
    CONTAINER_RUNTIME="podman"
    VOLUME_FLAGS=":Z"  # SELinux relabeling for Podman
    echo "Using Podman"
elif command -v docker &> /dev/null; then
    CONTAINER_RUNTIME="docker"
    VOLUME_FLAGS=""
    echo "Using Docker"
else
    echo "ERROR: Neither Podman nor Docker is installed or in PATH"
    exit 1
fi

# Get the repository root
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
echo "Repository root: $REPO_ROOT"

# Build the container image
echo ""
echo "Building JDT.LS container image..."
cd "$REPO_ROOT"
$CONTAINER_RUNTIME build -t jdtls-analyzer:test -f Dockerfile.test .

# Run the integration tests
echo ""
echo "Running Phase 2 integration tests with go test..."
$CONTAINER_RUNTIME run --rm \
  -v "$REPO_ROOT/java-analyzer-bundle.test:/tests${VOLUME_FLAGS}" \
  -e WORKSPACE_DIR=/tests/projects \
  -e JDTLS_PATH=/jdtls \
  --workdir /tests/integration \
  --entrypoint /bin/sh \
  jdtls-analyzer:test \
  -c "go test -v"

TEST_EXIT_CODE=$?

echo ""
echo "=========================================="
if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo "✓ All tests passed!"
else
    echo "✗ Some tests failed (exit code: $TEST_EXIT_CODE)"
fi
echo "=========================================="

exit $TEST_EXIT_CODE
