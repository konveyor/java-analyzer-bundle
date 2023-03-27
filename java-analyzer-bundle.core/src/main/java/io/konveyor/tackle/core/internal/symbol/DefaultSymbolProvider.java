package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.lsp4j.SymbolInformation;

public class DefaultSymbolProvider implements SymbolProvider {
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

    @Override
    public List<SymbolInformation> get(SearchMatch match) throws CoreException {

        // Given a default search match, we have to ask each individual 
        List<SymbolInformation> symbols = new ArrayList<SymbolInformation>();
        for (int i=1; i < 10; i++) {
            SymbolProvider p = map.get(i);
            if(p instanceof DefaultSymbolProvider) {
                continue;
            }
            var specificSymbols = p.get(match);
            if (specificSymbols != null || !specificSymbols.isEmpty()) {
                symbols.addAll(specificSymbols);
            }
        }
        logInfo("got all symbols: " + symbols);
        return symbols;
    }
}
