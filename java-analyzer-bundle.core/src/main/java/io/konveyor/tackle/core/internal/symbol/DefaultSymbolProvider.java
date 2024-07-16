package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.lsp4j.SymbolInformation;

public class DefaultSymbolProvider implements SymbolProvider, WithQuery, WithMaxResults {
    private List<SymbolProvider> defaultProviders;

    public DefaultSymbolProvider() {
        this. defaultProviders= new ArrayList<SymbolProvider>();
        this. defaultProviders.add(new MethodCallSymbolProvider());
        this.defaultProviders.add(new ConstructorCallSymbolProvider());
        this.defaultProviders.add(new ImportSymbolProvider());
        this.defaultProviders.add( new TypeSymbolProvider());
    }

    private int maxResults; 
    private String query;

    @Override
    public List<SymbolInformation> get(SearchMatch match) throws CoreException {

        // Given a default search match, we have to ask each individual 
        List<SymbolInformation> symbols = new ArrayList<SymbolInformation>();
        for (SymbolProvider p: defaultProviders) {
            if (p instanceof WithQuery) {
                ((WithQuery) p).setQuery(this.query);
            }
            if (p instanceof WithMaxResults) {
                ((WithMaxResults) p).setMaxResultes(this.maxResults);
            }
            logInfo("default provider: " + p);
            var specificSymbols = p.get(match);
            if (specificSymbols == null || specificSymbols.isEmpty()) {
                continue;
            }
            
            symbols.addAll(specificSymbols);
            logInfo("got Symbols: " + symbols.size());
            // Have to handle here, the search matches can not ballon
            // for now this will be fine
            if (this.maxResults != 0 && symbols.size() >= this.maxResults) {
                return symbols;
            }
        }
        return symbols;
    }

    @Override
    public void setMaxResultes(int maxResults) {
        // For a given search match, we should not find more than 100.
        //TODO: come back and make this configurable.
        this.maxResults = 100;
    }

    @Override
    public void setQuery(String query) {
        this.query = query;
    }
}
