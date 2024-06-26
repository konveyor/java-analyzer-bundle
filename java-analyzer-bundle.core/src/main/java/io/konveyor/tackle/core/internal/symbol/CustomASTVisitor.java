package io.konveyor.tackle.core.internal.symbol;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.search.SearchMatch;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

public class CustomASTVisitor extends ASTVisitor {
    private String query;
    private SearchMatch match;
    private boolean symbolMatches;


    public CustomASTVisitor(String query, SearchMatch match) {
        /*
         * When comparing query pattern with an actual java element's fqn
         * we need to make sure that * not preceded with a . are replaced
         * by .* so that java regex works as expected on them
        */
        this.query = query.replaceAll("(?<!\\.)\\*", ".*");
        this.symbolMatches = false;
        this.match = match;
    }

    /*
     * When visiting AST nodes, it may happen that we visit more nodes
     * than needed. We need to ensure that we are only visiting ones
     * that are found in the given search match. I wrote this for methods
     * where I observed that a node starts at the beginning of line whereas match
     * starts at an offset within that line. However, both end on the same position.
     * This could differ for other locations. In that case, change logic based
     * on type of the node you get.
     */
    private boolean shouldVisit(ASTNode node) {
        return (this.match.getOffset() + this.match.getLength()) == 
            (node.getStartPosition() + node.getLength());
    }

    /* 
     * This is to get information from a MethodInvocation
     * used for METHOD_CALL and CONSTRUCTOR_CALL matches
     * we only discard a match only when we can tell for sure
     * returning false stops further visits
    */
    @Override
    public boolean visit(MethodInvocation node) {
        try {
            if (!this.shouldVisit(node)) {
                return true;
            }
            IMethodBinding binding = node.resolveMethodBinding();
            if (binding != null) {
                // get fqn of the method being called
                ITypeBinding declaringClass = binding.getDeclaringClass();
                if (declaringClass != null) {
                    String fullyQualifiedName = declaringClass.getQualifiedName() + "." + binding.getName();
                    // match fqn with query pattern
                    if (fullyQualifiedName.matches(this.query)) {
                        this.symbolMatches = true;
                        return false;
                    } else {
                        logInfo("method fqn " + fullyQualifiedName + " did not match with " + query);
                        return true;
                    }
                }
            } 
            // sometimes binding or declaring class cannot be found, usually due to errors
            // in source code. in that case, we will fallback and accept the match
            this.symbolMatches = true;
            return false;
        } catch (Exception e) {
            logInfo("error visiting MethodInvocation node: " + e);
            // this is so that we fallback to old approach and we dont lose a result
            this.symbolMatches = true;
            return false;
        }
    }

    public boolean symbolMatches() {
        return this.symbolMatches;
    }
}
