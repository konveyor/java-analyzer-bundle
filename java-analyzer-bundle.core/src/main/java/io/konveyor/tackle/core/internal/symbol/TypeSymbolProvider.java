package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.TypeDeclarationMatch;
import org.eclipse.jdt.core.search.TypeParameterDeclarationMatch;
import org.eclipse.jdt.core.search.TypeParameterReferenceMatch;
import org.eclipse.jdt.core.search.TypeReferenceMatch;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;

public class TypeSymbolProvider implements SymbolProvider {
    @Override
    public List<SymbolInformation> get(SearchMatch match) {
        SymbolKind k = convertSymbolKind((IJavaElement) match.getElement());
        List<SymbolInformation> symbols = new ArrayList<>();
        // For Method Calls we will need to do the local variable trick
        if (!(match instanceof TypeReferenceMatch || 
            match instanceof TypeDeclarationMatch || 
            match instanceof TypeParameterDeclarationMatch || 
            match instanceof TypeParameterReferenceMatch)) {
                return null;
            }
        try {
            var mod = (IJavaElement) match.getElement();
            SymbolInformation symbol = new SymbolInformation();
            symbol.setName(mod.getElementName());
            symbol.setKind(convertSymbolKind(mod));
            symbol.setContainerName(mod.getParent().getElementName());
            Location location = JDTUtils.toLocation(mod);
            if (location == null) {
                return null;
            /// TODO: We should be able to find this but need to figure out how to get the Class File
            // This will also change the output and could be considered breaking and will require analyzer test updates
            //     IClassFile classFile = mod.getClassFile();
            //     String packageName = classFile.getParent().getElementName();
            //     String jarName = classFile.getParent().getParent().getElementName();
            //     String uriString = new URI("jdt", "contents", JDTUtils.PATH_SEPARATOR + jarName + JDTUtils.PATH_SEPARATOR + packageName + JDTUtils.PATH_SEPARATOR + classFile.getElementName(), classFile.getHandleIdentifier(), null).toASCIIString();
            //     if (uriString == null) {
            //         uriString = mod.getPath().toString();
            //     }
            //     Range range = JDTUtils.toRange(mod.getOpenable(), match.getOffset(), match.getLength());
            //    location = new Location(uriString, range);
            }
            symbol.setLocation(location);
            symbols.add(symbol);
            

        } catch (Exception e) {
            logInfo("Unable to convert for TypeSymbolProvider: " + e);
            return null;
        }

        return symbols;
    }
}
