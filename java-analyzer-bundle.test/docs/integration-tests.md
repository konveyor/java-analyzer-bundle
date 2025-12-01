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

**Test Functions** (18 total):
- `TestDefaultSearch(t *testing.T)` - Location type 0 (default)
- `TestInheritanceSearch(t *testing.T)` - Location type 1
- `TestMethodCallSearch(t *testing.T)` - Location type 2
- `TestConstructorCallSearch(t *testing.T)` - Location type 3
- `TestAnnotationSearch(t *testing.T)` - Location type 4
- `TestImplementsTypeSearch(t *testing.T)` - Location type 5
- `TestEnumConstantSearch(t *testing.T)` - Location type 6
- `TestReturnTypeSearch(t *testing.T)` - Location type 7
- `TestImportSearch(t *testing.T)` - Location type 8
- `TestVariableDeclarationSearch(t *testing.T)` - Location type 9
- `TestTypeSearch(t *testing.T)` - Location type 10
- `TestPackageDeclarationSearch(t *testing.T)` - Location type 11
- `TestFieldDeclarationSearch(t *testing.T)` - Location type 12
- `TestMethodDeclarationSearch(t *testing.T)` - Location type 13
- `TestClassDeclarationSearch(t *testing.T)` - Location type 14
- `TestCustomersTomcatLegacy(t *testing.T)` - Real-world migration patterns
- `TestAnnotatedElementMatching(t *testing.T)` - Priority 1: Annotation element matching
- `TestFilePathFiltering(t *testing.T)` - Priority 1: File path filtering

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

### Default Search (Location 0)
```go
// Search for all List usage across all location types
symbols, err := c.SearchSymbols(
    "test-project",
    "java.util.List",
    0,  // location type: default (all locations)
    "source-only",
    nil,
)

// Expected Results:
// ✓ Finds List in imports, fields, variables, type references, etc.
```

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

### Enum Constants (Location 6)
```go
// Search for enum constant usage
symbols, err := c.SearchSymbols(
    "test-project",
    "io.konveyor.demo.EnumExample.ACTIVE",
    6,  // location type: enum_constant
    "source-only",
    nil,
)

// Expected Results:
// ✓ Finds ACTIVE enum constant references
```

### Return Types (Location 7)
```go
// Search for methods returning String
symbols, err := c.SearchSymbols(
    "test-project",
    "java.lang.String",
    7,  // location type: return_type
    "source-only",
    nil,
)

// Expected Results:
// ✓ Finds getName() and other methods returning String
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

### Variable Declarations (Location 9)
```go
// Search for String variable declarations
symbols, err := c.SearchSymbols(
    "test-project",
    "java.lang.String",
    9,  // location type: variable_declaration
    "source-only",
    nil,
)

// Expected Results:
// ✓ Finds local String variable declarations in method bodies
```

### Package Declarations (Location 11)
```go
// Search for package declarations
symbols, err := c.SearchSymbols(
    "test-project",
    "io.konveyor.demo",
    11,  // location type: package
    "source-only",
    nil,
)

// Expected Results:
// ✓ Finds all files with package io.konveyor.demo
```

### Field Declarations (Location 12)
```go
// Search for String field declarations
symbols, err := c.SearchSymbols(
    "test-project",
    "java.lang.String",
    12,  // location type: field
    "source-only",
    nil,
)

// Expected Results:
// ✓ Finds applicationName and other String fields
```

### Method Declarations (Location 13)
```go
// Search for method declarations with wildcard
symbols, err := c.SearchSymbols(
    "test-project",
    "get*",
    13,  // location type: method_declaration
    "source-only",
    nil,
)

// Expected Results:
// ✓ Finds getName(), getItems(), and other getter methods
```

### Priority 1: Annotated Element Matching
```go
// Search for annotations with specific attribute values
annotationQuery := &client.AnnotationQuery{
    Pattern: "javax.annotation.sql.DataSourceDefinition",
    Elements: []client.AnnotationElement{
        {Name: "className", Value: "org.postgresql.Driver"},
    },
}

symbols, err := c.SearchSymbolsWithAnnotation(
    "test-project",
    "javax.annotation.sql.DataSourceDefinition",
    4,  // location type: annotation
    "source-only",
    nil,
    annotationQuery,
)

