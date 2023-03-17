package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.TypeReferenceMatch;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.lsp4j.SymbolInformation;

public class VariableDeclarationSymbolProvider implements SymbolProvider {
    @Override
    public List<SymbolInformation> get(SearchMatch match) throws CoreException {
        List<SymbolInformation> symbols = new ArrayList<>();
        try {
            TypeReferenceMatch m = (TypeReferenceMatch) match;
            ILocalVariable var = (ILocalVariable) m.getLocalElement();
            if (var == null) {
                return null;
            }
            SymbolInformation symbol = new SymbolInformation();
            symbol.setName(var.getElementName());
            symbol.setKind(convertSymbolKind(var));
            symbol.setContainerName(var.getParent().getElementName());
            symbol.setLocation(JDTUtils.toLocation(var));
            symbols.add(symbol);
        } catch (Exception e) {
            logInfo("match:" + match + " Unable to convert for variable: " + e);
            return null;
        }
        return symbols;
    }
}
