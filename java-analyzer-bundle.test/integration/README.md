# Phase 2 Integration Tests

This directory contains **Phase 2 integration tests** that verify actual JDT.LS search results against real Java codebases.

## Overview

Unlike Phase 1 unit tests that only verify commands execute without errors, Phase 2 tests:

✅ **Start an actual JDT.LS server** with the Java Analyzer Bundle plugin
✅ **Load real Java projects** into the workspace
✅ **Execute searches** for all location types (0-14)
✅ **Verify SymbolInformation results** - names, kinds, locations
✅ **Test migration patterns** - javax→jakarta, legacy APIs

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  GitHub Actions Runner                  │
├─────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────┐    │
│  │           Podman Container                      │    │
│  │  ┌──────────────────────────────────────────┐      │    │
│  │  │   JDT.LS Server (1.38.0)             │      │    │
│  │  │   + Java Analyzer Bundle Plugin      │      │    │
│  │  └──────────────────────────────────────┘      │    │
│  │         ↕️ LSP over stdio                       │    │
│  │  ┌──────────────────────────────────────┐      │    │
│  │  │   Go LSP Client                      │      │    │
│  │  │   (client/jdtls_client.go)           │      │    │
│  │  │   konveyor/analyzer-lsp/jsonrpc2_v2  │      │    │
│  │  └──────────────────────────────────────┘      │    │
│  │         ↕️                                       │    │
│  │  ┌──────────────────────────────────────┐      │    │
│  │  │   Go Test Framework                  │      │    │
│  │  │   (integration_test.go)              │      │    │
│  │  └──────────────────────────────────────┘      │    │
│  │         ↓                                       │    │
│  │  ┌──────────────────────────────────────┐      │    │
│  │  │   Test Workspace                     │      │    │
│  │  │   - test-project/                    │      │    │
│  │  │   - customers-tomcat-legacy/         │      │    │
│  │  └──────────────────────────────────────┘      │    │
│  └────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

## Components

### 1. `client/jdtls_client.go`
Go LSP client that communicates with JDT.LS via JSON-RPC 2.0 over stdio using the `konveyor/analyzer-lsp/jsonrpc2_v2` package.

**Key Methods**:
- `Start()` - Launch JDT.LS process and create stdio pipes
- `Initialize()` - Initialize LSP connection and workspace
- `ExecuteCommand()` - Execute workspace commands
- `SearchSymbols()` - Execute analyzer searches via `io.konveyor.tackle.ruleEntry`
- `Shutdown()` - Gracefully shutdown server

**Protocol**: LSP (Language Server Protocol) over stdin/stdout using `jsonrpc2.Connection`

**Implementation**:
```go
// Create JSON-RPC 2.0 stream over stdio
stream := jsonrpc2.NewStream(stdout, stdin)

// Create connection with handler
c.conn = jsonrpc2.NewConnection(stream, jsonrpc2.HandlerFunc(c.handleMessage))

// Call LSP methods
if err := c.conn.Call(c.ctx, "initialize", params, &result); err != nil {
    return nil, fmt.Errorf("initialize request failed: %w", err)
}
```

### 2. `integration_test.go`
Go test file using the standard `testing` package with test functions for all location types.

**Test Functions**:
- `TestInheritanceSearch(t *testing.T)` - Location type 1
- `TestMethodCallSearch(t *testing.T)` - Location type 2
- `TestConstructorCallSearch(t *testing.T)` - Location type 3
- `TestAnnotationSearch(t *testing.T)` - Location type 4
- `TestImplementsTypeSearch(t *testing.T)` - Location type 5
- `TestImportSearch(t *testing.T)` - Location type 8
- `TestTypeSearch(t *testing.T)` - Location type 10
- `TestClassDeclarationSearch(t *testing.T)` - Location type 14
- `TestCustomersTomcatLegacy(t *testing.T)` - Real-world migration patterns

**Test Setup**:
```go
func TestMain(m *testing.M) {
    // Initialize JDT.LS client once for all tests
    jdtlsClient = client.NewJDTLSClient(jdtlsPath, workspaceDir)
    jdtlsClient.Start()
    jdtlsClient.Initialize()

    // Run tests
    code := m.Run()

    // Cleanup
    jdtlsClient.Close()
    os.Exit(code)
}
```

