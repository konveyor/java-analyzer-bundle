# Java Analyzer Bundle - Test Module

This module contains comprehensive tests for the Java Analyzer Bundle, an Eclipse JDT.LS extension that provides Java code analysis capabilities for Konveyor/MTA.

## Quick Start

### Run All Tests

```bash
# From repository root
make ci         # Full CI pipeline (Phase 1 + Phase 2)
make phase1     # Maven unit tests only
make phase2     # Integration tests only
```

### Run Specific Tests

```bash
# Maven unit tests (48 tests)
mvn clean integration-test

# Go integration tests (specific test)
cd integration
go test -v -run TestInheritanceSearch
```

## Test Structure

This module contains two phases of testing:

### Phase 1: Unit Tests (Maven/JUnit)
**Location**: `src/main/java/`
**Technology**: JUnit-Plugin (runs in Eclipse environment)
**Count**: 48 tests

Tests command handling, parameter parsing, and error cases:
- `RuleEntryParamsTest` (14 tests)
- `AnnotationQueryTest` (18 tests)
- `SampleDelegateCommandHandlerTest` (16 tests)

**Run**: `mvn clean integration-test`

---

### Phase 2: Integration Tests (Go)
**Location**: `integration/`
**Technology**: Go with real JDT.LS server
**Count**: 18 test functions covering 15 location types + Priority 1 features

Tests actual search results against real Java codebases:
- Real JDT.LS server with analyzer plugin
- Actual workspace with test projects
- Symbol result verification
- Migration pattern testing
- Advanced features: annotated element matching, file path filtering

**Run**: `make phase2` or `cd integration && go test -v`

**✨ New in 2025-10-28**: Priority 1 advanced features
- **Annotated element matching**: 4 tests for searching annotations with specific attribute values
- **File path filtering**: 2 tests for restricting searches to specific directories

---

## Test Projects

**Location**: `projects/`

Two test projects provide Java code for integration testing:

### test-project
Systematic patterns for all 15 location types + advanced features:
- Inheritance, method calls, constructors
- Annotations, imports, type references
- Enums, fields, variables, return types
- **Advanced**: Annotated element matching (JMS, JPA, DataSource configs)
- **Advanced**: File path filtering (mixed JPA/JDBC anti-patterns)

### customers-tomcat-legacy
Real-world Spring MVC application:
- JPA entities with `javax.persistence.*` (migration target)
- Spring components (`@Service`, `@Autowired`)
- Repository patterns, REST controllers
- JBoss Logging (potential migration target)

---

## Documentation

**Location**: `docs/`

📖 **[Documentation Index](docs/README.md)** - Start here!

### Quick Links

- **[Integration Tests Guide](docs/integration-tests.md)** - Architecture, running tests, troubleshooting
- **[Query Reference](docs/query-reference.md)** - All search queries and patterns tested
- **[Quick Reference](docs/quick-reference.md)** - Coverage summary and test commands
- **[Test Projects](docs/test-projects.md)** - Java test project details

---

## Test Coverage Summary

### Location Types: 15/15 Tested (100%) ✅

| Location | Type | Status |
|----------|------|--------|
| 0 | Default (all locations) | ✅ |
| 1 | Inheritance | ✅ |
| 2 | Method Calls | ✅ |
| 3 | Constructors | ✅ |
| 4 | Annotations | ✅ |
| 5 | Implements | ✅ |
| 6 | Enum Constants | ✅ |
| 7 | Return Types | ✅ |
| 8 | Imports | ✅ |
| 9 | Variable Declarations | ✅ |
| 10 | Type References | ✅ |
| 11 | Package Declarations | ✅ |
| 12 | Field Declarations | ✅ |
| 13 | Method Declarations | ✅ |
| 14 | Class Declarations | ✅ |

**🎉 COMPLETE COVERAGE ACHIEVED!**

**See**: [Quick Reference](docs/quick-reference.md) for details

---

## Key Technologies

- **Java 17** - Target platform and tests
- **Eclipse Tycho 3.0.1** - Build system
- **JDT.LS 1.35.0** - Language server
- **Go 1.21+** - Integration test framework
- **Podman/Docker** - Container runtime for CI/CD
- **JUnit 4** - Unit test framework

