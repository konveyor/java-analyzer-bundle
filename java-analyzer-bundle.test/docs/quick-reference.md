# Integration Test Summary

Quick reference for what's tested and what's not in the Java Analyzer Bundle integration test suite.

## Coverage at a Glance

```
Location Types: 15/15 tested (100%) ✅
Test Functions: 18 (47 sub-tests total)
Advanced Features: 2/2 tested (Priority 1)
Query Patterns: 47 unique queries
Test Projects: 2 (systematic + real-world)
```

**✨ NEW (2025-10-28)**: Priority 1 advanced features complete!
- Annotated element matching (4 tests)
- File path filtering (2 tests)

## What's Tested ✅

| Location | Java Feature | Example Query | Test Project |
|----------|--------------|---------------|--------------|
| **0** | Default (All) | `java.io.File`, `println`, `BaseService` | test-project |
| **1** | Inheritance | `java.lang.Exception` | test-project |
| **2** | Method Calls | `println(*)`, `add(*)` | test-project |
| **3** | Constructors | `java.util.ArrayList`, `java.io.File` | test-project |
| **4** | Annotations | `@Entity`, `@Service`, `@CustomAnnotation` | both |
| **5** | Implements | `java.io.Serializable` | test-project |
| **6** | Enum Constants | `EnumExample.ACTIVE` | test-project |
| **7** | Return Types | `java.lang.String`, `int`, `EnumExample` | test-project |
| **8** | Imports | `java.io.File`, `javax.persistence.Entity` | both |
| **9** | Variable Decls | `java.lang.String`, `java.io.File` | test-project |
| **10** | Type Refs | `java.util.ArrayList` | test-project |
| **11** | Package Decls | `io.konveyor.demo.*`, `java.util`, `jakarta.*`, etc. | test-project |
| **12** | Field Decls | `java.util.List`, `java.io.File` | test-project |
| **13** | Method Decls | `processData`, `getName`, `add` | test-project |
| **14** | Class Decls | `SampleApplication` | test-project |

**Coverage**: 15/15 location types tested (100%) ✅ COMPLETE!

## Test Files

### Go Test Files
```
integration/
├── integration_test.go       # Main test functions
├── test_helpers.go           # Verification helpers
└── client/
    └── jdtls_client.go       # LSP client implementation
```

### Java Test Projects
```
projects/
├── test-project/                    # Systematic pattern coverage (19 Java files)
│   └── src/main/java/io/konveyor/demo/
│       ├── SampleApplication.java         # Main test file
│       ├── Calculator.java                # Return type examples
│       ├── EnumExample.java               # Enum constant examples
│       ├── PackageUsageExample.java       # Package reference examples
│       ├── ServletExample.java            # Jakarta servlet example
│       ├── annotations/CustomAnnotation.java
│       ├── annotations/DeprecatedApi.java
│       ├── inheritance/BaseService.java
│       ├── inheritance/DataService.java
│       ├── inheritance/CustomException.java
│       ├── jms/MessageProcessor.java      # JMS/EJB annotations
│       ├── jms/TopicMessageProcessor.java
│       ├── config/DataSourceConfig.java   # PostgreSQL @DataSourceDefinition
│       ├── config/MySQLDataSourceConfig.java # MySQL @DataSourceDefinition
│       ├── entity/Product.java            # JPA @Entity, @Column
│       ├── persistence/ServiceWithEntityManager.java
│       ├── persistence/JdbcOnlyService.java
│       ├── persistence/AnotherMixedService.java
│       └── persistence/PureJpaService.java
│
└── customers-tomcat-legacy/         # Real-world migration (10 Java files)
    └── src/main/java/io/konveyor/demo/ordermanagement/
        ├── model/Customer.java          # JPA entity
        ├── service/CustomerService.java # Spring service
        ├── controller/CustomerController.java
        ├── repository/CustomerRepository.java
        └── config/PersistenceConfig.java
```

## Test Functions

### Location Type Tests (15 tests - 100% coverage)

