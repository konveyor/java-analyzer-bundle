package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.search.MethodReferenceMatch;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;

import io.konveyor.tackle.core.internal.symbol.CustomASTVisitor.QueryLocation;

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
            Location location = getLocation(mod, match);
            symbol.setName(mod.getElementName());
            // If the search match is for a constructor, the enclosing element may not be a constructor.
            if (m.isConstructor()) {
                symbol.setKind(SymbolKind.Constructor);
            } else {
                logInfo("Method reference was not a constructor, skipping");
                return null;
            }
            symbol.setContainerName(mod.getParent().getElementName());
            symbol.setLocation(location);
            if (this.query.contains(".")) {
                ICompilationUnit unit = mod.getCompilationUnit();
                if (unit == null) {
                    IClassFile cls = (IClassFile) ((IJavaElement) mod).getAncestor(IJavaElement.CLASS_FILE);
                    if (cls != null) {
                        unit = cls.getWorkingCopy(new WorkingCopyOwnerImpl(), null);
                    }
                }
                if (this.queryQualificationMatches(this.query, unit, location)) {
                    ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
                    astParser.setSource(unit);
                    astParser.setResolveBindings(true);
                    CompilationUnit cu = (CompilationUnit) astParser.createAST(null);
                    CustomASTVisitor visitor = new CustomASTVisitor(query, match, QueryLocation.CONSTRUCTOR_CALL);
                    // Under tests, resolveConstructorBinding will return null if there are problems
                    IProblem[] problems = cu.getProblems();
                    if (problems != null && problems.length > 0) {
                        logInfo("KONVEYOR_LOG: " + "Found " + problems.length + " problems while compiling");
                        int count = 0;
                        for (IProblem problem : problems) {
                            logInfo("KONVEYOR_LOG: Problem - ID: " + problem.getID() + " Message: " + problem.getMessage());
                            count++;
                            if (count >= SymbolProvider.MAX_PROBLEMS_TO_LOG) {
                                logInfo("KONVEYOR_LOG: Only showing first " + SymbolProvider.MAX_PROBLEMS_TO_LOG + " problems, " + (problems.length - SymbolProvider.MAX_PROBLEMS_TO_LOG) + " more not displayed");
                                break;
                            }
                        }
                    }
                    cu.accept(visitor);
                    if (visitor.symbolMatches()) {
                        symbols.add(symbol);
                    }
                }
                unit.discardWorkingCopy();
                unit.close();
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
