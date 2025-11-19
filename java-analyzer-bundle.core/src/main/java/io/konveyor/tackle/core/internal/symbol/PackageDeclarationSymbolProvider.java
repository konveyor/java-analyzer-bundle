package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
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

            // Package searches with REFERENCES can return different element types:
            // - IImportDeclaration: import statements (import java.util.List;)
            // - IType/IMethod/IField: Fully qualified name usage (java.sql.Connection)
            // - IPackageDeclaration: Package declaration (package io.konveyor.demo;)
            // - IPackageFragment: Package fragment reference

            String packageName = null;
            IJavaElement locationElement = element;

            if (element instanceof IImportDeclaration) {
                // Import statement - extract package from the import
                IImportDeclaration importDecl = (IImportDeclaration) element;
                String importName = importDecl.getElementName();
                logInfo("Import declaration: " + importName);

                // Extract package from import (e.g., "java.util.List" -> "java.util")
                int lastDot = importName.lastIndexOf('.');
                if (lastDot > 0) {
                    packageName = importName.substring(0, lastDot);
                    logInfo("Extracted package from import: " + packageName);
                }
                locationElement = importDecl;
            } else if (element instanceof IType || element instanceof IMethod || element instanceof IField) {
                // Fully qualified name usage - extract package from the element's qualified name
                String fullyQualifiedName = null;
                if (element instanceof IType) {
                    fullyQualifiedName = ((IType) element).getFullyQualifiedName();
                } else if (element instanceof IMethod) {
                    IMethod method = (IMethod) element;
                    IType declaringType = method.getDeclaringType();
                    if (declaringType != null) {
                        fullyQualifiedName = declaringType.getFullyQualifiedName();
                    }
                } else if (element instanceof IField) {
                    IField field = (IField) element;
                    IType declaringType = field.getDeclaringType();
                    if (declaringType != null) {
                        fullyQualifiedName = declaringType.getFullyQualifiedName();
                    }
                }

                if (fullyQualifiedName != null) {
                    int lastDot = fullyQualifiedName.lastIndexOf('.');
                    if (lastDot > 0) {
                        packageName = fullyQualifiedName.substring(0, lastDot);
                        logInfo("Extracted package from FQN: " + packageName + " (from " + fullyQualifiedName + ")");
                    }
                }
            } else if (element instanceof IPackageDeclaration) {
                IPackageDeclaration packageDecl = (IPackageDeclaration) element;
                packageName = packageDecl.getElementName();
                logInfo("Direct IPackageDeclaration: " + packageName);
            } else if (element instanceof ICompilationUnit) {
                ICompilationUnit cu = (ICompilationUnit) element;
                IPackageDeclaration[] packages = cu.getPackageDeclarations();
                if (packages != null && packages.length > 0) {
                    packageName = packages[0].getElementName();
                    logInfo("Found package from ICompilationUnit: " + packageName);
                }
            } else if (element instanceof IPackageFragment) {
                IPackageFragment pkgFrag = (IPackageFragment) element;
                packageName = pkgFrag.getElementName();
                logInfo("IPackageFragment: " + packageName);
            }

            if (packageName != null && !packageName.isEmpty()) {
                SymbolInformation symbol = new SymbolInformation();
                symbol.setName(packageName);
                symbol.setKind(convertSymbolKind(element));

                // For packages, the container is typically the compilation unit or parent element
                IJavaElement parent = locationElement.getParent();
                if (parent != null) {
                    symbol.setContainerName(parent.getElementName());
                }

                symbol.setLocation(getLocation(locationElement, match));
                symbols.add(symbol);
                logInfo("Successfully created symbol for package reference: " + packageName);
            } else {
                logInfo("Could not extract package name from match element: " + element.getClass().getName());
            }
        } catch (Exception e) {
            logInfo("Error processing package reference: " + e.toString());
            e.printStackTrace();
        }

        return symbols;
    }

    @Override
    public void setQuery(String query) {
        this.query = query;
    }
}
