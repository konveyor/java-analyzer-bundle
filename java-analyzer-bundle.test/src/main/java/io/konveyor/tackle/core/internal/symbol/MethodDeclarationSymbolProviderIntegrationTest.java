package io.konveyor.tackle.core.internal.symbol;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.lsp4j.SymbolInformation;
import org.junit.Test;

import io.konveyor.tackle.core.internal.testing.AbstractSymbolProviderTest;


public class MethodDeclarationSymbolProviderIntegrationTest extends AbstractSymbolProviderTest {

    @Test
    public void testFullyQualifiedMethodDeclarations() {
        // Test fully qualified method declaration search for SampleApplication.merge
        List<SymbolInformation> sampleAppResults = searchMethodDeclarations("io.konveyor.demo.SampleApplication.merge");
        printResults(sampleAppResults);
        assertTrue("[1] Should find merge() declaration in SampleApplication.java", 
                   sampleAppResults.size() == 1);
        assertTrue("[1b] Result should be in SampleApplication.java",
                   sampleAppResults.get(0).getLocation().getUri().contains("SampleApplication.java"));

        // Test fully qualified method declaration search for PackageUsageExample.merge
        List<SymbolInformation> packageExampleResults = searchMethodDeclarations("io.konveyor.demo.PackageUsageExample.merge");
        printResults(packageExampleResults);
        assertTrue("[2] Should find merge() declaration in PackageUsageExample.java", 
                   packageExampleResults.size() == 1);
        assertTrue("[2b] Result should be in PackageUsageExample.java",
                   packageExampleResults.get(0).getLocation().getUri().contains("PackageUsageExample.java"));

        // Test fully qualified method declaration search for ServletExample.merge
        List<SymbolInformation> servletResults = searchMethodDeclarations("io.konveyor.demo.ServletExample.merge");
        printResults(servletResults);
        assertTrue("[3] Should find merge() declaration in ServletExample.java", 
                   servletResults.size() == 1);
        assertTrue("[3b] Result should be in ServletExample.java",
                   servletResults.get(0).getLocation().getUri().contains("ServletExample.java"));

        // Half qualified method declaration DOES work (different from method calls)
        // This is because method declarations are matched by class name + method name
        List<SymbolInformation> halfQualifiedResults = searchMethodDeclarations("SampleApplication.merge");
        printResults(halfQualifiedResults);
        assertTrue("[4] Should find 1 result matching SampleApplication.merge (half-qualified works for declarations)", 
                   halfQualifiedResults.size() == 1);
        assertTrue("[4b] Result should be in SampleApplication.java",
                   halfQualifiedResults.get(0).getLocation().getUri().contains("SampleApplication.java"));

        // Test pattern matching with wildcard on class name
        List<SymbolInformation> patternResults = searchMethodDeclarations("io.konveyor.demo.*.merge");
        printResults(patternResults);
        assertTrue("[5] Should find all 3 merge() declarations matching io.konveyor.demo.*.merge", 
                   patternResults.size() == 3);
    }


    @Test
    public void testNonQualifiedMethodDeclarations() {
        // Search for all "merge" method declarations
        List<SymbolInformation> allResults = searchMethodDeclarations("merge");
        printResults(allResults);
        
        // Should find all 3 merge() declarations in the project
        assertTrue("[1] Should find all 3 merge() declarations in the project", 
                   allResults.size() == 3);

        // Verify each file has exactly one merge() declaration
        List<SymbolInformation> sampleAppResults = inFile(allResults, "SampleApplication.java");
        assertTrue("[2] Should find exactly 1 merge() declaration in SampleApplication.java", 
                   sampleAppResults.size() == 1);

        List<SymbolInformation> packageExampleResults = inFile(allResults, "PackageUsageExample.java");
        assertTrue("[3] Should find exactly 1 merge() declaration in PackageUsageExample.java", 
                   packageExampleResults.size() == 1);

        List<SymbolInformation> servletResults = inFile(allResults, "ServletExample.java");
        assertTrue("[4] Should find exactly 1 merge() declaration in ServletExample.java", 
                   servletResults.size() == 1);

        // Test pattern matching with wildcard
        List<SymbolInformation> mergeStarResults = searchMethodDeclarations("merg*");
        printResults(mergeStarResults);
        assertTrue("[5] Should find all 3 merge() declarations matching merg*", 
                   mergeStarResults.size() == 3);
    }


    @Test
    public void testMethodDeclarationPatterns() {
        // Test various pattern combinations
        
        // Pattern: method name ending with 'e'
        List<SymbolInformation> mergeResults = searchMethodDeclarations("*merge");
        printResults(mergeResults);
        assertTrue("[1] Should find all 3 merge() declarations matching *merge", 
                   mergeResults.size() == 3);

        // Test fully qualified pattern with class wildcard
        List<SymbolInformation> demoPackageResults = searchMethodDeclarations("io.konveyor.demo.Sample*.merge");
        printResults(demoPackageResults);
        assertTrue("[2] Should find merge() in SampleApplication matching io.konveyor.demo.Sample*.merge", 
                   demoPackageResults.size() == 1);

        // Test fully qualified pattern with method wildcard
        List<SymbolInformation> sampleAllMethods = searchMethodDeclarations("io.konveyor.demo.SampleApplication.*");
        printResults(sampleAllMethods);
        assertTrue("[3] Should find multiple method declarations in SampleApplication matching io.konveyor.demo.SampleApplication.*", 
                   sampleAllMethods.size() > 1);
    }
}