| Function | Location | Queries | Primary Assertion |
|----------|----------|---------|-------------------|
| `TestDefaultSearch` | 0 | 3 | Verify cross-location search |
| `TestInheritanceSearch` | 1 | 3 | Verify subclasses found |
| `TestMethodCallSearch` | 2 | 2 | Verify call sites found |
| `TestConstructorCallSearch` | 3 | 2 | Verify instantiations found |
| `TestAnnotationSearch` | 4 | 1 | Verify annotation on class |
| `TestImplementsTypeSearch` | 5 | 1 | Verify implementing class |
| `TestEnumConstantSearch` | 6 | 2 | Verify enum constant usage |
| `TestReturnTypeSearch` | 7 | 3 | Verify method return types |
| `TestImportSearch` | 8 | 1 | Verify import statements |
| `TestVariableDeclarationSearch` | 9 | 3 | Verify local variable declarations |
| `TestTypeSearch` | 10 | 1 | Verify type references |
| `TestPackageDeclarationSearch` | 11 | 8 | Verify package references in imports/FQNs |
| `TestFieldDeclarationSearch` | 12 | 3 | Verify field declarations |
| `TestMethodDeclarationSearch` | 13 | 4 | Verify method declarations |
| `TestClassDeclarationSearch` | 14 | 1 | Verify class declaration |

### Real-World Migration Tests (1 test)

| Function | Location | Queries | Primary Assertion |
|----------|----------|---------|-------------------|
| `TestCustomersTomcatLegacy` | 4, 8 | 3 | Migration patterns (javax→jakarta) |

### Priority 1 Advanced Feature Tests (2 tests)

| Function | Feature | Queries | Primary Assertion |
|----------|---------|---------|-------------------|
| `TestAnnotatedElementMatching` | Annotation attributes | 4 | Annotations with specific element values |
| `TestFilePathFiltering` | Path filtering | 2 | Directory-scoped searches |

## Key Test Patterns

### Wildcard Matching
```go
"println(*)"     // Method with any parameters
"get*"           // Methods starting with 'get'
"javax.persistence.*"  // All classes in package
```

### Fully Qualified Names
```go
"java.util.ArrayList"                           // JDK class
"io.konveyor.demo.inheritance.BaseService"      // Custom class
"javax.persistence.Entity"                      // Framework annotation
```

### Simple Names
```go
"SampleApplication"  // Class declaration (location 14)
"processData"        // Method name (location 13)
```

## Migration Use Cases Tested

### ✅ javax → jakarta Migration

**Tested**:
- Find `@Entity` annotations on classes (location 4)
- Find `javax.persistence.*` imports (location 8)

**Example**:
```java
// Query: javax.persistence.Entity (location 4)
// Finds: Customer.java with @Entity annotation

// Query: javax.persistence.Entity (location 8)
// Finds: Customer.java imports
import javax.persistence.Entity;
import javax.persistence.Table;
```

### ✅ Spring Framework Components

**Tested**:
- Find `@Service` annotations (location 4)
- Find `@Autowired` usage (location 4)

**Example**:
```java
// Query: org.springframework.stereotype.Service (location 4)
// Finds: CustomerService.java

@Service
@Transactional
public class CustomerService implements ICustomerService {
    @Autowired
    private CustomerRepository repository;
}
```

### ✅ JBoss Logging → SLF4J

**Available for testing** (location 10 - type references):
```java
// Query: org.jboss.logging.Logger
import org.jboss.logging.Logger;

private static Logger logger = Logger.getLogger(CustomerService.class.getName());
```

## Running Tests

### Quick Run (Recommended)
```bash
make ci         # Full CI pipeline
make phase2     # Just integration tests
```

### Manual Container Run
```bash
# Build plugin JAR first, then build image (from repository root)
mvn clean install -DskipTests
podman build -t jdtls-analyzer:test -f Dockerfile.test .

# Run tests
podman run --rm \
  -v $(pwd)/java-analyzer-bundle.test:/tests:Z \
  -e WORKSPACE_DIR=/tests/projects \
  -e JDTLS_PATH=/jdtls \
  --workdir /tests/integration \
  --entrypoint /bin/sh \
  jdtls-analyzer:test \
  -c "go mod download && go test -v"
```

**Note**: `Dockerfile.test` requires a pre-built JAR and pre-installs golang. CI uses the
default `Dockerfile` which builds the JAR from source and installs golang at runtime.

### Run Specific Test
```bash
go test -v -run TestInheritanceSearch
go test -v -run "TestMethod.*"
```

## Expected Test Output

