package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.search.MethodReferenceMatch;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;

public class ConstructorCallSymbolProvider implements SymbolProvider, WithQuery {
    public String query;

    @Override
    public List<SymbolInformation> get(SearchMatch match) throws CoreException {
        List<SymbolInformation> symbols = new ArrayList<>();
        var el = (JavaElement) match.getElement();
        try {
            MethodReferenceMatch m = (MethodReferenceMatch) match;
            var mod  = (IMethod) m.getElement();
            SymbolInformation symbol = new SymbolInformation();
            symbol.setName(mod.getElementName());
            // If the search match is for a constructor, the enclosing element may not be a constructor.
            if (m.isConstructor()) {
                symbol.setKind(SymbolKind.Constructor);
            } else {
                logInfo("Method reference was not a constructor, skipping");
                return null;
            }
            symbol.setContainerName(mod.getParent().getElementName());
            symbol.setLocation(getLocation(mod, match));
            if (this.query.contains(".")) {
                ICompilationUnit unit = mod.getCompilationUnit();
                ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
                astParser.setSource(unit);
                astParser.setResolveBindings(true);
                CompilationUnit cu = (CompilationUnit) astParser.createAST(null);
                cu.accept(new ASTVisitor() {
                    // we are only doing this for MethodInvocation right now
                    // look into MethodDeclaration if needed
                    public boolean visit(MethodInvocation node) {
                        try {
                            IMethodBinding binding = node.resolveMethodBinding();
                            if (binding != null) {
                                // get fqn of the method being called
                                ITypeBinding declaringClass = binding.getDeclaringClass();
                                if (declaringClass != null) {
                                    String fullyQualifiedName = declaringClass.getQualifiedName() + "." + binding.getName();
                                    // match fqn with query pattern
                                    if (fullyQualifiedName.matches(getCleanedQuery(query))) {
                                        symbols.add(symbol);
                                    } else {
                                        logInfo("fqn " + fullyQualifiedName + " did not match with " + query);
                                    }
                                }
                            } 
                        } catch (Exception e) {
                            logInfo("error determining accuracy of match: " + e);
                        }
                        return true;
                    }
                });
            } else {
                symbols.add(symbol);
            }
        } catch (Exception e) {
            logInfo("unable to get constructor: " + e);
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
