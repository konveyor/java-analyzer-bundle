# Test Documentation Index

Welcome to the Java Analyzer Bundle test documentation. This directory contains comprehensive guides for understanding, running, and extending the test suite.

## 📚 Documentation Overview

### 🚀 [Quick Reference](quick-reference.md)
**Best for**: Getting started, quick lookups, command reference

A condensed guide with:
- Coverage summary (15/15 location types - 100% ✅)
- Test execution commands
- Expected output examples
- Key test patterns and wildcards
- Migration use cases tested
- Symbol information structure

**Start here if you want to**: Run tests quickly or check what's covered

---

### 🔍 [Query Reference](query-reference.md)
**Best for**: Understanding search queries, adding new tests, migration patterns

Comprehensive catalog of all tested search queries:
- All 15 search query patterns with examples
- Java code patterns for each location type
- Symbol information returned for each query
- Migration query catalog (javax→jakarta, Spring, JBoss→SLF4J)
- Wildcard pattern features
- Untested query patterns (ready to implement)

**Start here if you want to**: Understand what queries are tested or add new search patterns

---

### 🏗️ [Integration Tests Guide](integration-tests.md)
**Best for**: Deep technical understanding, architecture, troubleshooting

Complete integration test documentation:
- Architecture (JDT.LS server, LSP client, test framework)
- Component details (Go LSP client, test framework, helpers)
- Running tests (locally, containers, CI/CD)
- Test scenarios for all 15 location types (100% coverage)
- Test assertions and verification
- Troubleshooting guide
- Adding new tests

**Start here if you want to**: Understand the test infrastructure or debug issues

---

### 📦 [Test Projects](test-projects.md)
**Best for**: Understanding test data, Java code patterns

Detailed overview of the Java test projects:
- `test-project`: Systematic patterns for all 15 location types
- `customers-tomcat-legacy`: Real-world Spring MVC migration scenarios
- Package structures and key files
- Covered patterns per project
- Test scenario examples
- Migration targets (javax, Spring, JBoss)

**Start here if you want to**: Understand the test Java code or add new test patterns

---

## 🎯 Quick Navigation by Task

