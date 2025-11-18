package integration

import (
	"strings"

	"github.com/konveyor/analyzer-lsp/lsp/protocol"
)

// verifySymbolInResults checks if a symbol with expected name exists in results
func verifySymbolInResults(symbols []protocol.SymbolInformation, expectedName string, expectedKind ...protocol.SymbolKind) bool {
	for _, symbol := range symbols {
		if symbol.Name == expectedName {
			// If kind specified, verify it matches
			if len(expectedKind) > 0 && symbol.Kind != expectedKind[0] {
				return false
			}
			return true
		}
	}
	return false
}

// verifySymbolLocationContains checks if a symbol exists and its location contains expected file
func verifySymbolLocationContains(symbols []protocol.SymbolInformation, expectedName, expectedFile string) bool {
	for _, symbol := range symbols {
		if symbol.Name == expectedName {
			if strings.Contains(string(symbol.Location.URI), expectedFile) {
				return true
			}
		}
	}
	return false
}
