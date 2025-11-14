package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.lsp4j.SymbolInformation;

public class PackageDeclarationSymbolProvider implements SymbolProvider, WithQuery {
    private String query;

    @Override
    public List<SymbolInformation> get(SearchMatch match) throws CoreException {
        List<SymbolInformation> symbols = new ArrayList<>();
        try {
            IJavaElement element = (IJavaElement) match.getElement();
            logInfo("Package search match element type: " + element.getClass().getName() + ", element: " + element);

            // Package searches can return different element types
            IPackageDeclaration packageDecl = null;

            if (element instanceof IPackageDeclaration) {
                packageDecl = (IPackageDeclaration) element;
                logInfo("Direct IPackageDeclaration");
            } else if (element instanceof ICompilationUnit) {
                ICompilationUnit cu = (ICompilationUnit) element;
                IPackageDeclaration[] packages = cu.getPackageDeclarations();
                if (packages != null && packages.length > 0) {
                    packageDecl = packages[0];
                    logInfo("Found package from ICompilationUnit: " + packageDecl.getElementName());
                }
            } else if (element instanceof IPackageFragment) {
                // Sometimes the search returns the package fragment itself
                IPackageFragment pkgFrag = (IPackageFragment) element;
                logInfo("IPackageFragment: " + pkgFrag.getElementName());

                // Get a compilation unit from this package to extract the package declaration
                ICompilationUnit[] units = pkgFrag.getCompilationUnits();
                if (units != null && units.length > 0) {
                    IPackageDeclaration[] packages = units[0].getPackageDeclarations();
                    if (packages != null && packages.length > 0) {
                        packageDecl = packages[0];
                        logInfo("Found package from IPackageFragment: " + packageDecl.getElementName());
                    }
                }
            }

            if (packageDecl != null) {
                SymbolInformation symbol = new SymbolInformation();
                symbol.setName(packageDecl.getElementName());
                symbol.setKind(convertSymbolKind(packageDecl));

                // For packages, the container is typically the compilation unit
                IJavaElement parent = packageDecl.getParent();
                if (parent != null) {
                    symbol.setContainerName(parent.getElementName());
                }

                symbol.setLocation(getLocation(packageDecl, match));
                symbols.add(symbol);
                logInfo("Successfully created symbol for package: " + packageDecl.getElementName());
            } else {
                logInfo("Could not extract package declaration from match element: " + element.getClass().getName());
            }
        } catch (Exception e) {
            logInfo("Error processing package declaration: " + e.toString());
            e.printStackTrace();
        }

        return symbols;
    }

    @Override
    public void setQuery(String query) {
        this.query = query;
    }
}
