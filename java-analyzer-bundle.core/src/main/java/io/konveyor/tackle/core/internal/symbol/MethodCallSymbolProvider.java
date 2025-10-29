package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.search.MethodReferenceMatch;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;

import io.konveyor.tackle.core.internal.symbol.CustomASTVisitor.QueryLocation;

public class MethodCallSymbolProvider implements SymbolProvider, WithQuery {
    private String query;
    
    @Override
    public List<SymbolInformation> get(SearchMatch match) {
        SymbolKind k = convertSymbolKind((IJavaElement) match.getElement());
        List<SymbolInformation> symbols = new ArrayList<>();
        // For Method Calls we will need to do the local variable trick
        try {
            MethodReferenceMatch m = (MethodReferenceMatch) match;
            IJavaElement e = (IJavaElement) m.getElement();
            SymbolInformation symbol = new SymbolInformation();
            Location location = getLocation((IJavaElement) match.getElement(), match);
            symbol.setName(e.getElementName());
            symbol.setKind(convertSymbolKind(e));
            symbol.setContainerName(e.getParent().getElementName());
            symbol.setLocation(location); 
            if (this.query.contains(".")) { 
                ICompilationUnit unit = null;
                if (m.getElement() instanceof IMethod) {
                    unit = ((IMethod) m.getElement()).getCompilationUnit();
                } else if (m.getElement() instanceof IField) {
                    unit = ((IField) m.getElement()).getCompilationUnit();
                }

                if (unit == null) {
                    IClassFile cls = (IClassFile) ((IJavaElement) e).getAncestor(IJavaElement.CLASS_FILE);
                    if (cls != null) {
                        unit = cls.getWorkingCopy(new WorkingCopyOwnerImpl(), null);
                    }
                }
                if (this.queryQualificationMatches(this.query, e, unit, location)) {
                    ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
                    astParser.setSource(unit);
                    astParser.setResolveBindings(true);
                    CompilationUnit cu = (CompilationUnit) astParser.createAST(null);
                    CustomASTVisitor visitor = new CustomASTVisitor(query, match, QueryLocation.METHOD_CALL);
                    // Under tests, resolveConstructorBinding will return null if there are problems
                    IProblem[] problems = cu.getProblems();
                    if (problems != null && problems.length > 0) {
                        logInfo("KONVEYOR_LOG: " + "Found " + problems.length + " problems while compiling");
                        int count = 0;
                        for (IProblem problem : problems) {
                            logInfo("KONVEYOR_LOG: Problem - ID: " + problem.getID() + " Message: " + problem.getMessage());
                            count++;
                            if (count >= SymbolProvider.MAX_PROBLEMS_TO_LOG) {
                                logInfo("KONVEYOR_LOG: Only showing first " + SymbolProvider.MAX_PROBLEMS_TO_LOG + " problems, " +
                                       (problems.length - SymbolProvider.MAX_PROBLEMS_TO_LOG) + " more not displayed");
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
            logInfo("unable to convert for variable: " + e);
        }

        return symbols;
    }
    @Override
    public void setQuery(String query) {
        this.query = query;
    }
}
