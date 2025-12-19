package io.konveyor.tackle.core.internal.symbol;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.lsp4j.SymbolInformation;
import org.junit.Test;

import io.konveyor.tackle.core.internal.testing.AbstractSymbolProviderTest;


public class MethodCallSymbolProviderIntegrationTest extends AbstractSymbolProviderTest {

    @Test
    public void testFullyQualifiedMethodCalls() {
        List<SymbolInformation> results = searchMethodCalls("io.konveyor.demo.PackageUsageExample.merge");
        results = inFile(results, "SampleApplication.java");
        printResults(results);        
        assertTrue("[1] Should find usage of merge() call in test-project/SampleApplication.java#callFullyQualifiedMethod()", 
                    results.size() == 1);
    
        List<SymbolInformation> entityManagerMergeResults = searchMethodCalls("javax.persistence.EntityManager.merge");
        entityManagerMergeResults = inFile(entityManagerMergeResults, "PackageUsageExample.java");
        printResults(entityManagerMergeResults);
        assertTrue("[2] Should find usage of entityManager.merge() call in test-project/PackageUsageExample.java#merge()", 
                    entityManagerMergeResults.size() == 1);

        // Half qualified method call should not work
        List<SymbolInformation> halfQualifiedResults = searchMethodCalls("PackageUsageExample.merge");
        printResults(halfQualifiedResults);
        assertTrue("[3] Should not find any results matching PackageUsageExample.merge", 
                   halfQualifiedResults.size() == 0);

        // make sure patterns for fully qualified method calls work
        List<SymbolInformation> mergeStarResults = searchMethodCalls("io.konveyor.demo.PackageUsageExample.merg*");
        printResults(mergeStarResults);
        assertTrue("[4] Should find 1 result matching io.konveyor.demo.PackageUsageExample.merg*", 
                   mergeStarResults.size() == 1);
    }


    @Test
    public void testNonQualifiedMethodCalls() {
        List<SymbolInformation> allResults = searchMethodCalls("merge");
        
        List<SymbolInformation> sampleAppResults = inFile(allResults, "SampleApplication.java");
        printResults(sampleAppResults);
        assertTrue("[1] Should find usage of merge() call in test-project/SampleApplication.java#callFullyQualifiedMethod()", 
                   sampleAppResults.size() == 1);
        
        List<SymbolInformation> packageExampleResults = inFile(allResults, "PackageUsageExample.java");
        printResults(packageExampleResults);
        assertTrue("[2] Should find usage of merge() call in test-project/PackageUsageExample.java#merge()", 
                   packageExampleResults.size() == 1);

        // make sure pattterns work
        List<SymbolInformation> mergeStarResults = searchMethodCalls("merg*");
        printResults(mergeStarResults);
        assertTrue("[3] Should find usage of merge() call in test-project/SampleApplication.java#callFullyQualifiedMethod() and test-project/PackageUsageExample.java#merge()", 
                   mergeStarResults.size() == 2);
    }
}
