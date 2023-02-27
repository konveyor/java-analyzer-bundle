package io.konveyor.tackle.core.internal.symbol;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
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

public class InheritanceSymbolProvider implements SymbolProvider {
    @Override
    public List<SymbolInformation> get(SearchMatch match) {
        SymbolKind k = convertSymbolKind((IJavaElement) match.getElement());
        List<SymbolInformation> symbols = new ArrayList<>();
        try {
            IType mod = (IType)match.getElement();
            // Only add things that have a super class. This is an itermediate filtration.
            // TODO: add ability to find the actual super type and find the correct type of object (interface or class) based on super
            // TODO: I think that this is starting to get to the point where more than one symbol acceptor is going to be useful.
            logInfo("mod: " + mod + " superclass name: " + mod.getSuperclassName());
            if (mod.isClass() && (mod.getSuperclassName() == null || mod.getSuperclassName() == "java.lang.Object")) {
                return null;
            }
            if (mod.isInterface() && mod.getSuperInterfaceNames().length == 0) {
                return null;
            }
            SymbolInformation symbol = new SymbolInformation();
            symbol.setName(mod.getElementName());
            symbol.setKind(k);
            symbol.setContainerName(mod.getParent().getElementName());
            Location location = JDTUtils.toLocation(mod);
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
            logInfo("element:" + match.getElement() + " Unable to convert for inheritance: " + e);
        }
        return symbols;

    }
}
