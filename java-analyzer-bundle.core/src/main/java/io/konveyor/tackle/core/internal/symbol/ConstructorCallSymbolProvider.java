package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.search.MethodReferenceMatch;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;

public class ConstructorCallSymbolProvider implements SymbolProvider {
    @Override
    public List<SymbolInformation> get(SearchMatch match) throws CoreException {
        List<SymbolInformation> symbols = new ArrayList<>();
        var el = (JavaElement) match.getElement();
        try {
            MethodReferenceMatch m = (MethodReferenceMatch) match;
            var mod  = (IMethod) m.getElement();
            SymbolInformation symbol = new SymbolInformation();
            symbol.setName(mod.getElementName());
            // If the search match is for a constructor, the enclosing element may not be a constructor.
            if (m.isConstructor()) {
                symbol.setKind(SymbolKind.Constructor);
            } else {
                logInfo("Method reference was not a constructor, skipping");
                return null;
            }
            symbol.setContainerName(mod.getParent().getElementName());
            symbol.setLocation(getLocation(mod, match));
            symbols.add(symbol);
        } catch (Exception e) {
            logInfo("unable to get constructor: " + e);
            return null;
        }
        return symbols;
    }
}
