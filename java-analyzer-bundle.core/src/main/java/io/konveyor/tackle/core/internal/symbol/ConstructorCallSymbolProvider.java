package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.MethodDeclarationMatch;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;

public class ConstructorCallSymbolProvider implements SymbolProvider {
    @Override
    public List<SymbolInformation> get(SearchMatch match) throws CoreException {
        List<SymbolInformation> symbols = new ArrayList<>();
        try {
            MethodDeclarationMatch m = (MethodDeclarationMatch) match;

            //IMethod e = (IMethod) m.getElement();
            IJavaElement e = (IJavaElement) m.getElement();
            SymbolInformation symbol = new SymbolInformation();
            symbol.setKind(SymbolKind.Constructor);
            symbol.setName(e.getElementName());
            symbol.setContainerName(e.getParent().getElementName());
            Location location = JDTUtils.toLocation(e);
            if (location == null) {
                logInfo("Found match: "  + match + " must implement decompliation to get location correectly, skipping.");
            }
            symbol.setLocation(location);
            symbols.add(symbol);
        } catch (Exception e) {
            logInfo("unable to get constructor: " + e);
            return null;
        }
        return symbols;
    }
}