### 3. `test_helpers.go`
Helper functions for result verification.

**Key Functions**:
```go
func verifySymbolInResults(symbols []protocol.SymbolInformation, expectedName string,
    expectedKind ...protocol.SymbolKind) bool

func verifySymbolLocationContains(symbols []protocol.SymbolInformation,
    expectedName, expectedFile string) bool
```

### 4. Test Projects (in `../projects/`)
- **test-project**: Systematic test cases for all location types
- **customers-tomcat-legacy**: Real Spring MVC app with migration targets

## Running Tests

### Locally with Podman or Docker

The easiest way is to use the provided script (auto-detects Podman/Docker):

```bash
cd java-analyzer-bundle.test/integration
./run_local.sh
```

Or manually with **Podman** (preferred):

```bash
# Build the container image from repository root
cd /path/to/java-analyzer-bundle
podman build -t jdtls-analyzer:test .

# Run integration tests with go test
podman run --rm \
  -v $(pwd)/java-analyzer-bundle.test:/tests:Z \
  -e WORKSPACE_DIR=/tests/projects \
  -e JDTLS_PATH=/jdtls \
  --workdir /tests/integration \
  --entrypoint /bin/sh \
  jdtls-analyzer:test \
  -c "microdnf install -y golang && go mod download && go test -v"
```

Or with **Docker**:

```bash
# Build the Docker image
docker build -t jdtls-analyzer:test .

# Run integration tests with go test
docker run --rm \
  -v $(pwd)/java-analyzer-bundle.test:/tests \
  -e WORKSPACE_DIR=/tests/projects \
  -e JDTLS_PATH=/jdtls \
  --workdir /tests/integration \
  --entrypoint /bin/sh \
  jdtls-analyzer:test \
  -c "microdnf install -y golang && go mod download && go test -v"
```

**Note**: Podman uses `:Z` suffix on volumes for SELinux relabeling. Docker doesn't require this.

### Via GitHub Actions

Tests run automatically on:
- Push to `main` or `maven-index` branches
- Pull requests to `main`

Workflow: `.github/workflows/integration-tests.yml`

The workflow:
1. Runs Phase 1 unit tests with Maven
2. Builds the JDT.LS container image with Podman
3. Runs `go test -v` inside the container

### Manually (without containers)

Requires JDT.LS 1.38.0 and Go 1.21+ installed locally:

```bash
cd java-analyzer-bundle.test/integration

# Set environment variables
export JDTLS_PATH=/path/to/jdtls
export WORKSPACE_DIR=/path/to/java-analyzer-bundle.test/projects

# Run tests
go test -v
```

**Run specific tests:**
```bash
# Run a single test
go test -v -run TestInheritanceSearch

# Run tests matching a pattern
go test -v -run "TestMethod.*"
```

## Test Scenarios

### Inheritance (Location 1)
```go
// Search for classes extending BaseService
symbols, err := c.SearchSymbols(
    "test-project",
    "io.konveyor.demo.inheritance.BaseService",
    1,  // location type: inheritance
    "source-only",
    nil,
)

// Expected Results:
// ✓ SampleApplication (extends BaseService)
// ✓ DataService (extends BaseService)
```

### Method Calls (Location 2)
```go
// Search for println method calls
symbols, err := c.SearchSymbols(
    "test-project",
    "*.println",
    2,  // location type: method_call
    "source-only",
    nil,
)

// Expected Results:
// ✓ Multiple System.out.println calls in various classes
```

### Annotations (Location 4)
```go
// Search for JPA @Entity annotations
symbols, err := c.SearchSymbols(
    "customers-tomcat",
    "javax.persistence.Entity",
    4,  // location type: annotation
    "source-only",
    nil,
)

// Expected Results:
// ✓ Customer class with @Entity annotation
//
// MIGRATION TARGET: javax → jakarta namespace
```

