# Test Projects for Java Analyzer Bundle

This directory contains sample Java projects used for integration testing of the Java Analyzer Bundle's JDT.LS search functionality.

## Projects Overview

### 1. test-project
A comprehensive test project designed to cover all search location types (0-14) supported by the analyzer.

**Purpose**: Systematic testing of individual search patterns and location types

**Technologies**:
- Java 17
- Jakarta Servlet API 5.0

**Package Structure**:
```
io.konveyor.demo/
├── annotations/          - Custom annotations
├── inheritance/          - Inheritance and interface examples
├── SampleApplication.java - Main test class with various patterns
├── Calculator.java       - Return type examples
├── EnumExample.java      - Enum constant examples
└── ServletExample.java   - Jakarta EE servlet example
```

**Covered Location Types**:
- **0 (Default)**: General searches
- **1 (Inheritance)**: `extends BaseService`, `extends Exception`
- **2 (Method Call)**: `System.out.println()`, `items.add()`, `file.exists()`
- **3 (Constructor Call)**: `new String()`, `new ArrayList<>()`, `new File()`
- **4 (Annotation)**: `@CustomAnnotation`, `@DeprecatedApi`
- **5 (Implements Type)**: `implements Serializable`, `implements Comparable`, `implements AutoCloseable`
- **6 (Enum Constant)**: `ACTIVE`, `INACTIVE`, `PENDING`
- **7 (Return Type)**: Methods returning `int`, `String`, `EnumExample`, etc.
- **8 (Import)**: `import java.io.File`, `import java.util.ArrayList`
- **9 (Variable Declaration)**: Local variables of various types
- **10 (Type)**: All type references
- **11 (Package)**: `io.konveyor.demo`, `java.io`
- **12 (Field)**: Class fields of various types
- **13 (Method Declaration)**: `processData()`, `getName()`, `add()`
- **14 (Class Declaration)**: `SampleApplication`, `Calculator`, etc.

---

### 2. customers-tomcat-legacy
A realistic legacy Spring MVC application for testing real-world migration scenarios.

**Purpose**: Integration testing with complex framework patterns and annotations

**Technologies**:
- Java 1.8
- Spring Framework 5.3.7
- Spring Data JPA
- Hibernate 5.4.32
- Tomcat 9.0.46
- JPA/Hibernate annotations
- Oracle & PostgreSQL JDBC drivers
- JBoss Logging

**Package Structure**:
```
io.konveyor.demo.ordermanagement/
├── config/               - Spring configuration
│   ├── PersistenceConfig.java
│   └── WebConfig.java
├── controller/           - REST controllers
│   └── CustomerController.java
├── exception/            - Custom exceptions and handlers
│   ├── ResourceNotFoundException.java
│   └── handler/ExceptionHandlingController.java
├── model/                - JPA entities
│   └── Customer.java
├── repository/           - Spring Data repositories
│   └── CustomerRepository.java
├── service/              - Business logic services
│   ├── ICustomerService.java
│   └── CustomerService.java
└── OrderManagementAppInitializer.java
```

**Key Search Patterns**:

#### JPA Annotations:
- `@Entity` - Entity classes
- `@Table(name = "customers")` - Table mappings
- `@Id` - Primary keys
- `@Column` - Column mappings
- `@GeneratedValue` - ID generation
- `@SequenceGenerator` - Sequence generators

#### Spring Annotations:
- `@RestController` - REST controllers
- `@Service` - Service beans
- `@Autowired` - Dependency injection
- `@Transactional` - Transaction management
- `@RequestMapping` - Request mappings
- `@GetMapping` - HTTP GET mappings
- `@PathVariable` - Path variables

#### Framework Patterns:
- **Spring Data Repositories**: `extends JpaRepository`
- **Spring MVC**: Controllers, request mappings
- **JBoss Logging**: `Logger.getLogger()`
- **Exception Handling**: Custom exceptions extending `RuntimeException`
- **Pagination**: `Page<Customer>`, `Pageable`

#### Legacy API Patterns (Migration Targets):
- `javax.persistence.*` → Should migrate to `jakarta.persistence.*`
- Oracle JDBC driver (`com.oracle.database.jdbc`)
- JBoss Logging → Might migrate to SLF4J/Logback
- Older Spring Data patterns

---

## Using These Projects for Testing

### Test Scenarios

#### 1. Annotation Searches (Location 4)
```java
// Search for JPA @Entity annotations
query: "javax.persistence.Entity"
location: 4
expectedResults: Customer.java

// Search for Spring @Service annotations
query: "org.springframework.stereotype.Service"
location: 4
expectedResults: CustomerService.java
```

