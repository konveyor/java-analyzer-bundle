package io.konveyor.tackle.core.internal.symbol;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

public class ImportSymbolProvider implements SymbolProvider {
    @Override
    public List<SymbolInformation> get(SearchMatch match) throws CoreException {
        List<SymbolInformation> symbols = new ArrayList<>();
        try {
            IImportDeclaration mod = (IImportDeclaration) match.getElement();
            logInfo("match: " + mod);
            SymbolInformation symbol = new SymbolInformation();
            symbol.setName(mod.getElementName());
            symbol.setKind((SymbolKind) match.getElement());
            symbol.setContainerName(mod.getParent().getElementName());
            symbol.setLocation(JDTUtils.toLocation(mod));
            symbols.add(symbol);
        } catch (Exception e) {
            logInfo("element:" + match.getElement() + " Unable to convert for package: " + e);
            return null;
        }
        return symbols;
    }
}
