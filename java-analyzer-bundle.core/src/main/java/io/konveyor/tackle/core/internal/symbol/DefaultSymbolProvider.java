package io.konveyor.tackle.core.internal.symbol;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;

import java.util.List;

public class DefaultSymbolProvider implements SymbolProvider {
    @Override
    public List<SymbolInformation> get(SearchMatch match) throws CoreException {
        IJavaElement element = (IJavaElement) match.getElement();
        SymbolInformation symbol = new SymbolInformation();
        symbol.setName(element.getElementName());
        symbol.setKind(convertSymbolKind(element));
        symbol.setContainerName(element.getParent().getElementName());
        Location location = getLocation(element);
        if (location != null) {
            symbol.setLocation(location);
        }
        return List.of(symbol);
    }
}