```
=== RUN   TestInheritanceSearch
=== RUN   TestInheritanceSearch/Find_SampleApplication_extends_BaseService
=== RUN   TestInheritanceSearch/Find_DataService_extends_BaseService
=== RUN   TestInheritanceSearch/Find_CustomException_extends_Exception
--- PASS: TestInheritanceSearch (0.12s)
    --- PASS: TestInheritanceSearch/Find_SampleApplication_extends_BaseService (0.04s)
    --- PASS: TestInheritanceSearch/Find_DataService_extends_BaseService (0.04s)
    --- PASS: TestInheritanceSearch/Find_CustomException_extends_Exception (0.04s)

=== RUN   TestMethodCallSearch
=== RUN   TestMethodCallSearch/Find_println_calls
    integration_test.go:113: Found 8 println calls
=== RUN   TestMethodCallSearch/Find_List.add_in_SampleApplication
--- PASS: TestMethodCallSearch (0.08s)
    --- PASS: TestMethodCallSearch/Find_println_calls (0.04s)
    --- PASS: TestMethodCallSearch/Find_List.add_in_SampleApplication (0.04s)

...

PASS
ok      github.com/konveyor/java-analyzer-bundle/integration    5.234s
```

## Symbol Information Structure

Search results return LSP `SymbolInformation`:

```go
type SymbolInformation struct {
    Name          string        // Symbol name (e.g., "Customer", "println")
    Kind          SymbolKind    // Class, Method, Field, etc.
    Location      Location      // File URI and line range
    ContainerName string        // Parent context (e.g., package, class)
}
```

### Example Result
```go
// Query: javax.persistence.Entity (location 4)
SymbolInformation{
    Name:          "Entity",
    Kind:          Class,
    Location:      "file:///workspace/customers-tomcat/src/.../Customer.java#11",
    ContainerName: "Customer",
}
```

## Java Code Patterns Available

The test projects include patterns for **all 15 location types** - 100% tested ✅

### Code Richness

**test-project/SampleApplication.java** alone contains:
- 7 import statements (location 8)
- 1 class annotation (location 4)
- 1 inheritance relationship (location 1)
- 4 field declarations (location 12)
- 6 method declarations (location 13)
- 6 variable declarations across methods (location 9)
- 8 method calls (location 2)
- 6 constructor calls (location 3)
- Multiple type references (location 10)

## Advanced Features (Priority 1)

### Annotated Element Matching ✅

Search for annotations with specific attribute values - critical for migration detection.

**Test Function**: `TestAnnotatedElementMatching` (4 sub-tests)

**Examples**:
```go
// Find JMS message beans with specific destination lookup
annotationQuery := &client.AnnotationQuery{
    Pattern: "javax.ejb.ActivationConfigProperty",
    Elements: []client.AnnotationElement{
        {Name: "propertyName", Value: "destinationLookup"},
    },
}
symbols, _ := jdtlsClient.SearchSymbolsWithAnnotation("test-project",
    "javax.ejb.ActivationConfigProperty", 4, "source-only", nil, annotationQuery)
// Finds: MessageProcessor.java with @ActivationConfigProperty(propertyName="destinationLookup")

// Find database configurations by driver type
annotationQuery := &client.AnnotationQuery{
    Pattern: "javax.annotation.sql.DataSourceDefinition",
    Elements: []client.AnnotationElement{
        {Name: "className", Value: "org.postgresql.Driver"},
    },
}
// Finds: DataSourceConfig.java with PostgreSQL driver
```

**Use Cases**:
- JMS to Reactive Messaging migration
- Database driver detection for cloud migration
- JPA constraint analysis

### File Path Filtering ✅

Restrict searches to specific directories - useful for targeted analysis and anti-pattern detection.

**Test Function**: `TestFilePathFiltering` (2 sub-tests)

**Examples**:
```go
// Search only in persistence package
includedPaths := []string{"src/main/java/io/konveyor/demo/persistence"}
symbols, _ := jdtlsClient.SearchSymbols("test-project",
    "java.sql.PreparedStatement", 8, "source-only", includedPaths)
// Finds: 3 PreparedStatement imports ONLY in persistence package

// Verify other packages are excluded
includedPaths := []string{"src/main/java/io/konveyor/demo/jms"}
symbols, _ := jdtlsClient.SearchSymbols("test-project",
    "java.sql.PreparedStatement", 8, "source-only", includedPaths)
// Finds: 0 results (correct - jms package doesn't use JDBC)
```

**Important**: `includedPaths` requires exact directory paths, NOT glob patterns:
- ✅ Correct: `"src/main/java/io/konveyor/demo/persistence"`
- ❌ Wrong: `"**/persistence/*.java"`

**Use Cases**:
- Mixed JPA/JDBC anti-pattern detection
- Package-specific analysis
- Targeted migration assessment

## References

- **Detailed Coverage**: See `QUERY_COVERAGE.md` for all query patterns
- **Architecture**: See `README.md` for JDT.LS integration details
- **Test Code**: See `integration_test.go` for implementation