// Expected Results:
// ✓ Finds @DataSourceDefinition with PostgreSQL driver class
// ✓ Useful for detecting database driver dependencies for migration
```

### Priority 1: File Path Filtering
```go
// Search with file path filtering
includedPaths := []string{
    "src/main/java/io/konveyor/demo/persistence",
}

symbols, err := c.SearchSymbols(
    "test-project",
    "java.sql.PreparedStatement",
    8,  // location type: import
    "source-only",
    &includedPaths,
)

// Expected Results:
// ✓ Finds PreparedStatement imports only in persistence package
// ✓ Excludes matches from other packages
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

--- Testing Default Searches (Location 0) ---
✓ PASS: Default: Find all List usage across all location types

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

--- Testing Enum Constant Searches (Location 6) ---
✓ PASS: Enum Constant: Find ACTIVE enum constant references

--- Testing Return Type Searches (Location 7) ---
✓ PASS: Return Type: Find methods returning String

--- Testing Import Searches (Location 8) ---
✓ PASS: Import: Find java.io.* imports (2 found)

--- Testing Variable Declaration Searches (Location 9) ---
✓ PASS: Variable Declaration: Find String variable declarations

--- Testing Type Searches (Location 10) ---
✓ PASS: Type: Find String type references (15 found)

--- Testing Package Declaration Searches (Location 11) ---
✓ PASS: Package Declaration: Find io.konveyor.demo package

--- Testing Field Declaration Searches (Location 12) ---
✓ PASS: Field Declaration: Find String fields

--- Testing Method Declaration Searches (Location 13) ---
✓ PASS: Method Declaration: Find getter methods with wildcard

--- Testing Class Declaration Searches (Location 14) ---
✓ PASS: Class Declaration: Find SampleApplication class

--- Testing customers-tomcat-legacy Project ---
✓ PASS: Legacy Project: Find @Entity on Customer
✓ PASS: Legacy Project: Find @Service on CustomerService
✓ PASS: Legacy Project: Find javax.persistence imports (9 found)

--- Testing Priority 1 Advanced Features ---
✓ PASS: Annotated Element Matching: Find @ActivationConfigProperty with propertyName=destinationLookup
✓ PASS: Annotated Element Matching: Find @DataSourceDefinition with PostgreSQL driver
✓ PASS: Annotated Element Matching: Find @DataSourceDefinition with MySQL driver
✓ PASS: Annotated Element Matching: Find @Column with nullable=false
✓ PASS: File Path Filtering: Find PreparedStatement imports in persistence package
✓ PASS: File Path Filtering: Verify filtering excludes other packages

============================================================
Test Results: 18/18 passed (100%)
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

## Test Coverage by Java Features

### All Location Types Tested (15/15 - 100%)

#### ✅ Location 1: Inheritance
**Java Features**: Class extends another class
**Test**: `TestInheritanceSearch`
**Queries**:
- `io.konveyor.demo.inheritance.BaseService` → finds `SampleApplication`, `DataService` (custom subclasses)
- `java.lang.Exception` → finds `CustomException` (JDK subclass)

**Java Code Pattern**:
```java
public class SampleApplication extends BaseService { }
```

**Symbol Results**: Class declarations that extend the queried class

---

#### ✅ Location 2: Method Calls
**Java Features**: Method invocations on objects/classes
**Test**: `TestMethodCallSearch`
**Queries**:
- `println(*)` → finds all `System.out.println()` calls (wildcard parameter matching)
- `add(*)` → finds `List.add()` calls in `processData` method

**Java Code Pattern**:
```java
System.out.println("Processing: " + tempData);  // println method call
items.add(tempData);                             // add method call
```

**Symbol Results**: Method invocation locations with method name and containing method

---

#### ✅ Location 3: Constructor Calls
**Java Features**: Object instantiation with `new` keyword
**Test**: `TestConstructorCallSearch`
**Queries**:
- `java.util.ArrayList` → finds all `new ArrayList<>()` instantiations
- `java.io.File` → finds all `new File(...)` instantiations

**Java Code Pattern**:
```java
this.items = new ArrayList<>();           // ArrayList constructor
File tempFile = new File("/tmp/data.txt"); // File constructor
```

