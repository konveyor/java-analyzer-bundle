package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.lsp4j.SymbolInformation;

public class ReturnTypeSymbolProvider implements SymbolProvider, WithQuery {
    private String query;

    @Override
    public List<SymbolInformation> get(SearchMatch match) throws CoreException {
        List<SymbolInformation> symbols = new ArrayList<>();
        try {
            IMethod method = (IMethod) match.getElement();
            String signature = method.getReturnType();
            // Convert JVM type signature to readable type name (e.g., "I" -> "int")
            String readableType = Signature.toString(signature);

            String[] strings = this.query.split("\\.");
            logInfo("signature: " + signature + " readable: " + readableType + " query: " + this.query);
            for (String string : strings) {
                // remove regex pattern match character
                String s = string.replaceAll("\\*", "");
                s = s.replaceAll("\\[", "");
                // check if the string found is apart of the signature or readable type
                // TODO: Handle array cases. need to map [] to [ at the beginning.
                logInfo("signature: " + signature + " readable: " + readableType + " replaced string: " + s);
                if (signature.contains(s) || readableType.contains(s)) {
                    logInfo(s);
                    SymbolInformation symbol = new SymbolInformation();
                    symbol.setName(method.getElementName());
                    symbol.setKind(convertSymbolKind(method));
                    symbol.setContainerName(method.getParent().getElementName());
                    symbol.setLocation(getLocation(method, match));
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
