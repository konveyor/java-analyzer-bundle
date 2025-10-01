package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import io.konveyor.tackle.core.internal.query.AnnotationQuery;
import io.konveyor.tackle.core.internal.symbol.CustomASTVisitor.QueryLocation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.internal.core.ResolvedSourceField;
import org.eclipse.jdt.internal.core.ResolvedSourceMethod;
import org.eclipse.jdt.internal.core.ResolvedSourceType;
import org.eclipse.jdt.internal.core.SourceRefElement;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;

public class AnnotationSymbolProvider implements SymbolProvider, WithQuery, WithAnnotationQuery {

    private AnnotationQuery annotationQuery;
    private String query;

    @Override
    public List<SymbolInformation> get(SearchMatch match) throws CoreException {
        List<SymbolInformation> symbols = new ArrayList<>();
        try {
            IAnnotatable annotatable = (IAnnotatable) match.getElement();
            IJavaElement element = (IJavaElement) match.getElement();
            for (IAnnotation annotation : annotatable.getAnnotations()) {
                SymbolInformation symbol = new SymbolInformation();
                symbol.setName(annotation.getElementName());
                symbol.setKind(convertSymbolKind(element));
                symbol.setContainerName(annotation.getParent().getElementName());
                Location location = getLocation(element, match);
                symbol.setLocation(location);
                if (this.query.contains(".")) {
                    // First try to get compilation unit for source files
                    ICompilationUnit unit = (ICompilationUnit) element.getAncestor(IJavaElement.COMPILATION_UNIT);
                    if (unit == null) {
                        // If not in source, try to get class file for compiled classes
                        IClassFile cls = (IClassFile) element.getAncestor(IJavaElement.CLASS_FILE);
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
                    }
                    unit.discardWorkingCopy();
                    unit.close();
                } else {
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

    @Override
    public void setQuery(String query) {
        this.query = query;
    }
}
