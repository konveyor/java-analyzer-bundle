package io.konveyor.tackle.core.internal.testing;

import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.lsp4j.SymbolInformation;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;

import io.konveyor.tackle.core.internal.query.AnnotationQuery;

/**
 * Abstract base class for symbol provider integration tests.
 * 
 * <p>Provides common setup and teardown for tests that need a real Java project
 * with full JDT search capabilities. The test project is imported once per test
 * class and shared across all test methods.</p>
 * 
 * <p>Subclasses get convenient methods for executing searches by location type.</p>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * public class MethodCallSymbolProviderIntegrationTest extends AbstractSymbolProviderTest {
 *     
 *     @Test
 *     public void testFindsSimpleMethodCall() {
 *         List<SymbolInformation> results = searchMethodCalls("println");
 *         assertFalse("Should find println calls", results.isEmpty());
 *     }
 * }
 * }</pre>
 * 
 * <h3>Available Test Projects:</h3>
 * <ul>
 *   <li><code>test-project</code> - Main test project with various Java patterns</li>
 *   <li><code>customers-tomcat-legacy</code> - Spring/JPA legacy application</li>
 * </ul>
 */
public abstract class AbstractSymbolProviderTest {

    /** 
     * Default test project name. Subclasses can override by setting this in 
     * a @BeforeClass method before calling super.setUpTestProject().
     */
    protected static String testProjectName = "test-project";
    
    /** The imported Java project instance. */
    protected static IJavaProject testProject;
    
    /** Whether to use source-only mode (default) or full mode. */
    protected static String analysisMode = "source-only";

    /**
     * Sets up the test project before any tests run.
     * Imports the Maven project and waits for indexing to complete.
     * 
     * <p><b>Note:</b> These tests require an Eclipse/OSGi runtime. When run from
     * VS Code or as regular JUnit tests, they will be skipped.</p>
     */
    @BeforeClass
    public static void setUpTestProject() throws Exception {
        // Check if we're running in an Eclipse/OSGi environment
        // These tests require the full Eclipse runtime (ResourcesPlugin, M2E, JDT)
        boolean isEclipseRuntime = isRunningInEclipse();
        Assume.assumeTrue(
            "Skipping: These tests require Eclipse Plugin Test runtime. " +
            "Run via 'mvn verify' or as 'JUnit Plug-in Test' in Eclipse IDE.",
            isEclipseRuntime
        );
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Setting up test project: " + testProjectName);
        System.out.println("=".repeat(60) + "\n");
        
        // Check if project is already imported (from a previous test class)
        if (TestProjectManager.projectExists(testProjectName)) {
            System.out.println("Project already exists, reusing: " + testProjectName);
            testProject = TestProjectManager.getProject(testProjectName);
        } else {
            testProject = TestProjectManager.importMavenProject(testProjectName);
        }
        
        TestProjectManager.waitForProjectReady(testProject);
        
        System.out.println("\nTest project ready: " + testProjectName + "\n");
    }
    
