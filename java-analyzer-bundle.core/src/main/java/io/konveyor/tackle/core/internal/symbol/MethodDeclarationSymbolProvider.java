package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import io.konveyor.tackle.core.internal.query.AnnotationQuery;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.MethodDeclarationMatch;
import org.eclipse.jdt.internal.core.ResolvedSourceMethod;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.SourceRefElement;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;

public class MethodDeclarationSymbolProvider implements SymbolProvider, WithQuery, WithAnnotationQuery {
    private String query;
    private AnnotationQuery annotationQuery;

    public List<SymbolInformation> get(SearchMatch match) {
        SymbolKind k = convertSymbolKind((IJavaElement) match.getElement());
        List<SymbolInformation> symbols = new ArrayList<>();
        try {
            MethodDeclarationMatch m = (MethodDeclarationMatch) match;
            IMethod e = (IMethod) m.getElement();
            SymbolInformation symbol = new SymbolInformation();
            symbol.setName(e.getElementName());
            symbol.setKind(convertSymbolKind(e));
            symbol.setContainerName(e.getParent().getElementName());
            symbol.setLocation(getLocation(e, match));

            List<Class<? extends SourceRefElement>> classes = new ArrayList<>();
            classes.add(ResolvedSourceMethod.class);
            classes.add(SourceMethod.class);
            if (matchesAnnotationQuery(match, classes)) {
                symbols.add(symbol);
            }
        } catch (Exception e) {
            logInfo("unable to convert for variable: " + e);
        }

        return symbols;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public AnnotationQuery getAnnotationQuery() {
        return this.annotationQuery;
    }

    public void setAnnotationQuery(AnnotationQuery annotationQuery) {
        this.annotationQuery = annotationQuery;
    }

}
