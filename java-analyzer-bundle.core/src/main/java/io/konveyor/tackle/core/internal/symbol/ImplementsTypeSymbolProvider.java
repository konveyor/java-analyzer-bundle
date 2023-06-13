package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.lsp4j.SymbolInformation;

public class ImplementsTypeSymbolProvider implements SymbolProvider {
    @Override
    public List<SymbolInformation> get(SearchMatch match) throws CoreException {
        List<SymbolInformation> symbols = new ArrayList<>();
        try {
            IType mod = (IType) match.getElement();
            // An interface can not impment another interface
            // this allows us to easily filter this search down.
            if (mod.isInterface()) {
                return null;
            }
            SymbolInformation symbol = new SymbolInformation();
            symbol.setName(mod.getElementName());
            symbol.setKind(convertSymbolKind(mod));
            symbol.setContainerName(mod.getParent().getElementName());
            symbol.setLocation(getLocation(mod, match));
            symbols.add(symbol);
        } catch (Exception e) {
            logInfo("unable to convert for implements type: " + e);
            return null;
        }
        return symbols;
    }
}
