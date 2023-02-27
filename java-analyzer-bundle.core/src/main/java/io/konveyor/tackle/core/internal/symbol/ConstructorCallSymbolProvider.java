package io.konveyor.tackle.core.internal.symbol;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

public class ConstructorCallSymbolProvider implements SymbolProvider {
    @Override
    public List<SymbolInformation> get(SearchMatch match) throws CoreException {
        List<SymbolInformation> symbols = new ArrayList<>();
        try {
            IMethod mod = (IMethod) match.getElement();
            SymbolInformation symbol = new SymbolInformation();
            symbol.setName(mod.getElementName());
            symbol.setKind(convertSymbolKind((IJavaElement) match.getElement()));
            symbol.setContainerName(mod.getParent().getElementName());
            IClassFile classFile = mod.getClassFile();
            String packageName = classFile.getParent().getElementName();
            String jarName = classFile.getParent().getParent().getElementName();
            String uriString = new URI("jdt", "contents", JDTUtils.PATH_SEPARATOR + jarName + JDTUtils.PATH_SEPARATOR + packageName + JDTUtils.PATH_SEPARATOR + classFile.getElementName(), classFile.getHandleIdentifier(), null).toASCIIString();
            if (uriString == null) {
                uriString = mod.getPath().toString();
            }
            Range range = JDTUtils.toRange(mod.getOpenable(), mod.getNameRange().getOffset(), mod.getNameRange().getLength());
            Location loc = new Location(uriString, range);
            symbol.setLocation(loc);
            symbols.add(symbol);
        } catch (Exception e) {
            logInfo("Location: " + (JDTUtils.toLocation((JavaElement) match.getElement())));
            logInfo("unable to get method from symbol kind 2 and 3(method and constructor): " + e);
        }
        return symbols;
    }
}
