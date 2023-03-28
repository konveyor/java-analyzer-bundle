package io.konveyor.tackle.core.internal.symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.lsp4j.SymbolInformation;

public class DefaultSymbolProvider implements SymbolProvider, WithQuery, WithMaxResults {
    private static Map<Integer, SymbolProvider> map;
    static {
        map = new HashMap<>();
        map.put(1, new InheritanceSymbolProvider());
        map.put(2, new MethodCallSymbolProvider());
        map.put(3, new ConstructorCallSymbolProvider());
        map.put(4, new AnnotationSymbolProvider());
        map.put(5, new ImplementsTypeSymbolProvider());
        map.put(6, new DefaultSymbolProvider());
        map.put(7, new ReturnTypeSymbolProvider());
        map.put(8, new ImportSymbolProvider());
        map.put(9, new VariableDeclarationSymbolProvider());
        map.put(10, new TypeSymbolProvider());
    }

    private int maxResults; 
    private String query;

    @Override
    public List<SymbolInformation> get(SearchMatch match) throws CoreException {

        // Given a default search match, we have to ask each individual 
        List<SymbolInformation> symbols = new ArrayList<SymbolInformation>();
        for (int i=1; i < 10; i++) {
            SymbolProvider p = map.get(i);
            if(p instanceof DefaultSymbolProvider) {
                continue;
            }
            if (p instanceof WithQuery) {
                ((WithQuery) p).setQuery(this.query);
            }
            if (p instanceof WithMaxResults) {
                ((WithMaxResults) p).setMaxResultes(this.maxResults);
            }
            var specificSymbols = p.get(match);
            if (specificSymbols == null) {
                continue;
            }
            
            symbols.addAll(specificSymbols);
            // Have to handle here, the search matches can not ballon
            // for now this will be fine
            if (symbols.size() >= this.maxResults) {
                return symbols;
            }
        }
        return symbols;
    }

    @Override
    public void setMaxResultes(int maxResults) {
        this.maxResults = maxResults;
    }

    @Override
    public void setQuery(String query) {
        this.query = query;
    }
}