    /**
     * Checks if we're running inside an Eclipse/OSGi runtime.
     */
    private static boolean isRunningInEclipse() {
        try {
            // Try to access the workspace - this will fail outside Eclipse runtime
            org.eclipse.core.resources.ResourcesPlugin.getWorkspace();
            return true;
        } catch (IllegalStateException | NoClassDefFoundError | ExceptionInInitializerError e) {
            System.out.println("Not running in Eclipse runtime: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cleans up the test project after all tests complete.
     * Override this method with an empty body if you want to keep the project
     * between test runs (useful for debugging).
     */
    @AfterClass
    public static void tearDownTestProject() throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Tearing down test project: " + testProjectName);
        System.out.println("=".repeat(60) + "\n");
        
        if (testProject != null) {
            TestProjectManager.deleteProject(testProject);
            testProject = null;
        }
    }

    /**
     * Searches for method calls (location type 2).
     * 
     * @param query Search pattern (e.g., "println", "java.io.PrintStream.println")
     * @return List of matching symbols
     */
    protected List<SymbolInformation> searchMethodCalls(String query) {
        return SearchTestHelper.executeSearch(
                testProjectName, query, SearchTestHelper.LOCATION_METHOD_CALL, analysisMode);
    }

    /**
     * Searches for method declarations (location type 13).
     * 
     * @param query Method name pattern (e.g., "processData", "get*")
     * @return List of matching symbols
     */
    protected List<SymbolInformation> searchMethodDeclarations(String query) {
        return SearchTestHelper.executeSearch(
                testProjectName, query, SearchTestHelper.LOCATION_METHOD_DECLARATION, analysisMode);
    }

    /**
     * Searches for constructor calls (location type 3).
     * 
     * @param query Type pattern (e.g., "java.util.ArrayList", "java.io.File")
     * @return List of matching symbols
     */
    protected List<SymbolInformation> searchConstructorCalls(String query) {
        return SearchTestHelper.executeSearch(
                testProjectName, query, SearchTestHelper.LOCATION_CONSTRUCTOR_CALL, analysisMode);
    }

    /**
     * Searches for annotations (location type 4).
     * 
     * @param query Annotation pattern (e.g., "javax.ejb.Stateless")
     * @return List of matching symbols
     */
    protected List<SymbolInformation> searchAnnotations(String query) {
        return SearchTestHelper.executeSearch(
                testProjectName, query, SearchTestHelper.LOCATION_ANNOTATION, analysisMode);
    }

    /**
     * Searches for annotations with element matching.
     * 
     * @param query           Annotation pattern
     * @param annotationQuery Annotation query with elements
     * @return List of matching symbols
     */
    protected List<SymbolInformation> searchAnnotations(String query, AnnotationQuery annotationQuery) {
        return SearchTestHelper.executeSearch(
                testProjectName, query, SearchTestHelper.LOCATION_ANNOTATION, analysisMode,
                null, annotationQuery);
    }

    /**
     * Searches for type references (location type 10).
     * 
     * @param query Type pattern (e.g., "java.util.List", "java.io.*")
     * @return List of matching symbols
     */
    protected List<SymbolInformation> searchTypes(String query) {
        return SearchTestHelper.executeSearch(
                testProjectName, query, SearchTestHelper.LOCATION_TYPE, analysisMode);
    }

    /**
     * Searches for imports (location type 8).
     * 
     * @param query Import pattern (e.g., "java.io.File", "java.util.*")
     * @return List of matching symbols
     */
    protected List<SymbolInformation> searchImports(String query) {
        return SearchTestHelper.executeSearch(
                testProjectName, query, SearchTestHelper.LOCATION_IMPORT, analysisMode);
    }

    /**
     * Searches for inheritance relationships (location type 1).
     * 
     * @param query Base type pattern (e.g., "java.lang.Exception")
     * @return List of matching symbols (classes extending the type)
     */
    protected List<SymbolInformation> searchInheritance(String query) {
        return SearchTestHelper.executeSearch(
                testProjectName, query, SearchTestHelper.LOCATION_INHERITANCE, analysisMode);
    }

    /**
     * Searches for implements relationships (location type 5).
     * 
     * @param query Interface pattern (e.g., "java.io.Serializable")
     * @return List of matching symbols (classes implementing the interface)
     */
    protected List<SymbolInformation> searchImplements(String query) {
        return SearchTestHelper.executeSearch(
                testProjectName, query, SearchTestHelper.LOCATION_IMPLEMENTS_TYPE, analysisMode);
    }

    /**
     * Searches for field declarations (location type 12).
     * 
     * @param query Field type pattern (e.g., "java.lang.String")
     * @return List of matching symbols
     */
    protected List<SymbolInformation> searchFields(String query) {
        return SearchTestHelper.executeSearch(
                testProjectName, query, SearchTestHelper.LOCATION_FIELD, analysisMode);
    }

    /**
     * Searches for variable declarations (location type 9).
     * 
     * @param query Variable type pattern
     * @return List of matching symbols
     */
    protected List<SymbolInformation> searchVariableDeclarations(String query) {
        return SearchTestHelper.executeSearch(
                testProjectName, query, SearchTestHelper.LOCATION_VARIABLE_DECLARATION, analysisMode);
    }

    /**
     * Searches for return types (location type 7).
     * 
     * @param query Return type pattern
     * @return List of matching symbols
     */
    protected List<SymbolInformation> searchReturnTypes(String query) {
        return SearchTestHelper.executeSearch(
                testProjectName, query, SearchTestHelper.LOCATION_RETURN_TYPE, analysisMode);
    }

    /**
     * Searches for class declarations (location type 14).
     * 
     * @param query Class name pattern
     * @return List of matching symbols
     */
    protected List<SymbolInformation> searchClassDeclarations(String query) {
        return SearchTestHelper.executeSearch(
                testProjectName, query, SearchTestHelper.LOCATION_CLASS_DECLARATION, analysisMode);
    }

    /**
     * Searches for package usage (location type 11).
     * 
     * @param query Package pattern (e.g., "java.util", "javax.persistence")
     * @return List of matching symbols
     */
    protected List<SymbolInformation> searchPackages(String query) {
        return SearchTestHelper.executeSearch(
                testProjectName, query, SearchTestHelper.LOCATION_PACKAGE, analysisMode);
    }

    /**
     * Searches for enum constants (location type 6).
     * 
     * @param query Enum constant pattern
     * @return List of matching symbols
     */
    protected List<SymbolInformation> searchEnumConstants(String query) {
        return SearchTestHelper.executeSearch(
                testProjectName, query, SearchTestHelper.LOCATION_ENUM_CONSTANT, analysisMode);
    }

    /**
     * Prints search results for debugging.
     */
    protected void printResults(List<SymbolInformation> symbols) {
        SearchTestHelper.printResults(symbols);
    }

    /**
     * Returns a summary of search results.
     */
    protected String summarize(List<SymbolInformation> symbols) {
        return SearchTestHelper.summarizeResults(symbols);
    }

    /**
     * Filters results to a specific file.
     */
    protected List<SymbolInformation> inFile(List<SymbolInformation> symbols, String fileName) {
        return SearchTestHelper.filterByFile(symbols, fileName);
    }

    /**
     * Filters results by symbol name.
     */
    protected List<SymbolInformation> withName(List<SymbolInformation> symbols, String name) {
        return SearchTestHelper.filterByName(symbols, name);
    }
}
