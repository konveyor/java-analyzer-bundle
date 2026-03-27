# Test Projects for Java Analyzer Bundle

This directory contains sample Java projects used for integration testing of the Java Analyzer Bundle's JDT.LS search functionality.

## Projects Overview

### 1. test-project
A comprehensive test project designed to cover all search location types (0-14) supported by the analyzer.

**Purpose**: Systematic testing of individual search patterns and location types

**Technologies**:
- Java 17
- Jakarta Servlet API 5.0
- javax.persistence-api 2.2 (JPA annotations)
- javax.ejb-api 3.2.2 (EJB/JMS annotations)
- javax.jms-api 2.0.1 (JMS API)
- javax.annotation-api 1.3.2 (@DataSourceDefinition)

**Package Structure** (19 Java files):
```
io.konveyor.demo/
├── SampleApplication.java       - Main test class with various patterns
├── Calculator.java              - Return type examples (int, EnumExample)
├── EnumExample.java             - Enum constant examples
├── PackageUsageExample.java     - Package reference patterns
├── ServletExample.java          - Jakarta EE servlet example
├── annotations/
│   ├── CustomAnnotation.java    - Custom annotation definition
│   └── DeprecatedApi.java       - Deprecated API annotation
├── inheritance/
│   ├── BaseService.java         - Abstract base class (Serializable)
│   ├── DataService.java         - Extends BaseService
│   └── CustomException.java     - Extends Exception
├── jms/
│   ├── MessageProcessor.java    - @MessageDriven with @ActivationConfigProperty
│   └── TopicMessageProcessor.java - Additional JMS example
├── config/
│   ├── DataSourceConfig.java    - @DataSourceDefinition (PostgreSQL)
│   └── MySQLDataSourceConfig.java - @DataSourceDefinition (MySQL)
├── entity/
│   └── Product.java             - @Entity, @Column with attributes
└── persistence/
    ├── ServiceWithEntityManager.java - Mixed JPA/JDBC
    ├── JdbcOnlyService.java     - Pure JDBC with PreparedStatement
    ├── AnotherMixedService.java - Additional mixed pattern
    └── PureJpaService.java      - Pure JPA (no JDBC)
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

## Integration Test Status

All planned testing phases are complete:

- ✅ Commands execute without exceptions
- ✅ Parameter parsing and validation
- ✅ Real JDT.LS server with analyzer plugin loaded
- ✅ Search result verification (symbol names, kinds, locations)
- ✅ Migration pattern testing (javax→jakarta, Spring, JMS)
- ✅ Advanced features: annotated element matching, file path filtering

**18 test functions, 47 sub-tests, 100% pass rate.**

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

## Completed Milestones

1. ✅ Create test project structures
2. ✅ Add sample code covering all location types
3. ✅ Create workspace initialization in tests (Go LSP client)
4. ✅ Add result verification tests (18 functions, 47 sub-tests)
5. ✅ Create migration scenario tests (javax→jakarta, JMS, database drivers)
6. ✅ Add annotated element matching tests (4 sub-tests)
7. ✅ Add file path filtering tests (2 sub-tests)
