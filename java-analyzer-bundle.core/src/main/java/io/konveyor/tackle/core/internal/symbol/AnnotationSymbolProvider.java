package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
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

import io.konveyor.tackle.core.internal.query.AnnotationQuery;
import io.konveyor.tackle.core.internal.symbol.CustomASTVisitor.QueryLocation;

public class AnnotationSymbolProvider implements SymbolProvider, WithQuery, WithAnnotationQuery {

    private AnnotationQuery annotationQuery;
    private String query;

    private static final List<Class<? extends SourceRefElement>> ACCEPTED_CLASSES = new ArrayList<>();
    static {
        ACCEPTED_CLASSES.add(ResolvedSourceMethod.class);
        ACCEPTED_CLASSES.add(ResolvedSourceField.class);
        ACCEPTED_CLASSES.add(ResolvedSourceType.class);
    }

    @Override
    public List<SymbolInformation> get(SearchMatch match) throws CoreException {
        List<SymbolInformation> symbols = new ArrayList<>();
        try {
            IAnnotatable annotatable = (IAnnotatable) match.getElement();
            for (IAnnotation annotation : annotatable.getAnnotations()) {
                IJavaElement annotationElement = annotation.getPrimaryElement();
                SymbolInformation symbol = new SymbolInformation();
                symbol.setName(annotation.getElementName());
                symbol.setKind(convertSymbolKind(annotationElement));
                symbol.setContainerName(annotation.getParent().getElementName());
                Location location = getLocation(annotationElement, match);
                symbol.setLocation(location);
                if (this.query.contains(".")) {
                    // First try to get compilation unit for source files
                    ICompilationUnit unit = (ICompilationUnit) annotationElement.getAncestor(IJavaElement.COMPILATION_UNIT);
                    if (unit == null) {
                        // If not in source, try to get class file for compiled classes
                        IClassFile cls = (IClassFile) annotationElement.getAncestor(IJavaElement.CLASS_FILE);
                        if (cls != null) {
                            unit = cls.getWorkingCopy(new WorkingCopyOwnerImpl(), null);
                        }
                    }
                    if (unit != null) {
                        IType t = unit.getType(annotationElement.getElementName());
                        String fqdn = "";
                        if (!t.isResolved()) {
                            var elements = unit.codeSelect(match.getOffset(), match.getLength());
                            for (IJavaElement e: Arrays.asList(elements)) {
                                if (e instanceof IType) {
                                    var newT = (IType) e;
                                    if (newT.isResolved()) {
                                        fqdn = newT.getFullyQualifiedName('.');
                                        logInfo("FQDN from code select: " + fqdn);
                                    }
                                }
                            }
                        } else {
                            fqdn = t.getFullyQualifiedName('.');
                            logInfo("resolved type: " + fqdn);
                        }
                        if (query.matches(fqdn) || fqdn.matches(query)) {
                            if (unit.isWorkingCopy())  {
                                unit.discardWorkingCopy();
                                unit.close();
                            }

                            if (matchesAnnotationQuery(match, ACCEPTED_CLASSES)) {
                                symbols.add(symbol);
                            }
                            return symbols;
                        }
                    }

                    logInfo("falling back to resolving via AST");

                    if (this.queryQualificationMatches(this.query.replaceAll("\\(([A-Za-z_][A-Za-z0-9_]*(\\|[A-Za-z_][A-Za-z0-9_]*)*)\\)", ".*"), annotationElement, unit, location)) {
                        ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
                        astParser.setSource(unit);
                        astParser.setResolveBindings(true);
                        CompilationUnit cu = (CompilationUnit) astParser.createAST(null);
                        CustomASTVisitor visitor = new CustomASTVisitor(query, match, QueryLocation.ANNOTATION);
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
                                if (matchesAnnotationQuery(match, ACCEPTED_CLASSES)) {
                                    symbols.add(symbol);
                                }
                            } else {
                                symbols.add(symbol);
                            }
                        }
                    }
                    if (unit != null && unit.isWorkingCopy())  {
                        synchronized (SymbolProvider.LOCATION_LOCK) {
                            unit.discardWorkingCopy();
                            unit.close();
                        }
                    }
                } else {
                    if (annotationQuery != null) {
                        if (matchesAnnotationQuery(match, ACCEPTED_CLASSES)) {
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