---

## Directory Structure

```
java-analyzer-bundle.test/
├── README.md                    # This file
├── docs/                        # 📖 Documentation
│   ├── README.md               # Documentation index
│   ├── integration-tests.md    # Integration test guide
│   ├── query-reference.md      # Search query catalog
│   ├── quick-reference.md      # Quick reference
│   └── test-projects.md        # Test projects overview
│
├── src/main/java/              # Phase 1: Maven/JUnit unit tests
│   └── io/konveyor/tackle/...
│
├── integration/                 # Phase 2: Go integration tests
│   ├── client/                 # JDT.LS LSP client
│   ├── integration_test.go     # Test functions
│   ├── test_helpers.go         # Verification helpers
│   ├── go.mod                  # Go dependencies
│   └── run_local.sh            # Local test runner
│
└── projects/                    # Java test projects
    ├── test-project/           # Systematic patterns
    └── customers-tomcat-legacy/ # Real-world migration
```

---

## Maven Configuration

**Packaging**: `eclipse-test-plugin`
**Parent**: `java-analyzer-bundle` (root pom.xml)

**Test Execution**:
```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-surefire-plugin</artifactId>
  <configuration>
    <useUIHarness>true</useUIHarness>
    <useUIThread>false</useUIThread>
  </configuration>
</plugin>
```

---

## CI/CD Pipeline

**GitHub Actions**: `.github/workflows/integration-tests.yml`

**Two-Phase Execution**:
1. **Phase 1**: Maven unit tests (`mvn clean integration-test`)
2. **Phase 2**: Build container → Go integration tests

**Triggers**:
- Push to `main` or `maven-index` branches
- Pull requests to `main`

**Local Verification**:
```bash
make ci  # Runs same steps as GitHub Actions
```

---

## Adding New Tests

### Unit Tests (Phase 1)
1. Create test class in `src/main/java/`
2. Extend appropriate test base
3. Use `@Test` annotation
4. Run: `mvn clean integration-test`

### Integration Tests (Phase 2)
1. Add test function to `integration/integration_test.go`
2. Use `jdtlsClient.SearchSymbols()` for queries
3. Verify results with helper functions
4. Run: `go test -v -run TestYourTest`

See: [Integration Tests Guide](docs/integration-tests.md#adding-new-tests)

---

## Common Tasks

### View Test Coverage
```bash
mvn clean verify
# Coverage report: target/site/jacoco/index.html
```

### Debug Integration Tests
```bash
cd integration
export JDTLS_PATH=/path/to/jdtls
export WORKSPACE_DIR=/path/to/projects
go test -v -run TestSpecificTest
```

### Build Test Container
```bash
# From repository root
podman build -t jdtls-analyzer:test .
```

---

## Migration Testing

The integration tests verify detection of common migration patterns:

### javax → jakarta
```go
// Find javax.persistence imports (location 8)
symbols, _ := client.SearchSymbols("customers-tomcat",
    "javax.persistence.Entity", 8, "source-only", nil)
// Flags files needing jakarta migration
```

### Spring Framework
```go
// Find @Service annotations (location 4)
symbols, _ := client.SearchSymbols("customers-tomcat",
    "org.springframework.stereotype.Service", 4, "source-only", nil)
```

See: [Query Reference](docs/query-reference.md#migration-query-catalog)

---

## Resources

- **[Main Project README](../README.md)** - Core analyzer architecture
- **[CLAUDE.md](../CLAUDE.md)** - Development guidance
- **[Integration Tests](docs/integration-tests.md)** - Detailed test documentation
- **[Konveyor Analyzer LSP](https://github.com/konveyor/analyzer-lsp)** - LSP protocol implementation
- **[JDT.LS](https://github.com/eclipse/eclipse.jdt.ls)** - Eclipse Java language server

---

## Support

For issues or questions:
- Check [Integration Tests Guide](docs/integration-tests.md#troubleshooting)
- Review [Test Projects](docs/test-projects.md)
- See main project documentation

---

**Module**: `java-analyzer-bundle.test`
**Type**: Eclipse Test Plugin
**Java Version**: 17
**Test Framework**: JUnit 4 (unit) + Go (integration)
