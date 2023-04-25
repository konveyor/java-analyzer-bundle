package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
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
            var location = getLocation(mod, match);
            if (location == null) {
                IClassFile classFile = mod.getClassFile();
                String packageName = classFile.getParent().getElementName();
                String jarName = classFile.getParent().getParent().getElementName();
                String uriString = new URI("jdt", "contents", JDTUtils.PATH_SEPARATOR + jarName + JDTUtils.PATH_SEPARATOR + packageName + JDTUtils.PATH_SEPARATOR + classFile.getElementName(), classFile.getHandleIdentifier(), null).toASCIIString();
                if (uriString == null) {
                    uriString = mod.getPath().toString();
                }
                Range range = JDTUtils.toRange(mod.getOpenable(), mod.getNameRange().getOffset(), mod.getNameRange().getLength());
                location = new Location(uriString, range);
            }
            symbol.setLocation(location);
            symbols.add(symbol);
        } catch (Exception e) {
            logInfo("unable to convert for implements type: " + e);
            return null;
        }
        return symbols;
    }
}
