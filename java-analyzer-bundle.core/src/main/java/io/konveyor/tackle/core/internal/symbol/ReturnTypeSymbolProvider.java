package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;

public class ReturnTypeSymbolProvider implements SymbolProvider, WithQuery {
    private String query;

    @Override
    public List<SymbolInformation> get(SearchMatch match) throws CoreException {
        List<SymbolInformation> symbols = new ArrayList<>();
        try {
            IMethod method = (IMethod) match.getElement();
            String signature = method.getReturnType();
            String[] strings = this.query.split("\\.");
            logInfo("signature: " + signature + "query: " + this.query);
            for (String string : strings) {
                // remove regex pattern match character
                String s = string.replaceAll("\\*", "");
                s = s.replaceAll("\\[", "");
                // check if the string found is apart of the signature.
                // TODO: Handle array cases. need to map [] to [ at the beginning.
                logInfo("signature: " + signature + "replaced string" + s);
                if (signature.contains(s)) {
                    logInfo(s);
                    SymbolInformation symbol = new SymbolInformation();
                    symbol.setName(method.getElementName());
                    symbol.setKind(convertSymbolKind(method));
                    symbol.setContainerName(method.getParent().getElementName());
                    Location location = JDTUtils.toLocation(method);
                    if (location == null) {
                        IClassFile classFile = method.getClassFile();
                        String packageName = classFile.getParent().getElementName();
                        String jarName = classFile.getParent().getParent().getElementName();
                        String uriString = new URI("jdt", "contents", JDTUtils.PATH_SEPARATOR + jarName + JDTUtils.PATH_SEPARATOR + packageName + JDTUtils.PATH_SEPARATOR + classFile.getElementName(), classFile.getHandleIdentifier(), null).toASCIIString();
                        if (uriString == null) {
                            uriString = method.getPath().toString();
                        }
                        Range range = JDTUtils.toRange(method.getOpenable(), method.getNameRange().getOffset(), method.getNameRange().getLength());
                        location = new Location(uriString, range);
                    }
                    symbol.setLocation(location);
                    symbols.add(symbol);
                    return symbols;
                }
            }
            return null;
        } catch (Exception e) {
            logInfo("unable to get for return type: " + e);
            return null;
        }
    }

    @Override
    public void setQuery(String query) {
        this.query = query;
    }
}