**Symbol Results**: Constructor call locations

---

#### ✅ Location 4: Annotations
**Java Features**: Java annotations on classes, methods, fields
**Test**: `TestAnnotationSearch`, `TestCustomersTomcatLegacy`
**Queries**:
- `io.konveyor.demo.annotations.CustomAnnotation` → finds `@CustomAnnotation` on `SampleApplication`
- `javax.persistence.Entity` → finds `@Entity` on `Customer` (migration target)
- `org.springframework.stereotype.Service` → finds `@Service` on `CustomerService`

**Java Code Pattern**:
```java
@CustomAnnotation(value = "SampleApp", version = "1.0")
public class SampleApplication extends BaseService { }

@Entity
@Table(name = "customers")
public class Customer { }

@Service
@Transactional
public class CustomerService implements ICustomerService { }
```

**Symbol Results**: Annotation name with container showing the annotated element

---

#### ✅ Location 5: Implements Type
**Java Features**: Class/interface implements interface(s)
**Test**: `TestImplementsTypeSearch`
**Queries**:
- `java.io.Serializable` → finds `BaseService` (which implements Serializable)

**Java Code Pattern**:
```java
public abstract class BaseService implements Serializable, Comparable<BaseService> { }
```

**Symbol Results**: Classes implementing the queried interface

---

#### ✅ Location 8: Imports
**Java Features**: Import statements for classes/packages
**Test**: `TestImportSearch`, `TestCustomersTomcatLegacy`
**Queries**:
- `java.io.File` → finds import statements in files using File class
- `javax.persistence.Entity` → finds javax.persistence imports (migration target)

**Java Code Pattern**:
```java
import java.io.File;
import java.io.FileWriter;
import javax.persistence.Entity;
import javax.persistence.Table;
```

**Symbol Results**: Import statement locations

**Migration Use Case**: Finding legacy `javax.*` imports to replace with `jakarta.*`

---

#### ✅ Location 10: Type References
**Java Features**: Type usage in variable declarations, parameters, return types, generic types
**Test**: `TestTypeSearch`
**Queries**:
- `java.util.ArrayList` → finds all ArrayList type references (fields, variables, return types)

**Java Code Pattern**:
```java
private List<String> items;                    // Generic type reference
List<String> results = new ArrayList<>();      // Variable declaration type
```

**Symbol Results**: Locations where the type is referenced

---

#### ✅ Location 14: Class Declarations
**Java Features**: Class definition (class, interface, enum, annotation)
**Test**: `TestClassDeclarationSearch`
**Queries**:
- `SampleApplication` → finds the class declaration itself

**Java Code Pattern**:
```java
public class SampleApplication extends BaseService { }
```

**Symbol Results**: The class declaration symbol

---

#### ✅ Location 0: Default
**Java Features**: Default search behavior (all locations)
**Test**: `TestDefaultSearch`
**Queries**:
- `java.util.List` → finds all List usage across all location types

**Java Code Pattern**:
```java
// Matches in multiple contexts: imports, fields, variables, types, etc.
import java.util.List;
private List<String> items;
```

**Symbol Results**: All occurrences across all location types

---

#### ✅ Location 6: Enum Constants
**Java Features**: Enum constant declarations and references
**Test**: `TestEnumConstantSearch`
**Queries**:
- `io.konveyor.demo.EnumExample.ACTIVE` → finds ACTIVE constant usage
- `io.konveyor.demo.EnumExample.*` → finds all enum constant references

**Java Code Pattern** (test-project/EnumExample.java):
```java
public enum EnumExample {
    ACTIVE("active", 1),
    INACTIVE("inactive", 0),
    PENDING("pending", 2),
    ARCHIVED("archived", -1);
}

// Usage
EnumExample status = EnumExample.ACTIVE;
```

**Symbol Results**: Enum constant references

---

#### ✅ Location 7: Return Types
**Java Features**: Method return type declarations
**Test**: `TestReturnTypeSearch`
**Queries**:
- `java.lang.String` → finds all methods returning String
- `java.util.List` → finds all methods returning List

