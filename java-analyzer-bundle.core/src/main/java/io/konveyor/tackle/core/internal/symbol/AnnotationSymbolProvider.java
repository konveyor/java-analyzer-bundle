package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import io.konveyor.tackle.core.internal.query.AnnotationQuery;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.internal.core.ResolvedSourceField;
import org.eclipse.jdt.internal.core.ResolvedSourceMethod;
import org.eclipse.jdt.internal.core.ResolvedSourceType;
import org.eclipse.jdt.internal.core.SourceRefElement;
import org.eclipse.lsp4j.SymbolInformation;

public class AnnotationSymbolProvider implements SymbolProvider, WithAnnotationQuery {

    private AnnotationQuery annotationQuery;

    @Override
    public List<SymbolInformation> get(SearchMatch match) throws CoreException {
        List<SymbolInformation> symbols = new ArrayList<>();
        try {
            IAnnotatable mod = (IAnnotatable) match.getElement();
            IJavaElement element = (IJavaElement) match.getElement();
            for (IAnnotation annotation : mod.getAnnotations()) {
                SymbolInformation symbol = new SymbolInformation();
                symbol.setName(annotation.getElementName());
                symbol.setKind(convertSymbolKind(element));
                symbol.setContainerName(annotation.getParent().getElementName());
                symbol.setLocation(getLocation(element, match));

                if (annotationQuery != null) {
                    List<Class<? extends SourceRefElement>> classes = new ArrayList<>();
                    classes.add(ResolvedSourceMethod.class);
                    classes.add(ResolvedSourceField.class);
                    classes.add(ResolvedSourceType.class);
                    if (matchesAnnotationQuery(match, classes)) {
                        symbols.add(symbol);
                    }
                } else {
                    symbols.add(symbol);
                }
            }
            return symbols;
        } catch (Exception e) {
            logInfo("unable to match for annotations: " + e);
            return null;
        }
    }

    public AnnotationQuery getAnnotationQuery() {
        return annotationQuery;
    }

    public void setAnnotationQuery(AnnotationQuery annotationQuery) {
        this.annotationQuery = annotationQuery;
    }
}
