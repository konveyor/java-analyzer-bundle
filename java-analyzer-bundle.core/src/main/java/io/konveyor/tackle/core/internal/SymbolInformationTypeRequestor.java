package io.konveyor.tackle.core.internal;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.lsp4j.SymbolInformation;

import io.konveyor.tackle.core.internal.symbol.SymbolProvider;
import io.konveyor.tackle.core.internal.symbol.SymbolProviderResolver;
import io.konveyor.tackle.core.internal.symbol.WithMaxResults;
import io.konveyor.tackle.core.internal.symbol.WithQuery;

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
        if (maxResults == 0) {
            this.maxResults = 10000;
        }
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
        if ((!this.query.contains("?") && !this.query.contains("*")) && match.getAccuracy() == SearchMatch.A_INACCURATE) {
            var e = (IJavaElement) match.getElement();
            //TODO: This is a hack, this will give use some clue of what we are looking at, if the search is exact then this should match
            // I don't love this, but seems to be the right way
            logInfo("attempting: " + e.getHandleIdentifier());
            // Adding specific case for annotations, they will always be inaccurrate.
            if (!e.getHandleIdentifier().contains(query) && !(this.symbolKind == 4 || this.symbolKind == 5 || this.symbolKind == 1)) {
                logInfo("exact match is looking for accurate results" + match);
                return;
            }
        }
        
        if ((this.query.contains("?") && (this.query.contains("(") || this.query.contains(")"))) && match.getAccuracy() == SearchMatch.A_INACCURATE) {
            if(!this.query.contains("*")) {
                logInfo("exact match is looking for accurate results " + match);
                return;
            }
        }

        SymbolProvider symbolProvider = SymbolProviderResolver.resolve(this.symbolKind, match);
        if (symbolProvider instanceof WithQuery) {
            ((WithQuery) symbolProvider).setQuery(this.query);
        }
        if (symbolProvider instanceof WithMaxResults) {
            ((WithMaxResults) symbolProvider).setMaxResultes(this.maxResults);
        }

        logInfo("getting match: " + match + "with provider: " + symbolProvider);
        List<SymbolInformation> symbols = Optional.ofNullable(symbolProvider.get(match)).orElse(new ArrayList<>());
        this.symbols.addAll(symbols);

    }

    public List<SymbolInformation> getSymbols() {
        return this.symbols;
    }
}
