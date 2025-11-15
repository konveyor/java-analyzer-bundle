package io.konveyor.tackle.core.internal.symbol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for CustomASTVisitor parameter extraction logic
 */
public class CustomASTVisitorTest {

    private CustomASTVisitor visitor;

    @Before
    public void setUp() {
        // We only need to test extractParameterTypes, which doesn't require
        // a full visitor setup, but we need an instance to call the method
        visitor = new CustomASTVisitor("dummy", null, CustomASTVisitor.QueryLocation.METHOD_CALL);
    }

    @Test
    public void testExtractParameterTypes_noParameters() {
        String query = "com.example.ClassName.method";
        List<String> result = visitor.extractParameterTypes(query);
        assertNull("Should return null when no parameters specified", result);
    }

    @Test
    public void testExtractParameterTypes_emptyParameters() {
        String query = "com.example.ClassName.method()";
        List<String> result = visitor.extractParameterTypes(query);
        assertEquals("Should return empty list for empty parameters",
                     Collections.emptyList(), result);
    }

    @Test
    public void testExtractParameterTypes_singleParameter() {
        String query = "com.example.ClassName.method(java.lang.String)";
        List<String> result = visitor.extractParameterTypes(query);
        assertEquals("Should extract single parameter",
                     Arrays.asList("java.lang.String"), result);
    }

    @Test
    public void testExtractParameterTypes_twoParameters() {
        String query = "com.example.ClassName.method(java.lang.String, java.lang.Integer)";
        List<String> result = visitor.extractParameterTypes(query);
        assertEquals("Should extract two parameters",
                     Arrays.asList("java.lang.String", "java.lang.Integer"), result);
    }

    @Test
    public void testExtractParameterTypes_threeParameters() {
        String query = "java.sql.DriverManager.getConnection(java.lang.String, java.lang.String, java.lang.String)";
        List<String> result = visitor.extractParameterTypes(query);
        assertEquals("Should extract three parameters",
                     Arrays.asList("java.lang.String", "java.lang.String", "java.lang.String"), result);
    }

    @Test
    public void testExtractParameterTypes_withWildcard() {
        String query = "com.example.ClassName.method(*, java.lang.String)";
        List<String> result = visitor.extractParameterTypes(query);
        assertEquals("Should extract parameters including wildcard",
                     Arrays.asList("*", "java.lang.String"), result);
    }

    @Test
    public void testExtractParameterTypes_simpleGeneric() {
        String query = "com.example.ClassName.method(java.util.List<java.lang.String>)";
        List<String> result = visitor.extractParameterTypes(query);
        assertEquals("Should handle simple generic types",
                     Arrays.asList("java.util.List<java.lang.String>"), result);
    }

    @Test
    public void testExtractParameterTypes_nestedGenerics() {
        String query = "com.example.ClassName.method(java.util.Map<java.lang.String, java.lang.Integer>)";
        List<String> result = visitor.extractParameterTypes(query);
        assertEquals("Should handle nested generics with comma",
                     Arrays.asList("java.util.Map<java.lang.String, java.lang.Integer>"), result);
    }

    @Test
    public void testExtractParameterTypes_multipleGenerics() {
        String query = "com.example.ClassName.method(java.util.List<java.lang.String>, java.util.Set<java.lang.Integer>)";
        List<String> result = visitor.extractParameterTypes(query);
        assertEquals("Should handle multiple generic parameters",
                     Arrays.asList("java.util.List<java.lang.String>", "java.util.Set<java.lang.Integer>"), result);
    }

    @Test
    public void testExtractParameterTypes_deeplyNestedGenerics() {
        String query = "com.example.ClassName.method(java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>)";
        List<String> result = visitor.extractParameterTypes(query);
        assertEquals("Should handle deeply nested generics",
                     Arrays.asList("java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>"), result);
    }

    @Test
    public void testExtractParameterTypes_primitiveTypes() {
        String query = "com.example.ClassName.method(int, boolean, double)";
        List<String> result = visitor.extractParameterTypes(query);
        assertEquals("Should handle primitive types",
                     Arrays.asList("int", "boolean", "double"), result);
    }

    @Test
    public void testExtractParameterTypes_mixedTypes() {
        String query = "com.example.ClassName.method(int, java.lang.String, java.util.List<java.lang.Integer>)";
        List<String> result = visitor.extractParameterTypes(query);
        assertEquals("Should handle mixed primitive, object, and generic types",
                     Arrays.asList("int", "java.lang.String", "java.util.List<java.lang.Integer>"), result);
    }

    @Test
    public void testExtractParameterTypes_arrayTypes() {
        String query = "com.example.ClassName.method(java.lang.String[], int[])";
        List<String> result = visitor.extractParameterTypes(query);
        assertEquals("Should handle array types",
                     Arrays.asList("java.lang.String[]", "int[]"), result);
    }

    @Test
    public void testExtractParameterTypes_withWhitespace() {
        String query = "com.example.ClassName.method( java.lang.String , java.lang.Integer )";
        List<String> result = visitor.extractParameterTypes(query);
        assertEquals("Should trim whitespace from parameters",
                     Arrays.asList("java.lang.String", "java.lang.Integer"), result);
    }

    @Test
    public void testExtractParameterTypes_realWorldExample() {
        String query = "java.util.Properties.setProperty(java.lang.String, java.lang.String)";
        List<String> result = visitor.extractParameterTypes(query);
        assertEquals("Should handle real-world example from issue",
                     Arrays.asList("java.lang.String", "java.lang.String"), result);
    }

    @Test
    public void testExtractParameterTypes_varargs() {
        String query = "com.example.ClassName.method(java.lang.String...)";
        List<String> result = visitor.extractParameterTypes(query);
        assertEquals("Should handle varargs notation",
                     Arrays.asList("java.lang.String..."), result);
    }
}