**Java Code Pattern** (test-project/SampleApplication.java):
```java
public String getName() {              // String return type
    return applicationName;
}

public List<String> getItems() {       // List return type
    return items;
}
```

**Symbol Results**: Methods with specified return types

---

#### ✅ Location 9: Variable Declarations
**Java Features**: Local variable declarations in method bodies
**Test**: `TestVariableDeclarationSearch`
**Queries**:
- `java.lang.String` → finds String variable declarations
- `java.io.File` → finds File variable declarations

**Java Code Pattern** (test-project/SampleApplication.java):
```java
public void processData() {
    String tempData = "temporary";     // String variable
    int count = 0;                     // int variable
    File tempFile = new File("/tmp");  // File variable
}
```

**Symbol Results**: Local variable declaration locations

---

#### ✅ Location 11: Package Declarations
**Java Features**: Package statements at top of Java files
**Test**: `TestPackageDeclarationSearch`
**Queries**:
- `io.konveyor.demo` → finds all classes in this package
- `io.konveyor.demo.*` → finds all classes in this package and subpackages

**Java Code Pattern** (all test files):
```java
package io.konveyor.demo;
package io.konveyor.demo.ordermanagement.model;
```

**Symbol Results**: Package declaration locations

---

#### ✅ Location 12: Field Declarations
**Java Features**: Class-level field/member variable declarations
**Test**: `TestFieldDeclarationSearch`
**Queries**:
- `java.lang.String` → finds String fields
- `java.util.List` → finds List fields

**Java Code Pattern** (test-project/SampleApplication.java):
```java
private String applicationName;        // String field
private List<String> items;            // List field
private File configFile;               // File field
private static final int MAX_RETRIES = 3;  // static final field
```

**Symbol Results**: Field declaration locations (both static and instance)

---

#### ✅ Location 13: Method Declarations
**Java Features**: Method signature declarations
**Test**: `TestMethodDeclarationSearch`
**Queries**:
- `processData` → finds processData method declarations
- `get*` → finds all getter methods (wildcard)
- `print*` → finds all methods starting with print

**Java Code Pattern** (test-project/SampleApplication.java):
```java
public void processData() { }          // processData method
public String getName() { }            // getName method
public static void printVersion() { } // static method
```

**Symbol Results**: Method declaration locations

---

## Coverage Matrix

| Location | Description | Tested | Test Function |
|----------|-------------|--------|---------------|
| 0 | Default | ✅ | `TestDefaultSearch` |
| 1 | Inheritance | ✅ | `TestInheritanceSearch` |
| 2 | Method call | ✅ | `TestMethodCallSearch` |
| 3 | Constructor | ✅ | `TestConstructorCallSearch` |
| 4 | Annotation | ✅ | `TestAnnotationSearch` + `TestCustomersTomcatLegacy` |
| 5 | Implements | ✅ | `TestImplementsTypeSearch` |
| 6 | Enum constant | ✅ | `TestEnumConstantSearch` |
| 7 | Return type | ✅ | `TestReturnTypeSearch` |
| 8 | Import | ✅ | `TestImportSearch` + `TestCustomersTomcatLegacy` |
| 9 | Variable decl | ✅ | `TestVariableDeclarationSearch` |
| 10 | Type | ✅ | `TestTypeSearch` |
| 11 | Package | ✅ | `TestPackageDeclarationSearch` |
| 12 | Field | ✅ | `TestFieldDeclarationSearch` |
| 13 | Method decl | ✅ | `TestMethodDeclarationSearch` |
| 14 | Class decl | ✅ | `TestClassDeclarationSearch` |

**Current Coverage**: 15/15 location types (100%) 🎉

**Priority 1 Advanced Features**:
- ✅ Annotated Element Matching - `TestAnnotatedElementMatching` (4 tests)
- ✅ File Path Filtering - `TestFilePathFiltering` (2 tests)

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

**✅ Completed**:
- ✅ All location types tested (0-14) - 100% coverage achieved!
- ✅ Annotation query elements tested (Priority 1 feature)
- ✅ File path filtering tested (Priority 1 feature)
- ✅ Source-only analysis mode tested

**Potential Future Work**:
- [ ] Test complex regex patterns with OR expressions
- [ ] Test full analysis mode (vs source-only)
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
