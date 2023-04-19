package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.MethodDeclarationMatch;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;

public class MethodCallSymbolProvider implements SymbolProvider, WithQuery {
    private String query;
    
    @Override
    public List<SymbolInformation> get(SearchMatch match) {
        SymbolKind k = convertSymbolKind((IJavaElement) match.getElement());
        List<SymbolInformation> symbols = new ArrayList<>();
        // We need to find the Declartions for the given search, from here, we need to find the references.
        try {
            MethodDeclarationMatch m = (MethodDeclarationMatch) match;

            // Default to filter to only accurate matches
            var filterOut = m.getAccuracy() != SearchMatch.A_ACCURATE;
            if (query.contains("*")) {
                filterOut = false;
            }

            if (filterOut) {
                return symbols;
            }
            //IMethod e = (IMethod) m.getElement();
            IJavaElement e = (IJavaElement) m.getElement();
            SymbolInformation symbol = new SymbolInformation();
            symbol.setName(e.getElementName());
            symbol.setKind(convertSymbolKind(e));
            symbol.setContainerName(e.getParent().getElementName());
            Location location = JDTUtils.toLocation(e);
            if (location == null) {
                logInfo("Found match: "  + match + " must implement decompliation to get location correectly, skipping.");
            }
            symbol.setLocation(location);
            symbols.add(symbol);
        } catch (Exception e) {
            logInfo("unable to convert for variable: " + e);
        }

        return symbols;
    }
    @Override
    public void setQuery(String query) {
        this.query = query;
    }
}
