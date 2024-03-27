package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.URIUtil;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.TypeDeclarationMatch;
import org.eclipse.jdt.core.search.TypeParameterDeclarationMatch;
import org.eclipse.jdt.core.search.TypeParameterReferenceMatch;
import org.eclipse.jdt.core.search.TypeReferenceMatch;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;

public class TypeSymbolProvider implements SymbolProvider, WithQuery {
    private String query;

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
        // TypeReferenceMatch often are inaccurate in that if a pattern is a.b.C we get
        // matches for references of C no matter whether they are in package a.b or not
        // we try to confirm whether C we are getting belongs to package a.b with checks:
        // first, we check if the file belongs to package a.b
        // second, we check if CompilationUnit has package declaration of a.b
        // third, we check if CompilationUnit has explicit import of a.b.C or a.b.*
        if (match instanceof TypeReferenceMatch && this.query.contains(".")) {
            try {
                String qualification = "";
                int dotIndex = this.query.lastIndexOf('.');
                if (dotIndex > 0) {
                    qualification = this.query.substring(0, dotIndex);
                }
                var element = (IJavaElement) match.getElement();
                ICompilationUnit compilationUnit = (ICompilationUnit) element
                        .getAncestor(IJavaElement.COMPILATION_UNIT);
                if (compilationUnit == null) {
                    IClassFile cls = (IClassFile) element.getAncestor(IJavaElement.CLASS_FILE);
                    if (cls != null) {
                        // TODO: make sure following doesn't affect performance
                        compilationUnit = cls.becomeWorkingCopy(null, null, null);
                    }
                }
                boolean isAccurate = false;
                Location location = getLocation((IJavaElement) match.getElement(), match);
                // if the file is in the same package as the query
                // there's a high chance its an accurate match
                if (qualification != "" && location.getUri().contains(qualification.replaceAll(".", "/"))) {
                    isAccurate = true;
                }
                if (compilationUnit != null && !isAccurate) {
                    // if the file contains package declaration that matches the query, then type
                    // can be referenced without its fully qualified name
                    for (IPackageDeclaration packageDecl : compilationUnit.getPackageDeclarations()) {
                        if (qualification != "" && packageDecl.getElementName().matches(qualification)) {
                            isAccurate = true;
                        }
                    }
                    // if the file contains explicit imports for the fully qualified name
                    // or a .* import with partial qualified name then type must be accurate
                    if (isAccurate) {
                        for (IImportDeclaration importDecl : compilationUnit.getImports()) {
                            String importElement = importDecl.getElementName();
                            if (importElement.matches(this.query)) {
                                isAccurate = true;
                            }
                            if (qualification != "" &&
                                    importElement.replaceAll(".*", "").matches(qualification)) {
                                isAccurate = true;
                            }
                        }
                    }
                }
                if (!isAccurate) {
                    return null;
                }
            } catch (Exception e) {
                logInfo("failed to determine accuracy of TypeReferenceMatch accepting.." + match);
            }
        }
        try {
            var mod = (IJavaElement) match.getElement();
            SymbolInformation symbol = new SymbolInformation();
            symbol.setName(mod.getElementName());
            symbol.setKind(convertSymbolKind(mod));
            symbol.setContainerName(mod.getParent().getElementName());
            symbol.setLocation(getLocation((IJavaElement) match.getElement(), match));
            symbols.add(symbol);

        } catch (Exception e) {
            logInfo("Unable to convert for TypeSymbolProvider: " + e);
            return null;
        }

        return symbols;
    }

    @Override
    public void setQuery(String query) {
        // TODO Auto-generated method stub
        this.query = query;
    }
}
