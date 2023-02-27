package io.konveyor.tackle.core.internal.symbol;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.search.MethodReferenceMatch;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

public class MethodCallSymbolProvider implements SymbolProvider {
    @Override
    public List<SymbolInformation> get(SearchMatch match) {
        SymbolKind k = convertSymbolKind((IJavaElement) match.getElement());
        List<SymbolInformation> symbols = new ArrayList<>();
        // For Method Calls we will need to do the local variable trick
        try {
            logInfo("match: " + match);
            MethodReferenceMatch m = (MethodReferenceMatch) match;
            IMethod e = (IMethod) m.getElement();
            SymbolInformation symbol = new SymbolInformation();
            symbol.setName(e.getElementName());
            symbol.setKind(convertSymbolKind(e));
            symbol.setContainerName(e.getParent().getElementName());
            Location location = JDTUtils.toLocation(e);
            if (location == null) {
                IClassFile classFile = e.getClassFile();
                String packageName = classFile.getParent().getElementName();
                String jarName = classFile.getParent().getParent().getElementName();
                String uriString = new URI("jdt", "contents", JDTUtils.PATH_SEPARATOR + jarName + JDTUtils.PATH_SEPARATOR + packageName + JDTUtils.PATH_SEPARATOR + classFile.getElementName(), classFile.getHandleIdentifier(), null).toASCIIString();
                if (uriString == null) {
                    uriString = e.getPath().toString();
                }
                Range range = JDTUtils.toRange(e.getOpenable(), e.getNameRange().getOffset(), e.getNameRange().getLength());
                location = new Location(uriString, range);
            }
            symbol.setLocation(location);
            symbols.add(symbol);
        } catch (Exception e) {
            logInfo("match:" + match + " Unable to convert for variable: " + e);
        }

        return symbols;
    }
}
