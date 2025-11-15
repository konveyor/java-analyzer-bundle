# Parameter-Specific Matching for METHOD_CALL and CONSTRUCTOR_CALL

This document describes the parameter-specific matching feature for METHOD_CALL and CONSTRUCTOR_CALL patterns.

## Overview

Starting from this version, METHOD_CALL and CONSTRUCTOR_CALL patterns can distinguish between overloaded methods with different parameter signatures. This enables precise targeting of specific method overloads in migration rules.

## Basic Usage

### Syntax

```yaml
when:
  java.referenced:
    location: METHOD_CALL
    pattern: <fully.qualified.ClassName>.<methodName>(<param1Type>, <param2Type>, ...)
```

### Example: Exact Parameter Matching

Target only the 3-parameter version of `DriverManager.getConnection`:

```yaml
when:
  java.referenced:
    location: METHOD_CALL
    pattern: java.sql.DriverManager.getConnection(java.lang.String, java.lang.String, java.lang.String)
```

**Matches:**
```java
DriverManager.getConnection("jdbc:mysql://localhost/db", "user", "password");
```

**Does NOT match:**
```java
DriverManager.getConnection("jdbc:mysql://localhost/db");  // 1 param
DriverManager.getConnection("jdbc:mysql://localhost/db", props);  // 2 params (String, Properties)
```

## Advanced Features

### 1. Wildcard Matching

Use `*` to match any parameter type at a specific position:

```yaml
when:
  java.referenced:
    location: METHOD_CALL
    pattern: com.example.MyClass.processData(*, java.lang.String)
```

**Matches:**
```java
processData(123, "output");           // (int, String)
processData(new Object(), "output");  // (Object, String)
```

**Does NOT match:**
```java
processData("input", 123);  // Wrong order: (String, int)
```

### 2. Subtype Matching

Patterns automatically support subtype matching based on Java type hierarchies:

```yaml
when:
  java.referenced:
    location: METHOD_CALL
    pattern: com.example.MyClass.accept(java.lang.Object)
```

**Matches:**
```java
accept("string");        // String extends Object
accept(123);             // Integer extends Object
accept(new ArrayList()); // ArrayList extends Object
```

### 3. Generic Type Matching

Generic type parameters are matched exactly:

```yaml
when:
  java.referenced:
    location: METHOD_CALL
    pattern: com.example.MyClass.process(java.util.List<java.lang.String>)
```

**Matches:**
```java
process(new ArrayList<String>());
process(Arrays.asList("a", "b", "c"));
```

**Does NOT match:**
```java
process(new ArrayList<Integer>());  // Different generic type
process(new ArrayList());            // Raw type
```

### 4. Nested Generics

Complex nested generic types are supported:

```yaml
when:
  java.referenced:
    location: METHOD_CALL
    pattern: com.example.MyClass.transform(java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>)
```

**Matches:**
```java
transform(new HashMap<String, List<Integer>>());
```

### 5. Array Types

Array parameters can be specified:

```yaml
when:
  java.referenced:
    location: METHOD_CALL
    pattern: com.example.MyClass.process(java.lang.String[], int[])
```

**Matches:**
```java
process(new String[]{"a", "b"}, new int[]{1, 2, 3});
```

### 6. Primitive Types

Primitive types are supported:

```yaml
when:
  java.referenced:
    location: METHOD_CALL
    pattern: com.example.MyClass.calculate(int, double, boolean)
```

**Matches:**
```java
calculate(10, 3.14, true);
```

## Constructor Calls

The same parameter matching works for CONSTRUCTOR_CALL patterns:

```yaml
when:
  java.referenced:
    location: CONSTRUCTOR_CALL
    pattern: java.io.FileOutputStream(java.lang.String, boolean)
```

**Matches:**
```java
new FileOutputStream("/path/to/file", true);
```

**Does NOT match:**
```java
new FileOutputStream("/path/to/file");  // Different overload
```

## Backward Compatibility

Patterns **without** parameter types continue to work as before, matching all overloads:

```yaml
when:
  java.referenced:
    location: METHOD_CALL
    pattern: java.sql.DriverManager.getConnection
```

**Matches all overloads:**
```java
DriverManager.getConnection("url");
DriverManager.getConnection("url", props);
DriverManager.getConnection("url", "user", "password");
```

## Edge Cases

### Empty Parameter List

To match only methods with no parameters, use empty parentheses:

```yaml
pattern: com.example.MyClass.getInstance()
```

### Varargs

Varargs can be specified with the `...` notation:

```yaml
pattern: com.example.MyClass.format(java.lang.String, java.lang.Object...)
```

## Migration Examples

### Example 1: Deprecated Overload

Flag only the deprecated 2-param version of `setProperty`:

```yaml
- ruleID: deprecated-setproperty-001
  when:
    java.referenced:
      location: METHOD_CALL
      pattern: com.legacy.Config.setProperty(java.lang.String, java.lang.String)
  message: "Use setProperty(String, String, boolean) instead"
```

### Example 2: Security Issue

Detect insecure URL constructor usage:

```yaml
- ruleID: security-url-constructor-001
  when:
    java.referenced:
      location: CONSTRUCTOR_CALL
      pattern: java.net.URL(java.lang.String)
  message: "Avoid using URL(String) constructor due to DNS lookup issues"
```

### Example 3: API Migration

Match specific generic signatures during API migration:

```yaml
- ruleID: api-migration-001
  when:
    java.referenced:
      location: METHOD_CALL
      pattern: com.oldapi.Service.getData(java.util.List<java.lang.String>)
  message: "Migrate to newapi.Service.fetchData(Collection<String>)"
```

## Technical Details

### Type Resolution

- Parameter types are resolved using Eclipse JDT bindings
- Fully qualified names are required (e.g., `java.lang.String`, not `String`)
- Generic type arguments are preserved and matched exactly

### Subtype Matching Algorithm

The type matching algorithm:
1. Checks for exact type match (including generics)
2. Checks erasure type match (generics erased)
3. Recursively checks superclasses
4. Recursively checks interfaces

### Wildcards

- Wildcard `*` matches any single parameter type
- Wildcards do not match against parameter count (use appropriate number of parameters)
