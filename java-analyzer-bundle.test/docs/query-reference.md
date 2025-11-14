# Integration Test Query Coverage

This document provides a comprehensive overview of all search queries tested by the integration test suite, organized by location type and use case.

## Quick Reference

**Total Tests**: 9 test functions covering 8 location types
**Test Projects**:
- `test-project`: Systematic patterns for all location types
- `customers-tomcat`: Real-world Spring MVC application with migration targets

---

## Tested Search Queries

### Location 1: Inheritance

**Tests**: 3 query patterns

| Query | Project | Expected Results | Java Pattern |
|-------|---------|------------------|--------------|
| `io.konveyor.demo.inheritance.BaseService` | test-project | `SampleApplication`, `DataService` | Custom class inheritance |
| `java.lang.Exception` | test-project | `CustomException` | JDK class inheritance |

**What Gets Found**: Classes that extend the queried class

**Code Examples**:
```java
// SampleApplication.java
public class SampleApplication extends BaseService { }

// DataService.java
public class DataService extends BaseService { }

// CustomException.java
public class CustomException extends Exception { }
```

**Symbol Information Returned**:
- Name: Subclass name (e.g., "SampleApplication")
- Kind: `Class`
- Location: Class declaration line
- ContainerName: Package

---

### Location 2: Method Calls

**Tests**: 2 query patterns

| Query | Project | Expected Results | Java Pattern |
|-------|---------|------------------|--------------|
| `println(*)` | test-project | Multiple println calls | Wildcard method matching |
| `add(*)` | test-project | List.add calls in processData | Instance method calls |

**What Gets Found**: Method invocation sites

**Code Examples**:
```java
// Multiple println calls found across files
System.out.println("Processing: " + tempData);
System.out.println(path);
System.out.println("Version 1.0");

// add method calls
items.add(tempData);
results.add("item");
```

**Symbol Information Returned**:
- Name: Method name (e.g., "println", "add")
- Kind: `Method`
- Location: Call site line
- ContainerName: Containing method