### Import Searches (Location 8)
```go
// Search for javax.persistence imports
symbols, err := c.SearchSymbols(
    "customers-tomcat",
    "javax.persistence.*",
    8,  // location type: import
    "source-only",
    nil,
)

// Expected Results:
// ✓ Customer.java imports javax.persistence.*
//
// These imports flag code for jakarta migration!
```

## Test Assertions

Tests verify using helper functions from `tests/test_utils.go`:

1. **Symbol Existence**:
   ```go
   if VerifySymbolInResults(symbols, "SampleApplication") {
       results.AddPass("Test passed")
   } else {
       results.AddFail("Test failed", "SampleApplication not found")
   }
   ```

2. **Symbol Kind**:
   ```go
   if VerifySymbolInResults(symbols, "Customer", protocol.Class) {
       results.AddPass("Test passed")
   }
   ```

3. **Symbol Location**:
   ```go
   if VerifySymbolLocationContains(symbols, "Customer", "Customer.java") {
       results.AddPass("Test passed")
   }
   ```

4. **Result Count**:
   ```go
   if count := CountSymbols(symbols); count > 0 {
       results.AddPass(fmt.Sprintf("Found %d results", count))
   }
   ```

## Expected Output

```
============================================================
Phase 2 Integration Tests - JDT.LS Search Verification
============================================================

JDT.LS Path: /jdtls
Workspace: /tests/projects

Initializing JDT.LS client...
Started JDT.LS server with PID 1234
JDT.LS initialized successfully
JDT.LS ready for testing

--- Testing Inheritance Searches (Location 1) ---
✓ PASS: Inheritance: Find SampleApplication extends BaseService
✓ PASS: Inheritance: Find DataService extends BaseService
✓ PASS: Inheritance: Find CustomException extends Exception

--- Testing Method Call Searches (Location 2) ---
✓ PASS: Method Call: Find println calls (8 found)
✓ PASS: Method Call: Find List.add in SampleApplication

--- Testing Constructor Call Searches (Location 3) ---
✓ PASS: Constructor: Find ArrayList instantiations (3 found)
✓ PASS: Constructor: Find File instantiations (5 found)

--- Testing Annotation Searches (Location 4) ---
✓ PASS: Annotation: Find @CustomAnnotation on SampleApplication

--- Testing Implements Type Searches (Location 5) ---
✓ PASS: Implements: Find BaseService implements Serializable

--- Testing Import Searches (Location 8) ---
✓ PASS: Import: Find java.io.* imports (2 found)

--- Testing Type Searches (Location 10) ---
✓ PASS: Type: Find String type references (15 found)

--- Testing Class Declaration Searches (Location 14) ---
✓ PASS: Class Declaration: Find SampleApplication class

--- Testing customers-tomcat-legacy Project ---
✓ PASS: Legacy Project: Find @Entity on Customer
✓ PASS: Legacy Project: Find @Service on CustomerService
✓ PASS: Legacy Project: Find javax.persistence imports (9 found)

============================================================
Test Results: 15/15 passed
============================================================
```

## Troubleshooting

### JDT.LS Fails to Start

Check container logs:
```bash
# Podman
podman logs <container-id>

# Docker
docker logs <container-id>
```

Common issues:
- Insufficient memory (JDT.LS needs ~512MB)
- Missing Java 17
- Workspace directory not mounted
- SELinux blocking access (Podman) - use `:Z` on volumes

### No Search Results

Possible causes:
1. **Project not loaded**: JDT.LS needs time to index projects (5 second wait in Initialize())
2. **Build errors**: Check for compilation errors in test projects
3. **Plugin not loaded**: Verify analyzer bundle is in `/jdtls/` directory
4. **Wrong project name**: Must match pom.xml `<artifactId>`

### LSP Communication Errors

- Verify JSON-RPC message format
- Check Content-Length headers
- Enable JDT.LS verbose logging: Add `-verbose` to JDT.LS command
- Check Go logs for `jsonrpc2` errors

### Build Errors

```bash
# Verify Go dependencies
go mod download
go mod verify

# Check for compilation errors
go build -o jdtls-integration-tests .
```

## Adding New Tests

