package integration

import (
	"fmt"
	"os"
	"testing"

	"github.com/konveyor/java-analyzer-bundle/integration/client"
)

var jdtlsClient *client.JDTLSClient

// TestMain sets up and tears down the JDT.LS client for all tests
func TestMain(m *testing.M) {
	// Get paths from environment or use defaults
	jdtlsPath := os.Getenv("JDTLS_PATH")
	if jdtlsPath == "" {
		jdtlsPath = "/jdtls"
	}

	workspaceDir := os.Getenv("WORKSPACE_DIR")
	if workspaceDir == "" {
		workspaceDir = "/workspace"
	}
	// Create and start JDT.LS client
	jdtlsClient = client.NewJDTLSClient(jdtlsPath, workspaceDir)

	if err := jdtlsClient.Start(); err != nil {
		fmt.Fprintf(os.Stderr, "FATAL ERROR: Failed to start JDT.LS: %v\n", err)
		os.Exit(1)
	}

	// Initialize LSP connection
	if _, err := jdtlsClient.Initialize(); err != nil {
		fmt.Fprintf(os.Stderr, "FATAL ERROR: Failed to initialize JDT.LS: %v\n", err)
		jdtlsClient.Close()
		os.Exit(1)
	}
	// Run tests
	code := m.Run()

	// Cleanup
	if err := jdtlsClient.Close(); err != nil {
		fmt.Fprintf(os.Stderr, "Warning: Failed to close JDT.LS cleanly: %v\n", err)
	}

	os.Exit(code)
}

// TestInheritanceSearch tests inheritance search (location type 1)
func TestInheritanceSearch(t *testing.T) {
	testCases := []struct {
		Name             string
		projectName      string
		query            string
		location         int
		analysisMode     string
		includedPaths    []string
		exceptedFileName string
	}{
		{
			Name:             "Find SampleApplication extends BaseService",
			projectName:      "test-project",
			query:            "io.konveyor.demo.inheritance.BaseService",
			location:         1,
			analysisMode:     "source-only",
			exceptedFileName: "SampleApplication",
		},
		{
			Name:             "Find DataService extends BaseService",
			projectName:      "test-project",
			query:            "io.konveyor.demo.inheritance.BaseService",
			location:         1,
			analysisMode:     "source-only",
			exceptedFileName: "SampleApplication",
		},
		{
			Name:             "Find CustomException extends Exception",
			projectName:      "test-project",
			query:            "java.lang.Exception",
			location:         1,
			analysisMode:     "source-only",
			exceptedFileName: "CustomException",
		},
	}

	for _, tc := range testCases {
		t.Run(tc.Name, func(t *testing.T) {
			symbols, err := jdtlsClient.SearchSymbols(tc.projectName, tc.query, tc.location, tc.analysisMode, tc.includedPaths)
			if err != nil {
				t.Fatalf("Search failed: %v", err)
			}

			if !verifySymbolInResults(symbols, tc.exceptedFileName) {
				t.Errorf("SampleApplication not found in results")
			}
		})
	}
}

// TestMethodCallSearch tests method call search (location type 2)
func TestMethodCallSearch(t *testing.T) {
	t.Run("Find println calls", func(t *testing.T) {
		symbols, err := jdtlsClient.SearchSymbols("test-project", "println(*)", 2, "source-only", nil)
		if err != nil {
			t.Fatalf("Search failed: %v", err)
		}

		count := len(symbols)
		if count == 0 {
			t.Errorf("No println calls found")
		} else {
			t.Logf("Found %d println calls", count)
		}
	})

	t.Run("Find List.add in SampleApplication", func(t *testing.T) {
		symbols, err := jdtlsClient.SearchSymbols("test-project", "add(*)", 2, "source-only", nil)
		if err != nil {
			t.Fatalf("Search failed: %v", err)
		}

		if !verifySymbolLocationContains(symbols, "processData", "SampleApplication") {
			t.Errorf("add method not found in SampleApplication, got %d results", len(symbols))
		}
	})
}

// TestConstructorCallSearch tests constructor call search (location type 3)
func TestConstructorCallSearch(t *testing.T) {
	t.Run("Find ArrayList instantiations", func(t *testing.T) {
		symbols, err := jdtlsClient.SearchSymbols("test-project", "java.util.ArrayList", 3, "source-only", nil)
		if err != nil {
			t.Fatalf("Search failed: %v", err)
		}

		t.Logf("symbols: %#v", symbols)

		if len(symbols) == 0 {
			t.Errorf("No ArrayList constructors found")
		} else {
			t.Logf("Found %d ArrayList instantiations", len(symbols))
		}
	})

	t.Run("Find File instantiations", func(t *testing.T) {
		symbols, err := jdtlsClient.SearchSymbols("test-project", "java.io.File", 3, "source-only", nil)
		if err != nil {
			t.Fatalf("Search failed: %v", err)
		}

		count := len(symbols)
		if count == 0 {
			t.Errorf("No File constructors found")
		} else {
			t.Logf("Found %d File instantiations", count)
		}
	})
}