**Pattern Features**:
- `*` wildcard matches any parameters
- Works with unqualified method names (doesn't require full class path)

---

### Location 3: Constructor Calls

**Tests**: 2 query patterns

| Query | Project | Expected Results | Java Pattern |
|-------|---------|------------------|--------------|
| `java.util.ArrayList` | test-project | 3+ ArrayList instantiations | Generic collection creation |
| `java.io.File` | test-project | 5+ File instantiations | File object creation |

**What Gets Found**: Object instantiation sites (new keyword usage)

**Code Examples**:
```java
// ArrayList constructors
this.items = new ArrayList<>();
List<String> results = new ArrayList<>();

// File constructors
this.configFile = new File("config.xml");
File tempFile = new File("/tmp/data.txt");
File dir = new File("/tmp");
File file1 = new File(dir, "test.txt");
```

**Symbol Information Returned**:
- Name: Constructor invocation
- Kind: `Constructor`
- Location: new keyword line
- ContainerName: Containing method

**Use Cases**:
- Find object creation patterns
- Identify instantiation of deprecated classes
- Track resource allocation (Files, Streams, Connections)

---

### Location 4: Annotations

**Tests**: 3 query patterns

| Query | Project | Expected Results | Java Pattern |
|-------|---------|------------------|--------------|
| `io.konveyor.demo.annotations.CustomAnnotation` | test-project | `@CustomAnnotation` on SampleApplication | Custom annotation usage |
| `javax.persistence.Entity` | customers-tomcat | `@Entity` on Customer | JPA entity (migration target) |
| `org.springframework.stereotype.Service` | customers-tomcat | `@Service` on CustomerService | Spring component |

**What Gets Found**: Annotation usage on classes, methods, fields

**Code Examples**:
```java
// Custom annotation
@CustomAnnotation(value = "SampleApp", version = "1.0")
public class SampleApplication extends BaseService { }

// JPA annotation (javax → jakarta migration target)
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    private Long id;

    @Column(length = 20)
    private String username;
}

// Spring annotation
@Service
@Transactional
public class CustomerService implements ICustomerService { }
```

**Symbol Information Returned**:
- Name: Annotation simple name (e.g., "Entity", "Service", "CustomAnnotation")
- Kind: `Class` or `Interface`
- Location: Annotation line
- ContainerName: Annotated element name (e.g., "Customer", "CustomerService")

**Migration Use Cases**:
- Find `javax.persistence.*` annotations for Jakarta EE migration
- Find Spring framework annotations
- Identify custom annotation usage

---

### Location 5: Implements Type

**Tests**: 1 query pattern

| Query | Project | Expected Results | Java Pattern |
|-------|---------|------------------|--------------|
| `java.io.Serializable` | test-project | `BaseService` | Interface implementation |

**What Gets Found**: Classes implementing the specified interface

**Code Examples**:
```java
public abstract class BaseService implements Serializable, Comparable<BaseService> {
    private static final long serialVersionUID = 1L;
    // ...
}
```

**Symbol Information Returned**:
- Name: Implementing class name
- Kind: `Class`
- Location: Class declaration line
- ContainerName: Package

**Use Cases**:
- Find all implementations of a deprecated interface
- Identify classes using specific frameworks (e.g., `Runnable`, `Callable`)
- Track serializable classes

---

### Location 8: Imports

**Tests**: 2 query patterns

| Query | Project | Expected Results | Java Pattern |
|-------|---------|------------------|--------------|
| `java.io.File` | test-project | File import statements | Specific class import |
| `javax.persistence.Entity` | customers-tomcat | javax.persistence imports | Migration target detection |

**What Gets Found**: Import statement lines

**Code Examples**:
```java
// test-project/SampleApplication.java
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

// customers-tomcat/Customer.java (migration targets)
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
```

**Symbol Information Returned**:
- Name: Imported class/package
- Kind: Varies
- Location: Import statement line
- ContainerName: File/package

**Migration Use Cases**:
- **Critical for javax → jakarta migration**: Find all files importing `javax.persistence.*`, `javax.servlet.*`, etc.
- Find deprecated API usage
- Identify framework dependencies

---

### Location 10: Type References

**Tests**: 1 query pattern

| Query | Project | Expected Results | Java Pattern |
|-------|---------|------------------|--------------|
| `java.util.ArrayList` | test-project | All ArrayList type usage | Type references in declarations |

**What Gets Found**: Type usage in variable declarations, fields, parameters, return types

**Code Examples**:
```java
// Field declarations
private List<String> items;

// Variable declarations
List<String> results = new ArrayList<>();

// Parameter types
public void addItems(List<String> newItems) { }

// Return types
public List<String> getItems() { return items; }

// Generic type parameters
Map<String, List<Integer>> data;
```

**Symbol Information Returned**:
- Name: Context where type is used
- Kind: Varies (Field, Method, Variable)
- Location: Type reference line

**Use Cases**:
- Find all usages of a specific type
- Identify collections usage patterns
- Track type dependencies

---

### Location 14: Class Declarations

**Tests**: 1 query pattern

| Query | Project | Expected Results | Java Pattern |
|-------|---------|------------------|--------------|
| `SampleApplication` | test-project | SampleApplication class | Class definition |

**What Gets Found**: The class declaration itself

**Code Examples**:
```java
public class SampleApplication extends BaseService {
    // class body
}
```

**Symbol Information Returned**:
- Name: Class name
- Kind: `Class`
- Location: Class declaration line
- ContainerName: Package

**Use Cases**:
- Find specific class definitions
- Verify class exists
- Get class location

---

## Real-World Migration Testing

### customers-tomcat-legacy Project

**Purpose**: Tests real Spring MVC application migration scenarios

#### Test: Find @Entity Annotations
```java
// Query: javax.persistence.Entity (location 4)
// Finds: Customer.java

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
}
```

**Migration Action**: Replace `javax.persistence.Entity` with `jakarta.persistence.Entity`

---

#### Test: Find @Service Annotations
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

**Use Case**: Identify Spring components for framework upgrade

---

#### Test: Find javax.persistence Imports
```java
// Query: javax.persistence.Entity (location 8)
// Finds: Customer.java import statements

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
```

**Migration Action**: This is the primary detection method for jakarta migration
- Each import flagged indicates code that needs updating
- Can search for `javax.persistence.*` to find all JPA imports
- Can search for `javax.servlet.*` for servlet migration
- Can search for `javax.transaction.*` for transaction migration

---

## Query Pattern Features

### Wildcards

The analyzer supports wildcard patterns in queries:

#### Method Wildcards
```go
// Find all println calls regardless of parameters
"println(*)"

// Find all methods starting with 'get'
"get*"

// Find all methods containing 'process'
"*process*"
```

#### Package Wildcards
```go
// Find all imports from java.io package
"java.io.*"

// Find all javax.persistence classes
"javax.persistence.*"
```

### Fully Qualified Names

Most queries use fully qualified class names:

```go
"java.util.ArrayList"                    // JDK class
"io.konveyor.demo.inheritance.BaseService"  // Custom class
"javax.persistence.Entity"               // Framework annotation
```

### Simple Names

Some location types accept simple names:

```go
"SampleApplication"  // Class declaration search (location 14)
"processData"        // Method declaration search (location 13)
```

---

## Test Execution Summary

### Test Function Overview

| Test Function | Location Types | Queries | Assertions |
|---------------|----------------|---------|------------|
| `TestInheritanceSearch` | 1 | 3 | Verify specific classes found |
| `TestMethodCallSearch` | 2 | 2 | Verify call sites and count |
| `TestConstructorCallSearch` | 3 | 2 | Verify instantiation count |
| `TestAnnotationSearch` | 4 | 1 | Verify annotation on target |
| `TestImplementsTypeSearch` | 5 | 1 | Verify implementing class |
| `TestImportSearch` | 8 | 1 | Verify import statements |
| `TestTypeSearch` | 10 | 1 | Verify type reference count |
| `TestClassDeclarationSearch` | 14 | 1 | Verify class declaration |
| `TestCustomersTomcatLegacy` | 4, 8 | 3 | Migration pattern verification |

**Total Query Executions**: 15 unique search queries across 9 test functions

---

## Test Projects Structure

### test-project

**Purpose**: Systematic coverage of all location types

**Key Files**:
- `SampleApplication.java` - Main test file with method calls, constructors, fields, variables
- `BaseService.java` - Inheritance base class, implements Serializable
- `DataService.java` - Another BaseService subclass
- `CustomException.java` - Exception inheritance
- `CustomAnnotation.java` - Custom annotation definition
- `EnumExample.java` - Enum with constants (location 6)

**Coverage**: Designed to test all 15 location types systematically

---

### customers-tomcat-legacy

**Purpose**: Real-world migration scenario testing

**Key Files**:
- `Customer.java` - JPA entity with javax.persistence annotations
- `CustomerService.java` - Spring service with @Service, @Autowired
- `CustomerRepository.java` - JPA repository
- `CustomerController.java` - Spring MVC controller
- `PersistenceConfig.java` - JPA configuration
- `WebConfig.java` - Spring MVC configuration

**Coverage**: Real migration targets (javax → jakarta, JBoss → SLF4J)

**Annotations Present**:
- `@Entity`, `@Table`, `@Id`, `@Column`, `@GeneratedValue` (JPA)
- `@Service`, `@Transactional`, `@Autowired` (Spring)
- `@RestController`, `@RequestMapping`, `@GetMapping` (Spring MVC)

---

## Migration Query Catalog

Common queries for identifying migration targets:

### javax → jakarta Migration

```go
// JPA Entities
"javax.persistence.Entity"        // location 4 (annotations)
"javax.persistence.*"             // location 8 (imports)

// Servlet API
"javax.servlet.http.HttpServlet"  // location 1 (inheritance)
"javax.servlet.*"                 // location 8 (imports)

// Transactions
"javax.transaction.Transactional" // location 4 (annotations)
"javax.transaction.*"             // location 8 (imports)
```

### Spring Framework Upgrade

```go
// Component scanning
"org.springframework.stereotype.Service"     // location 4
"org.springframework.stereotype.Controller"  // location 4
"org.springframework.stereotype.Repository"  // location 4

// Dependency injection
"org.springframework.beans.factory.annotation.Autowired" // location 4

// Web MVC
"org.springframework.web.bind.annotation.RestController" // location 4
"org.springframework.web.bind.annotation.RequestMapping" // location 4
```

### Logging Framework Migration (JBoss → SLF4J)

```go
// Find JBoss Logger usage
"org.jboss.logging.Logger"        // location 10 (type references)
"org.jboss.logging.*"             // location 8 (imports)

// Replace with SLF4J
"org.slf4j.Logger"                // location 10
```

---

## Performance Characteristics

**Test Execution Time**: ~5-10 seconds for full suite in container
**JDT.LS Initialization**: ~5 seconds
**Per-Query Time**: ~100-500ms depending on result count
**Result Counts**: Range from 1 (specific class) to 15+ (common types like String)

---

## Next Steps for Complete Coverage

**🎉 100% Location Type Coverage Achieved!**

All 15 location types (0-14) are now fully tested with comprehensive integration tests:
- ✅ Location 0 - Default search behavior (`TestDefaultSearch`)
- ✅ Location 6 - Enum constant references (`TestEnumConstantSearch`)
- ✅ Location 7 - Method return types (`TestReturnTypeSearch`)
- ✅ Location 9 - Variable declarations (`TestVariableDeclarationSearch`)
- ✅ Location 11 - Package declarations (`TestPackageDeclarationSearch`)
- ✅ Location 12 - Field declarations (`TestFieldDeclarationSearch`)
- ✅ Location 13 - Method declarations (`TestMethodDeclarationSearch`)

Plus **Priority 1 Advanced Features**:
- ✅ Annotated element matching (4 tests in `TestAnnotatedElementMatching`)
- ✅ File path filtering (2 tests in `TestFilePathFiltering`)
