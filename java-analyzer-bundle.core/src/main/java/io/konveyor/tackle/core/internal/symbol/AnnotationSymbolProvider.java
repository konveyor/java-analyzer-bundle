package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.lsp4j.SymbolInformation;

public class AnnotationSymbolProvider implements SymbolProvider {
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
                symbol.setLocation(getLocation(element));
                symbols.add(symbol);
            }
            return symbols;
        } catch (Exception e) {
            logInfo("unable to match for annotations: " + e);
            return null;
        }
    }
}