// TestAnnotationSearch tests annotation search (location type 4)
func TestAnnotationSearch(t *testing.T) {
	t.Run("Find @CustomAnnotation on SampleApplication", func(t *testing.T) {
		symbols, err := jdtlsClient.SearchSymbols("test-project", "io.konveyor.demo.annotations.CustomAnnotation", 4, "source-only", nil)
		if err != nil {
			t.Fatalf("Search failed: %v", err)
		}

		// Annotation symbols have the annotation name, and container is the annotated element
		// We expect to find CustomAnnotation with container "SampleApplication"
		found := false
		for _, symbol := range symbols {
			if symbol.Name == "CustomAnnotation" && symbol.ContainerName == "SampleApplication" {
				found = true
				break
			}
		}
		if !found {
			t.Errorf("CustomAnnotation not found on SampleApplication class, got %d results", len(symbols))
		}
	})
}

// TestImplementsTypeSearch tests implements type search (location type 5)
func TestImplementsTypeSearch(t *testing.T) {
	t.Run("Find BaseService implements Serializable", func(t *testing.T) {
		symbols, err := jdtlsClient.SearchSymbols("test-project", "java.io.Serializable", 5, "source-only", nil)
		if err != nil {
			t.Fatalf("Search failed: %v", err)
		}

		if !verifySymbolInResults(symbols, "BaseService") {
			t.Errorf("BaseService not found, got %d results", len(symbols))
		}
	})
}

// TestImportSearch tests import search (location type 8)
func TestImportSearch(t *testing.T) {
	t.Run("Find java.io.File imports", func(t *testing.T) {
		symbols, err := jdtlsClient.SearchSymbols("test-project", "java.io.File", 8, "source-only", nil)
		if err != nil {
			t.Fatalf("Search failed: %v", err)
		}

		count := len(symbols)
		if count == 0 {
			t.Errorf("No java.io.File imports found")
		} else {
			t.Logf("Found %d java.io.File imports", count)
		}
	})
}

// TestTypeSearch tests type search (location type 10)
func TestTypeSearch(t *testing.T) {
	t.Run("Find ArrayList type references", func(t *testing.T) {
		// ArrayList has explicit imports in SampleApplication, so this should work
		symbols, err := jdtlsClient.SearchSymbols("test-project", "java.util.ArrayList", 10, "source-only", nil)
		if err != nil {
			t.Fatalf("Search failed: %v", err)
		}

		count := len(symbols)
		if count == 0 {
			t.Errorf("No ArrayList type references found")
		} else {
			t.Logf("Found %d ArrayList type references", count)
		}
	})
}

// TestClassDeclarationSearch tests class declaration search (location type 14)
func TestClassDeclarationSearch(t *testing.T) {
	t.Run("Find SampleApplication class", func(t *testing.T) {
		symbols, err := jdtlsClient.SearchSymbols("test-project", "SampleApplication", 14, "source-only", nil)
		if err != nil {
			t.Fatalf("Search failed: %v", err)
		}

		if !verifySymbolInResults(symbols, "SampleApplication") {
			t.Errorf("SampleApplication class not found")
		}
	})
}

// TestCustomersTomcatLegacy tests searches against the customers-tomcat-legacy project
func TestCustomersTomcatLegacy(t *testing.T) {
	t.Run("Find @Entity on Customer", func(t *testing.T) {
		symbols, err := jdtlsClient.SearchSymbols("customers-tomcat", "javax.persistence.Entity", 4, "source-only", nil)
		if err != nil {
			t.Fatalf("Search failed: %v", err)
		}

		// Annotation symbols have the annotation name, and container is the annotated element
		found := false
		for _, symbol := range symbols {
			if symbol.Name == "Entity" && symbol.ContainerName == "Customer" {
				found = true
				break
			}
		}
		if !found {
			t.Errorf("@Entity not found on Customer, got %d results", len(symbols))
		}
	})

	t.Run("Find @Service on CustomerService", func(t *testing.T) {
		symbols, err := jdtlsClient.SearchSymbols("customers-tomcat", "org.springframework.stereotype.Service", 4, "source-only", nil)
		if err != nil {
			t.Fatalf("Search failed: %v", err)
		}

		// Annotation symbols have the annotation name, and container is the annotated element
		found := false
		for _, symbol := range symbols {
			if symbol.Name == "Service" && symbol.ContainerName == "CustomerService" {
				found = true
				break
			}
		}
		if !found {
			t.Errorf("@Service not found on CustomerService, got %d results", len(symbols))
		}
	})

	t.Run("Find javax.persistence.Entity imports (migration target)", func(t *testing.T) {
		// Search for specific import instead of wildcard, since the project has specific imports
		symbols, err := jdtlsClient.SearchSymbols("customers-tomcat", "javax.persistence.Entity", 8, "source-only", nil)
		if err != nil {
			t.Fatalf("Search failed: %v", err)
		}

		count := len(symbols)
		if count == 0 {
			t.Errorf("No javax.persistence.Entity imports found - migration target not detected")
		} else {
			t.Logf("Found %d javax.persistence.Entity imports", count)
		}
	})
}