### I want to run tests
→ [Quick Reference - Running Tests](quick-reference.md#running-tests)

### I want to understand what's tested
→ [Quick Reference - Coverage Summary](quick-reference.md#coverage-at-a-glance)
→ [Query Reference - Test Coverage](query-reference.md#tested-search-queries)

### I want to add a new test
→ [Integration Tests Guide - Adding New Tests](integration-tests.md#adding-new-tests)
→ [Query Reference - All Tested Patterns](query-reference.md#tested-search-queries)

### I want to understand the architecture
→ [Integration Tests Guide - Architecture](integration-tests.md#architecture)
→ [Integration Tests Guide - Components](integration-tests.md#components)

### I want to understand test projects
→ [Test Projects - Overview](test-projects.md#projects-overview)
→ [Test Projects - Coverage Matrix](test-projects.md#coverage-matrix)

### I want to add Java test code
→ [Test Projects - Adding New Test Projects](test-projects.md#adding-new-test-projects)

### I'm debugging a failing test
→ [Integration Tests Guide - Troubleshooting](integration-tests.md#troubleshooting)

### I want to understand migration testing
→ [Query Reference - Migration Query Catalog](query-reference.md#migration-query-catalog)
→ [Test Projects - Legacy API Patterns](test-projects.md#legacy-api-patterns-migration-targets)

---

## 📊 Test Coverage Summary

### Location Types Tested: 15/15 (100%) ✅ COMPLETE!

**All Location Types Tested** ✅:
- Location 0: Default (searches all locations)
- Location 1: Inheritance (extends)
- Location 2: Method Calls
- Location 3: Constructor Calls
- Location 4: Annotations
- Location 5: Implements (interfaces)
- Location 6: Enum Constants
- Location 7: Return Types
- Location 8: Imports
- Location 9: Variable Declarations
- Location 10: Type References
- Location 11: Package Declarations
- Location 12: Field Declarations
- Location 13: Method Declarations
- Location 14: Class Declarations

**🎉 100% Coverage Achieved!**

---

## 🧪 Test Structure

### Phase 1: Maven Unit Tests (48 tests)
**Location**: `../src/main/java/`
**Framework**: JUnit-Plugin
**Coverage**: Command handling, parameter parsing, error cases

**Tests**:
- `RuleEntryParamsTest` (14 tests)
- `AnnotationQueryTest` (18 tests)
- `SampleDelegateCommandHandlerTest` (16 tests)

**Run**: `mvn clean integration-test`

---

### Phase 2: Go Integration Tests (18 functions)
**Location**: `../integration/`
**Framework**: Go test with real JDT.LS server
**Coverage**: Actual search results, symbol verification, migration patterns

**Tests**:
- `TestDefaultSearch` (location 0)
- `TestInheritanceSearch` (location 1)
- `TestMethodCallSearch` (location 2)
- `TestConstructorCallSearch` (location 3)
- `TestAnnotationSearch` (location 4)
- `TestImplementsTypeSearch` (location 5)
- `TestEnumConstantSearch` (location 6)
- `TestReturnTypeSearch` (location 7)
- `TestImportSearch` (location 8)
- `TestVariableDeclarationSearch` (location 9)
- `TestTypeSearch` (location 10)
- `TestPackageDeclarationSearch` (location 11)
- `TestFieldDeclarationSearch` (location 12)
- `TestMethodDeclarationSearch` (location 13)
- `TestClassDeclarationSearch` (location 14)
- `TestCustomersTomcatLegacy` (migration patterns)
- `TestAnnotatedElementMatching` (Priority 1: annotation attributes)
- `TestFilePathFiltering` (Priority 1: file path filtering)

**Run**: `make phase2` or `cd ../integration && go test -v`

---

## 🏗️ Test Projects

### test-project
**Purpose**: Systematic coverage of all location types
**Files**: 8 Java files covering all 15 location types
**Key**: `SampleApplication.java` - main test file with comprehensive patterns

### customers-tomcat-legacy
**Purpose**: Real-world migration scenario testing
**Framework**: Spring MVC + JPA + Hibernate
**Key**: Tests javax→jakarta migration, Spring patterns, JBoss Logging

See: [Test Projects Guide](test-projects.md)

---

## 🔧 Common Commands

```bash
# Run all tests (CI pipeline)
make ci

# Run Phase 1 only (Maven unit tests)
make phase1

# Run Phase 2 only (integration tests)
make phase2

# Run specific integration test
cd ../integration
go test -v -run TestInheritanceSearch

# Build test container
podman build -t jdtls-analyzer:test ..

# Run tests in container
cd ../integration
./run_local.sh
```

---

## 📖 Document Relationships

```
README.md (you are here)
    ├─ Provides overview and navigation
    │
    ├─► quick-reference.md
    │   ├─ Coverage summary
    │   ├─ Quick commands
    │   └─ Symbol structure
    │
    ├─► query-reference.md
    │   ├─ All queries tested
    │   ├─ Java patterns
    │   ├─ Migration catalog
    │   └─ Untested patterns
    │
    ├─► integration-tests.md
    │   ├─ Architecture details
    │   ├─ Component breakdown
    │   ├─ Running tests
    │   ├─ Test scenarios
    │   └─ Troubleshooting
    │
    └─► test-projects.md
        ├─ Project structures
        ├─ Code patterns
        ├─ Test scenarios
        └─ Coverage matrix
```

---

## 🎓 Learning Path

### Beginner: Just Running Tests
1. Read [Quick Reference](quick-reference.md)
2. Run `make ci` from repository root
3. Review test output

### Intermediate: Understanding Tests
1. Read [Quick Reference - Coverage](quick-reference.md#test-coverage-by-java-features)
2. Read [Query Reference - Tested Queries](query-reference.md#tested-search-queries)
3. Review [Test Projects - Overview](test-projects.md#projects-overview)
4. Run specific tests: `go test -v -run TestInheritanceSearch`

### Advanced: Adding/Debugging Tests
1. Read [Integration Tests - Architecture](integration-tests.md#architecture)
2. Read [Integration Tests - Components](integration-tests.md#components)
3. Study [Query Reference - All Tested Patterns](query-reference.md#tested-search-queries)
4. Review [Integration Tests - Adding New Tests](integration-tests.md#adding-new-tests)
5. Add new test function and verify

### Expert: Migration Testing
1. Review [Query Reference - Migration Catalog](query-reference.md#migration-query-catalog)
2. Study [Test Projects - Legacy Patterns](test-projects.md#legacy-api-patterns-migration-targets)
3. Review [Quick Reference - Migration Use Cases](quick-reference.md#migration-use-cases-tested)
4. Design custom migration queries

---

## 📈 Test Coverage Achievement

**Status**: ✅ 100% COMPLETE!
**Coverage**: 15/15 location types (100%)

**All Location Types Tested**:
- ✅ Location 0: Default (all locations)
- ✅ Location 1: Inheritance
- ✅ Location 2: Method Calls
- ✅ Location 3: Constructors
- ✅ Location 4: Annotations
- ✅ Location 5: Implements
- ✅ Location 6: Enum Constants
- ✅ Location 7: Return Types
- ✅ Location 8: Imports
- ✅ Location 9: Variable Declarations
- ✅ Location 10: Type References
- ✅ Location 11: Package Declarations
- ✅ Location 12: Field Declarations
- ✅ Location 13: Method Declarations
- ✅ Location 14: Class Declarations

🎉 **Comprehensive test coverage achieved across all Java code search location types!**

See: [Query Reference](query-reference.md) for all tested patterns

---

## 🔗 External Resources

- **[JDT.LS Documentation](https://github.com/eclipse/eclipse.jdt.ls)** - Language server
- **[LSP Specification](https://microsoft.github.io/language-server-protocol/)** - Protocol spec
- **[Konveyor Analyzer LSP](https://github.com/konveyor/analyzer-lsp)** - JSON-RPC implementation
- **[Main Project README](../../README.md)** - Core analyzer architecture
- **[CLAUDE.md](../../CLAUDE.md)** - Development guidance

---

## 📝 Documentation Maintenance

When updating tests:
- ✅ Update coverage counts in all documents
- ✅ Add new queries to [Query Reference](query-reference.md)
- ✅ Update test counts in [Quick Reference](quick-reference.md)
- ✅ Document new Java patterns in [Test Projects](test-projects.md)
- ✅ Update [Integration Tests](integration-tests.md) if adding test infrastructure

---

**Happy Testing!** 🎉

For questions or issues, start with [Integration Tests - Troubleshooting](integration-tests.md#troubleshooting).
