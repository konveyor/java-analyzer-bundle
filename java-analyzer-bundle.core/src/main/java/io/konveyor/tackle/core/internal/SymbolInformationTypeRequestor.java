package io.konveyor.tackle.core.internal;

import io.konveyor.tackle.core.internal.symbol.SymbolProvider;
import io.konveyor.tackle.core.internal.symbol.SymbolProviderResolver;
import io.konveyor.tackle.core.internal.symbol.WithQuery;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.lsp4j.SymbolInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

public class SymbolInformationTypeRequestor extends SearchRequestor {
    private List<SymbolInformation> symbols;
    private int maxResults;
    private boolean sourceOnly;
    private boolean isSymbolTagSupported;
    private IProgressMonitor monitor;
    private int symbolKind;
    private String query;

    public SymbolInformationTypeRequestor(List<SymbolInformation> symbols, int maxResults, IProgressMonitor monitor, int symbolKind, String query) {
        this.symbols = symbols;
        this.maxResults = maxResults;
        this.monitor = monitor;
        this.symbolKind = symbolKind;
        this.query = query;
    }


    @Override
    public void acceptSearchMatch(SearchMatch match) throws CoreException {
        if (maxResults > 0 && symbols.size() >= maxResults) {
            monitor.setCanceled(true);
            logInfo("maxResults > 0 && symbols.size() >= maxResults");
            return;
        }
        // If we are not looking at files, then we don't want to return anytyhing for the match.
        //logInfo("getResource().getType()" + match.getResource().getType());
        if ((match.getResource().getType() | IResource.FILE) == 0 || match.getElement() == null) {
            logInfo("match.getResource().getType() | IResource.FILE");
            return;

        }
//        SymbolKind k = convertSymbolKind(element);
//        logInfo("symbolKind: " + k + " kind passed in " + this.symbolKind);

        SymbolProvider symbolProvider = SymbolProviderResolver.resolve(this.symbolKind);
        if (symbolProvider instanceof WithQuery) {
            ((WithQuery) symbolProvider).setQuery(this.query);
        }

        List<SymbolInformation> symbols = Optional.ofNullable(symbolProvider.get(match)).orElse(new ArrayList<>());
        this.symbols.addAll(symbols);

    }

    public List<SymbolInformation> getSymbols() {
        return this.symbols;
    }
}
