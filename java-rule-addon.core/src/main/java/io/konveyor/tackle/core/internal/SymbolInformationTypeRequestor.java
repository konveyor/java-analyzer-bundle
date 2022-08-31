package io.konveyor.tackle.core.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.Location;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.CoreException; 
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;

public class SymbolInformationTypeRequestor extends SearchRequestor {
    private List<SymbolInformation> symbols;
		private int maxResults;
		private boolean sourceOnly;
		private boolean isSymbolTagSupported;
		private IProgressMonitor monitor;

		public SymbolInformationTypeRequestor(List<SymbolInformation> symbols, int maxResults, IProgressMonitor monitor) {
			this.symbols = symbols;
			this.maxResults = maxResults;
			this.monitor = monitor;
		}
        

        @Override
        public void acceptSearchMatch(SearchMatch match) throws CoreException {

            if (maxResults > 0 && symbols.size() >= maxResults) {
                monitor.setCanceled(true);
                return;
            }
            
            // If we are not looking at files, then we don't want to return anytyhing for the match.
            if ((match.getResource().getType() | IResource.FILE) != 0 || match.getElement() == null) {
                return;

            }
            
            IJavaElement element = (IJavaElement)match.getElement();
            Location location = null;
            try {
                // This casting is safe or is assumed to be safer because the ToString on SearchMatch does it
                location = JDTUtils.toLocation(element);
            } catch (Exception e) {
                JavaLanguageServerPlugin.logException("Unable to determine location for " + match.getResource().getLocationURI(), e);
                return;
            }
            
            SymbolInformation symbol = new SymbolInformation();
            symbol.setName(element.getElementName());
            symbol.setKind(convertSymbolKind(element));
            symbol.setContainerName(element.getParent().getElementName());
            symbol.setLocation(location);
            symbols.add(symbol);
        }

        private SymbolKind convertSymbolKind(IJavaElement element) {
            switch (element.getElementType()) {
            case IJavaElement.TYPE:
                try {
                    IType type = (IType)element;
                    if (type.isInterface()) {
                        return SymbolKind.Interface;
                    }
                    else if (type.isEnum()) {
                        return SymbolKind.Enum;
                    }
                } catch (JavaModelException ignore) {
                }
                return SymbolKind.Class;
            case IJavaElement.ANNOTATION:
                return SymbolKind.Property; // TODO: find a better mapping
            case IJavaElement.CLASS_FILE:
            case IJavaElement.COMPILATION_UNIT:
                return SymbolKind.File;
            case IJavaElement.FIELD:
                IField field = (IField) element;
                try {
                    if (field.isEnumConstant()) {
                        return SymbolKind.EnumMember;
                    }
                    int flags = field.getFlags();
                    if (Flags.isStatic(flags) && Flags.isFinal(flags)) {
                        return SymbolKind.Constant;
                    }
                } catch (JavaModelException ignore) {
                }
                return SymbolKind.Field;
            case IJavaElement.IMPORT_CONTAINER:
            case IJavaElement.IMPORT_DECLARATION:
                //should we return SymbolKind.Namespace?
            case IJavaElement.JAVA_MODULE:
                return SymbolKind.Module;
            case IJavaElement.INITIALIZER:
                return SymbolKind.Constructor;
            case IJavaElement.LOCAL_VARIABLE:
                return SymbolKind.Variable;
            case IJavaElement.TYPE_PARAMETER:
                return SymbolKind.TypeParameter;
            case IJavaElement.METHOD:
                try {
                    // TODO handle `IInitializer`. What should be the `SymbolKind`?
                    if (element instanceof IMethod) {
                        if (((IMethod) element).isConstructor()) {
                            return SymbolKind.Constructor;
                        }
                    }
                    return SymbolKind.Method;
                } catch (JavaModelException e) {
                    return SymbolKind.Method;
                }
            case IJavaElement.PACKAGE_DECLARATION:
                return SymbolKind.Package;
        }
        return SymbolKind.String;
    }
}
