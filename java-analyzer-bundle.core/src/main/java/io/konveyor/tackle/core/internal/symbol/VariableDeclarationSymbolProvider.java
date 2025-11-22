package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.TypeReferenceMatch;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;

public class VariableDeclarationSymbolProvider implements SymbolProvider {
    @Override
    public List<SymbolInformation> get(SearchMatch match) throws CoreException {
        List<SymbolInformation> symbols = new ArrayList<>();
        try {
            if (!(match instanceof TypeReferenceMatch)) {
                return null;
            }

            TypeReferenceMatch m = (TypeReferenceMatch) match;
            ILocalVariable var = (ILocalVariable) m.getLocalElement();

            if (var != null) {
                // Standard path: getLocalElement() worked (concrete classes like String, File)
                SymbolInformation symbol = new SymbolInformation();
                symbol.setName(var.getElementName());
                symbol.setKind(convertSymbolKind(var));
                symbol.setContainerName(var.getParent().getElementName());
                symbol.setLocation(getLocation(var, match));
                symbols.add(symbol);
                return symbols;
            }

            // Fallback for interface types where getLocalElement() returns null
            // Use AST parsing to check if this type reference is part of a variable declaration
            IJavaElement element = (IJavaElement) match.getElement();
            if (!(element instanceof IMethod)) {
                return null;
            }

            IMethod method = (IMethod) element;
            ICompilationUnit unit = method.getCompilationUnit();
            if (unit == null) {
                return null;
            }

            // Parse AST to find variable declarations at the match offset
            ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
            parser.setSource(unit);
            parser.setResolveBindings(false); // Don't need bindings for structure check
            CompilationUnit cu = (CompilationUnit) parser.createAST(new NullProgressMonitor());

            final int matchOffset = match.getOffset();
            final int matchLength = match.getLength();
            final List<VariableInfo> foundVars = new ArrayList<>();

            cu.accept(new ASTVisitor() {
                @Override
                public boolean visit(VariableDeclarationStatement node) {
                    // Check if the type reference in this statement overlaps with our match
                    int typeStart = node.getType().getStartPosition();
                    int typeLength = node.getType().getLength();

                    if (typeStart == matchOffset ||
                        (matchOffset >= typeStart && matchOffset < typeStart + typeLength)) {
                        // This is a variable declaration for our matched type
                        for (Object obj : node.fragments()) {
                            if (obj instanceof VariableDeclarationFragment) {
                                VariableDeclarationFragment frag = (VariableDeclarationFragment) obj;
                                SimpleName name = frag.getName();
                                foundVars.add(new VariableInfo(
                                    name.getIdentifier(),
                                    name.getStartPosition(),
                                    name.getLength()
                                ));
                            }
                        }
                    }
                    return super.visit(node);
                }
            });

            // Create symbol for each found variable
            for (VariableInfo varInfo : foundVars) {
                SymbolInformation symbol = new SymbolInformation();
                symbol.setName(varInfo.name);
                symbol.setKind(SymbolKind.Variable);
                symbol.setContainerName(method.getElementName());

                // Create location for the variable name
                symbol.setLocation(getLocation(element, match));
                symbols.add(symbol);
            }

        } catch (Exception e) {
            logInfo("unable to convert for variable: " + e);
            return null;
        }
        return symbols;
    }

    private static class VariableInfo {
        String name;
        int offset;
        int length;

        VariableInfo(String name, int offset, int length) {
            this.name = name;
            this.offset = offset;
            this.length = length;
        }
    }
}