1. **Add test function** to `integration_test.go`:
   ```go
   func TestMyPattern(t *testing.T) {
       t.Run("Find MyClass", func(t *testing.T) {
           symbols, err := jdtlsClient.SearchSymbols(
               "test-project",
               "my.package.*",
               10,  // location type
               "source-only",
               nil,
           )

           if err != nil {
               t.Fatalf("Search failed: %v", err)
           }

           if !verifySymbolInResults(symbols, "MyClass") {
               t.Errorf("MyClass not found in results")
           }
       })
   }
   ```

2. **Add test data** to projects if needed

3. **Run the new test**:
   ```bash
   go test -v -run TestMyPattern
   ```

## Coverage Matrix

| Location | Description | Tested | Test Function |
|----------|-------------|--------|---------------|
| 0 | Default | ❌ | - |
| 1 | Inheritance | ✅ | `TestInheritanceSearch` |
| 2 | Method call | ✅ | `TestMethodCallSearch` |
| 3 | Constructor | ✅ | `TestConstructorCallSearch` |
| 4 | Annotation | ✅ | `TestAnnotationSearch` + `TestCustomersTomcatLegacy` |
| 5 | Implements | ✅ | `TestImplementsTypeSearch` |
| 6 | Enum constant | ❌ | - |
| 7 | Return type | ❌ | - |
| 8 | Import | ✅ | `TestImportSearch` + `TestCustomersTomcatLegacy` |
| 9 | Variable decl | ❌ | - |
| 10 | Type | ✅ | `TestTypeSearch` |
| 11 | Package | ❌ | - |
| 12 | Field | ❌ | - |
| 13 | Method decl | ❌ | - |
| 14 | Class decl | ✅ | `TestClassDeclarationSearch` |

**Current Coverage**: 8/15 location types (53%)

## Migration Testing

The customers-tomcat-legacy project tests real migration scenarios:

### javax → jakarta Migration
```go
// Find all javax.persistence usage
symbols, err := c.SearchSymbols(
    "customers-tomcat",
    "javax.persistence.*",
    8,  // import location type
    "source-only",
    nil,
)

// These hits indicate code that needs jakarta migration
```

### Spring Framework Patterns
```go
// Find @Autowired usage
symbols, err := c.SearchSymbols(
    "customers-tomcat",
    "org.springframework.beans.factory.annotation.Autowired",
    4,  // annotation location type
    "source-only",
    nil,
)

// @Service usage
symbols, err = c.SearchSymbols(
    "customers-tomcat",
    "org.springframework.stereotype.Service",
    4,
    "source-only",
    nil,
)
```

### JBoss Logging → SLF4J
```go
// Find JBoss Logger usage
symbols, err := c.SearchSymbols(
    "customers-tomcat",
    "org.jboss.logging.Logger",
    10,  // type location
    "source-only",
    nil,
)

// These can be flagged for SLF4J migration
```

## Dependencies

The integration tests use the following Go packages:

```go
require (
    github.com/konveyor/analyzer-lsp v0.4.0-alpha.1
    github.com/sirupsen/logrus v1.9.3
)
```

**Key packages from analyzer-lsp**:
- `github.com/konveyor/analyzer-lsp/jsonrpc2_v2` - JSON-RPC 2.0 implementation (imported as `jsonrpc2`)
- `github.com/konveyor/analyzer-lsp/lsp/protocol` - LSP protocol types (ExecuteCommandParams, SymbolInformation, etc.)

## Future Enhancements

- [ ] Add tests for remaining location types (0, 6, 7, 9, 11, 12, 13)
- [ ] Test with annotation query elements
- [ ] Test complex regex patterns with OR expressions
- [ ] Test included paths filtering
- [ ] Test source-only vs full analysis modes
- [ ] Performance benchmarking
- [ ] Parallel test execution
- [ ] Test result JSON export
- [ ] Integration with coverage reporting

## References

- [LSP Specification](https://microsoft.github.io/language-server-protocol/)
- [JDT.LS Documentation](https://github.com/eclipse/eclipse.jdt.ls)
- [Konveyor Analyzer LSP](https://github.com/konveyor/analyzer-lsp)
- [Java Analyzer Bundle](../../README.md)
- [Test Projects Documentation](../projects/README.md)