#### 2. Inheritance Searches (Location 1)
```java
// Search for classes extending BaseService
query: "io.konveyor.demo.inheritance.BaseService"
location: 1
expectedResults: SampleApplication.java, DataService.java

// Search for Exception subclasses
query: "java.lang.Exception"
location: 1
expectedResults: CustomException.java
```

#### 3. Method Call Searches (Location 2)
```java
// Search for System.out.println calls
query: "*.println"
location: 2
expectedResults: Multiple matches across both projects

// Search for Logger usage
query: "org.jboss.logging.Logger.getLogger"
location: 2
expectedResults: CustomerController.java, CustomerService.java
```

#### 4. Import Searches (Location 8)
```java
// Search for javax.persistence imports (migration target!)
query: "javax.persistence.*"
location: 8
expectedResults: Customer.java

// Search for Spring imports
query: "org.springframework.*"
location: 8
expectedResults: Multiple Spring classes
```

#### 5. Constructor Call Searches (Location 3)
```java
// Search for ArrayList instantiations
query: "java.util.ArrayList"
location: 3
expectedResults: SampleApplication.java

// Search for File instantiations
query: "java.io.File"
location: 3
expectedResults: SampleApplication.java
```

#### 6. Implements Type Searches (Location 5)
```java
// Search for Serializable implementations
query: "java.io.Serializable"
location: 5
expectedResults: BaseService.java

// Search for Spring Data repositories
query: "org.springframework.data.repository.JpaRepository"
location: 5
expectedResults: CustomerRepository.java
```

---

## Integration Test Strategy

### Phase 1: Basic Verification Tests (Current)
- ✅ Verify commands execute without exceptions
- ✅ Verify parameter parsing
- ✅ Verify non-null results

### Phase 2: Search Result Verification (Next)
1. **Load test projects into Eclipse workspace**
2. **Verify search result counts**:
   ```java
   assertEquals(expectedCount, results.size());
   ```
3. **Verify symbol information**:
   ```java
   assertEquals("Customer", symbol.getName());
   assertEquals(SymbolKind.Class, symbol.getKind());
   assertTrue(symbol.getLocation().getUri().contains("Customer.java"));
   ```

### Phase 3: Migration Pattern Testing
Test common migration scenarios:
- **javax → jakarta** namespace migration
- **Oracle → PostgreSQL** driver migration
- **JBoss → SLF4J** logging migration
- **Spring Framework** version upgrades
- **Legacy servlet → Spring Boot** migration

---

## Test Data Characteristics

### test-project
- **Simple, focused examples**
- **Each class targets specific location types**
- **Minimal dependencies**
- **Easy to reason about expected results**

### customers-tomcat-legacy
- **Real-world complexity**
- **Framework-heavy (Spring, Hibernate)**
- **Multiple annotation types**
- **Realistic migration targets**
- **Tests framework-specific search patterns**

---

## Adding New Test Projects

When adding new test projects:

1. **Create Maven project** with appropriate dependencies
2. **Document search patterns** it's designed to test
3. **Include various location types** to maximize coverage
4. **Add migration targets** if testing migration scenarios
5. **Update this README** with project details and test scenarios

---

## Coverage Matrix

| Location Type | test-project | customers-tomcat-legacy |
|---------------|--------------|-------------------------|
| 0 (Default) | ✅ | ✅ |
| 1 (Inheritance) | ✅ BaseService, Exception | ✅ JpaRepository |
| 2 (Method Call) | ✅ println, add, exists | ✅ Logger, Repository methods |
| 3 (Constructor) | ✅ File, ArrayList, String | ✅ Logger.getLogger |
| 4 (Annotation) | ✅ CustomAnnotation | ✅ @Entity, @Service, @Autowired |
| 5 (Implements) | ✅ Serializable, Comparable | ✅ JpaRepository |
| 6 (Enum) | ✅ EnumExample constants | ❌ |
| 7 (Return Type) | ✅ int, String, custom | ✅ Page, Customer |
| 8 (Import) | ✅ java.io, java.util | ✅ javax.persistence, org.springframework |
| 9 (Variable) | ✅ Local variables | ✅ Local variables |
| 10 (Type) | ✅ All types | ✅ All types |
| 11 (Package) | ✅ io.konveyor.demo | ✅ io.konveyor.demo.ordermanagement |
| 12 (Field) | ✅ String, List, File | ✅ Logger, CustomerService, Repository |
| 13 (Method Decl) | ✅ processData, getName | ✅ findById, findAll |
| 14 (Class Decl) | ✅ SampleApplication, Calculator | ✅ Customer, CustomerService |

---

## Next Steps

1. **✅ Create test project structures**
2. **✅ Add sample code covering all location types**
3. **⏳ Create workspace initialization in tests**
4. **⏳ Add result verification tests**
5. **⏳ Create migration scenario tests**
6. **⏳ Add test documentation for each scenario**
